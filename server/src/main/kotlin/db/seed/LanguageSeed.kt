package db.seed

import db.seed.json.CountryWithName
import db.tables.Country
import db.tables.Quiz
import db.tables.QuizTopic
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import util.normalizeSearchKey

private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/** 言語クイズの出題文で、現地語の国名を区切る文字 */
const val LANGUAGE_QUIZ_NAME_SEPARATOR = "\n"

/**
 * resources/seed/country.json の現地語の国名から言語クイズを登録する
 * 登録済み（管理画面で削除した問題を含む）の国はスキップするので、何度呼んでもよい
 * seedCountries の後に呼ぶこと
 *
 * @return 新しく登録した言語クイズの数
 */
fun seedLanguageQuizzes(): Int {
    val countryNameJson = object {}.javaClass.getResourceAsStream("/seed/country.json")?.bufferedReader()?.readText()
        ?: error("Cannot read country.json")
    val nativeNames = json.decodeFromString<List<CountryWithName>>(countryNameJson)
        .associate { it.name.official to quizNativeNames(it) }

    val insertedCount = transaction {
        val seededKeys = seededKeys(QuizTopic.LANGUAGE)
        Country.selectAll()
            .map { it[Country.id].value to it[Country.officialName] }
            .filter { (id, _) -> countrySeedKey(QuizTopic.LANGUAGE, id) !in seededKeys }
            .count { (id, officialName) ->
                // 現地語の国名が英語名・日本語名と同じ国は、問題にならないので登録しない
                val names = nativeNames[officialName].orEmpty().ifEmpty { return@count false }
                Quiz.insert {
                    it[topic] = QuizTopic.LANGUAGE
                    it[countryId] = id
                    it[hintText] = names.joinToString(LANGUAGE_QUIZ_NAME_SEPARATOR)
                    it[seedKey] = countrySeedKey(QuizTopic.LANGUAGE, id)
                }
                true
            }
    }

    println("Seeded $insertedCount language quizzes.")
    return insertedCount
}

/**
 * 出題に使う現地語の国名
 * 英語名・日本語名と同じ表記（大文字小文字・アクセントの違いを含む）は答えそのものなので除く
 */
private fun quizNativeNames(country: CountryWithName): List<String> {
    val knownKeys = listOfNotNull(
        country.name.common,
        country.name.official,
        country.translations.jpn?.common,
        country.translations.jpn?.official,
    ).map(::normalizeSearchKey).toSet()

    return country.name.nativeName.values
        .map { it.common }
        .distinct()
        .filter { normalizeSearchKey(it) !in knownKeys }
}
