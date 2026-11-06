package repository

import db.tables.Quiz
import db.tables.QuizAnswer
import db.tables.QuizTopic
import dto.AnswerResult
import dto.CountryCandidate
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
     * @param choiceCount 4 択モードの選択肢の数。0 なら選択肢を返さない
     */
    fun randomQuiz(topic: QuizTopic, excludeIds: List<Long> = emptyList(), choiceCount: Int = 0): QuizQuestion? {
        return transaction {
            val row = Quiz
                .select(Quiz.id, Quiz.hintText, Quiz.countryId)
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
                ?: return@transaction null
            val quizId = row[Quiz.id].value
            QuizQuestion(
                quizId = quizId,
                hintText = row[Quiz.hintText],
                choices = if (choiceCount > 0) choices(quizId, row[Quiz.countryId], choiceCount) else emptyList(),
            )
        }
    }

    /**
     * 4 択モードの選択肢を作る
     * 正解の国 1 つと、別解を含む正解の国以外からランダムに選んだ国を混ぜて返す
     * （通貨クイズの「¥」で日本と中国が両方並ぶと、どちらを選んでも正解になってしまうため）
     */
    private fun choices(quizId: Long, mainCountryId: Long, count: Int): List<CountryCandidate> {
        val correctCountryIds = listOf(mainCountryId) + altCountryIds(quizId)
        val answer = countryRepository.findById(mainCountryId) ?: return emptyList()
        val wrongs = countryRepository.randomCountries(count = count - 1, excludeIds = correctCountryIds)
        return (wrongs + answer).shuffled()
    }

    private fun altCountryIds(quizId: Long): List<Long> =
        QuizAnswer
            .select(QuizAnswer.countryId)
            .where { QuizAnswer.quizId eq quizId }
            .orderBy(QuizAnswer.id to SortOrder.ASC)
            .map { it[QuizAnswer.countryId] }

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
            val correctCountryIds = listOf(mainCountryId) + altCountryIds(quizId)
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
