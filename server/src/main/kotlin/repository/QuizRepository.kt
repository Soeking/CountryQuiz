package repository

import db.tables.Country
import db.tables.CountryName
import db.tables.Quiz
import db.tables.QuizAnswer
import db.tables.QuizTopic
import dto.AnswerResult
import dto.CountryCandidate
import dto.QuizQuestion
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Random
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class QuizRepository {

    /**
     * 指定した種類のクイズをランダムに 1 問取得する
     *
     * @param excludeIds 出題済みのクイズ id（同じ問題を続けて出さないため）
     */
    fun randomQuiz(topic: QuizTopic, excludeIds: List<Long> = emptyList()): QuizQuestion? {
        return transaction {
            Quiz
                .select(Quiz.id, Quiz.hintText)
                .where {
                    (Quiz.topic eq topic) and
                        // 出題する画像も文章もない問題は除く
                        (Quiz.image.isNotNull() or Quiz.hintText.isNotNull()) and
                        (Quiz.id notInList excludeIds)
                }
                .orderBy(Random())
                .limit(1)
                .firstOrNull()
                ?.let { QuizQuestion(quizId = it[Quiz.id].value, hintText = it[Quiz.hintText]) }
        }
    }

    /** クイズの画像を取得する */
    fun findImage(quizId: Long): ByteArray? {
        return transaction {
            Quiz
                .select(Quiz.image)
                .where { Quiz.id eq quizId }
                .firstOrNull()
                ?.get(Quiz.image)
                ?.bytes
        }
    }

    /**
     * 回答を判定する
     * Quiz.countryId と、QuizAnswer に登録した別解の国を正解とする
     *
     * @return クイズが存在しない場合は null
     */
    fun judge(quizId: Long, countryId: Long): AnswerResult? {
        return transaction {
            val mainCountryId = Quiz
                .select(Quiz.countryId)
                .where { Quiz.id eq quizId }
                .firstOrNull()
                ?.get(Quiz.countryId)
                ?: return@transaction null
            val altCountryIds = QuizAnswer
                .select(QuizAnswer.countryId)
                .where { QuizAnswer.quizId eq quizId }
                .orderBy(QuizAnswer.id to SortOrder.ASC)
                .map { it[QuizAnswer.countryId] }

            val correctCountryIds = listOf(mainCountryId) + altCountryIds
            val correct = countryId in correctCountryIds
            // 別解で正解した場合は、回答した国を正解として先に表示する
            val answers = correctCountryIds
                .sortedByDescending { correct && it == countryId }
                .mapNotNull(::findDisplayCountry)
                .ifEmpty { return@transaction null }

            AnswerResult(correct = correct, answer = answers.first(), otherAnswers = answers.drop(1))
        }
    }

    /** 表示用の国。seed で日本語名を先に登録しているので、id が最小の表記を表示名にする（日本語名がない国は英語名） */
    private fun findDisplayCountry(countryId: Long): CountryCandidate? =
        Country
            .join(
                otherTable  = CountryName,
                joinType    = JoinType.INNER,
                onColumn    = Country.id,
                otherColumn = CountryName.countryId,
            )
            .selectAll()
            .where { Country.id eq countryId }
            .orderBy(CountryName.id to SortOrder.ASC)
            .firstOrNull()
            ?.let {
                CountryCandidate(
                    countryId   = it[Country.id].value,
                    code        = it[Country.code2],
                    matchedName = it[CountryName.name],
                )
            }
}
