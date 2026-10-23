package repository

import db.tables.Quiz
import db.tables.QuizAnswer
import db.tables.QuizTopic
import dto.AnswerResult
import dto.QuizQuestion
import org.jetbrains.exposed.v1.core.Random
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class QuizRepository(private val countryRepository: CountryRepository = CountryRepository()) {

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
                        (Quiz.deleted eq false) and
                        // 管理画面で画像・出題文をまだ登録していない問題は出さない
                        (if (topic == QuizTopic.FLAG) Quiz.image.isNotNull() else Quiz.hintText.isNotNull()) and
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
                .where { (Quiz.id eq quizId) and (Quiz.deleted eq false) }
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
                .where { (Quiz.id eq quizId) and (Quiz.deleted eq false) }
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
                .mapNotNull(countryRepository::findById)
                .ifEmpty { return@transaction null }

            AnswerResult(correct = correct, answer = answers.first(), otherAnswers = answers.drop(1))
        }
    }

}
