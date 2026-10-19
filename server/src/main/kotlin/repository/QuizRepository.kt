package repository

import db.tables.Country
import db.tables.CountryName
import db.tables.Quiz
import db.tables.QuizTopic
import dto.AnswerResult
import dto.CountryCandidate
import dto.FlagQuiz
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Random
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class QuizRepository {

    /**
     * 国旗クイズをランダムに 1 問取得する
     *
     * @param excludeIds 出題済みのクイズ id（同じ問題を続けて出さないため）
     */
    fun randomFlagQuiz(excludeIds: List<Long> = emptyList()): FlagQuiz? {
        return transaction {
            Quiz
                .select(Quiz.id)
                .where {
                    (Quiz.topic eq QuizTopic.FLAG) and
                        Quiz.image.isNotNull() and
                        (Quiz.id notInList excludeIds)
                }
                .orderBy(Random())
                .limit(1)
                .firstOrNull()
                ?.let { FlagQuiz(quizId = it[Quiz.id].value) }
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
     *
     * @return クイズが存在しない場合は null
     */
    fun judge(quizId: Long, countryId: Long): AnswerResult? {
        return transaction {
            val answerCountryId = Quiz
                .select(Quiz.countryId)
                .where { Quiz.id eq quizId }
                .firstOrNull()
                ?.get(Quiz.countryId)
                ?: return@transaction null

            // seed で日本語名を先に登録しているので、id が最小の表記を表示名にする（日本語名がない国は英語名）
            val answer = Country
                .join(
                    otherTable  = CountryName,
                    joinType    = JoinType.INNER,
                    onColumn    = Country.id,
                    otherColumn = CountryName.countryId,
                )
                .selectAll()
                .where { Country.id eq answerCountryId }
                .orderBy(CountryName.id to SortOrder.ASC)
                .firstOrNull()
                ?.let {
                    CountryCandidate(
                        countryId   = it[Country.id].value,
                        code        = it[Country.code2],
                        matchedName = it[CountryName.name],
                    )
                }
                ?: return@transaction null

            AnswerResult(correct = answerCountryId == countryId, answer = answer)
        }
    }
}
