package repository

import db.tables.Country
import db.tables.CountryName
import db.tables.Quiz
import db.tables.QuizAnswer
import db.tables.QuizTopic
import dto.AdminQuiz
import dto.AdminQuizRequest
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import util.normalizeSearchKey

/** 入力内容が正しくないときに投げる。メッセージは管理画面にそのまま表示する */
class QuizValidationException(message: String) : IllegalArgumentException(message)

/** 管理画面からのクイズの一覧・作成・更新・削除 */
class AdminQuizRepository(private val countryRepository: CountryRepository = CountryRepository()) {

    /**
     * 削除していないクイズを一覧する
     *
     * @param query 正解の国名（日本語名・英語名の部分一致）か出題文の部分一致で絞り込む。空なら全件
     */
    fun list(topic: QuizTopic, query: String = ""): List<AdminQuiz> {
        return transaction {
            val quizzes = Quiz
                .select(Quiz.id, Quiz.topic, Quiz.countryId, Quiz.hintText, HAS_IMAGE)
                .where { (Quiz.topic eq topic) and (Quiz.deleted eq false) }
                .orderBy(Quiz.id to SortOrder.ASC)
                .map { it.toAdminQuiz() }
            if (query.isBlank()) return@transaction quizzes

            val normalized = normalizeSearchKey(query)
            val matchedCountryIds = if (normalized.isEmpty()) emptySet() else CountryName
                .select(CountryName.countryId, CountryName.searchKey)
                .filter { normalized in it[CountryName.searchKey] }
                .map { it[CountryName.countryId] }
                .toSet()
            quizzes.filter { quiz ->
                quiz.answers.any { it.countryId in matchedCountryIds } ||
                    quiz.hintText?.contains(query.trim(), ignoreCase = true) == true
            }
        }
    }

    /** 削除していないクイズを 1 件取得する */
    fun find(quizId: Long): AdminQuiz? {
        return transaction {
            Quiz
                .select(Quiz.id, Quiz.topic, Quiz.countryId, Quiz.hintText, HAS_IMAGE)
                .where { (Quiz.id eq quizId) and (Quiz.deleted eq false) }
                .firstOrNull()
                ?.toAdminQuiz()
        }
    }

    /**
     * クイズを作成する。国旗クイズの画像は作成後に setImage で登録する
     *
     * @throws QuizValidationException 入力内容が正しくない場合
     */
    fun create(topic: QuizTopic, request: AdminQuizRequest): AdminQuiz {
        val hintText = validHintText(topic, request.hintText)
        return transaction {
            val answerIds = validAnswerCountryIds(request.answerCountryIds)
            val quizId = Quiz.insertAndGetId {
                it[Quiz.topic] = topic
                it[countryId] = answerIds.first()
                it[Quiz.hintText] = hintText
            }.value
            replaceAltAnswers(quizId, answerIds.drop(1))
            find(quizId)!!
        }
    }

    /**
     * 出題文と正解を更新する。クイズの種類は変えられない
     *
     * @return クイズが存在しない場合は null
     * @throws QuizValidationException 入力内容が正しくない場合
     */
    fun update(quizId: Long, request: AdminQuizRequest): AdminQuiz? {
        return transaction {
            val topic = findTopic(quizId) ?: return@transaction null
            val hintText = validHintText(topic, request.hintText)
            val answerIds = validAnswerCountryIds(request.answerCountryIds)
            Quiz.update({ Quiz.id eq quizId }) {
                it[countryId] = answerIds.first()
                it[Quiz.hintText] = hintText
            }
            replaceAltAnswers(quizId, answerIds.drop(1))
            find(quizId)
        }
    }

    /**
     * 国旗画像を登録する
     *
     * @return クイズが存在しない場合は null
     * @throws QuizValidationException 国旗クイズでない、または PNG でない場合
     */
    fun setImage(quizId: Long, png: ByteArray): AdminQuiz? {
        if (!png.isPng()) throw QuizValidationException("PNG 画像を選んでください")
        return transaction {
            val topic = findTopic(quizId) ?: return@transaction null
            if (topic != QuizTopic.FLAG) throw QuizValidationException("画像を登録できるのは国旗クイズだけです")
            Quiz.update({ Quiz.id eq quizId }) { it[image] = ExposedBlob(png) }
            find(quizId)
        }
    }

    /**
     * クイズを削除する
     * seed で登録した問題が次の起動時に復活しないよう、行は消さずに削除済みにする
     *
     * @return 削除した場合は true、クイズが存在しない場合は false
     */
    fun delete(quizId: Long): Boolean {
        return transaction {
            Quiz.update({ (Quiz.id eq quizId) and (Quiz.deleted eq false) }) { it[deleted] = true } > 0
        }
    }

    /** 削除していないクイズの種類 */
    private fun findTopic(quizId: Long): QuizTopic? =
        Quiz.select(Quiz.topic)
            .where { (Quiz.id eq quizId) and (Quiz.deleted eq false) }
            .firstOrNull()
            ?.get(Quiz.topic)

    /** 別解を入れ替える */
    private fun replaceAltAnswers(quizId: Long, altCountryIds: List<Long>) {
        QuizAnswer.deleteWhere { QuizAnswer.quizId eq quizId }
        altCountryIds.forEach { altCountryId ->
            QuizAnswer.insert {
                it[QuizAnswer.quizId] = quizId
                it[countryId] = altCountryId
            }
        }
    }

    /** 正解の国（メイン＋別解）を、表示用の国名付きで返す */
    private fun ResultRow.toAdminQuiz(): AdminQuiz {
        val quizId = this[Quiz.id].value
        val altCountryIds = QuizAnswer
            .select(QuizAnswer.countryId)
            .where { QuizAnswer.quizId eq quizId }
            .orderBy(QuizAnswer.id to SortOrder.ASC)
            .map { it[QuizAnswer.countryId] }
        return AdminQuiz(
            quizId = quizId,
            topic = this[Quiz.topic].value,
            hintText = this[Quiz.hintText],
            hasImage = this[HAS_IMAGE],
            answers = (listOf(this[Quiz.countryId]) + altCountryIds).mapNotNull(countryRepository::findById),
        )
    }

    /**
     * 保存する出題文
     * 言語クイズは 1 行に 1 つの国名なので、各行の前後の空白と空行を除く
     */
    private fun validHintText(topic: QuizTopic, hintText: String?): String? {
        if (topic == QuizTopic.FLAG) return null
        val normalized = hintText.orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
        if (normalized.isEmpty()) throw QuizValidationException("出題文を入力してください")
        return normalized
    }

    /** 重複を除いた正解の国 id。存在しない国が含まれていればエラーにする */
    private fun validAnswerCountryIds(countryIds: List<Long>): List<Long> {
        val ids = countryIds.distinct()
        if (ids.isEmpty()) throw QuizValidationException("正解の国を 1 つ以上選んでください")
        val existingCount = Country.selectAll().where { Country.id inList ids }.count()
        if (existingCount != ids.size.toLong()) throw QuizValidationException("存在しない国が含まれています")
        return ids
    }

    private fun ByteArray.isPng(): Boolean =
        size > PNG_SIGNATURE.size && PNG_SIGNATURE.indices.all { this[it] == PNG_SIGNATURE[it] }

    private companion object {
        /** 画像の有無。一覧で画像本体まで読み込まないよう、select ではこの式を使う（取り出しには同じインスタンスが必要） */
        val HAS_IMAGE = Quiz.image.isNotNull()
        val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0D, 0x0A, 0x1A, 0x0A)
    }
}
