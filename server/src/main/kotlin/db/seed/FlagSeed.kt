package db.seed

import datasource.CountryWithName
import db.tables.Country
import db.tables.Quiz
import db.tables.QuizTopic
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

private val httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build()

/**
 * resources/seed/country.json の国旗画像 URL から画像を 1 件ずつ取得して国旗クイズを登録する
 * 国旗クイズ登録済みの国はスキップするので、何度呼んでもよい（取得に失敗した国は次回再試行される）
 * seedCountries の後に呼ぶこと
 *
 * @return 新しく登録した国旗クイズの数
 */
fun seedFlagQuizzes(): Int {
    val countryNameJson = object {}.javaClass.getResourceAsStream("/seed/country.json")?.bufferedReader()?.readText()
        ?: error("Cannot read country.json")
    val flagUrls = json.decodeFromString<List<CountryWithName>>(countryNameJson)
        .associate { it.name.official to it.flags.png }

    // 国旗クイズ未登録の国（id, 正式名称）
    val targets = transaction {
        val seededCountryIds = Quiz
            .select(Quiz.countryId)
            .where { Quiz.topic eq QuizTopic.FLAG }
            .map { it[Quiz.countryId] }
            .toSet()

        Country.selectAll()
            .map { it[Country.id].value to it[Country.officialName] }
            .filter { (id, _) -> id !in seededCountryIds }
    }

    // ダウンロード中に DB をロックし続けないよう、1 件ずつ取得してから登録する
    val insertedCount = targets.count { (id, officialName) ->
        val url = flagUrls[officialName] ?: run {
            println("Flag url not found: $officialName")
            return@count false
        }
        val image = downloadImage(url) ?: return@count false

        transaction {
            Quiz.insert {
                it[topic] = QuizTopic.FLAG
                it[countryId] = id
                it[Quiz.image] = ExposedBlob(image)
            }
        }
        true
    }

    println("Seeded $insertedCount of ${targets.size} flag quizzes.")
    return insertedCount
}

/** 画像を取得する。失敗した場合はログを出して null を返す */
private fun downloadImage(url: String): ByteArray? {
    val request = HttpRequest.newBuilder(URI(url))
        .timeout(Duration.ofSeconds(30))
        // Wikimedia は User-Agent のないリクエストを拒否する
        .header("User-Agent", "QuizeCountry/1.0")
        .build()

    return runCatching { httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray()) }
        .onFailure { println("Failed to download flag: $url ($it)") }
        .getOrNull()
        ?.takeIf { response ->
            (response.statusCode() == 200).also {
                if (!it) println("Failed to download flag: $url (status ${response.statusCode()})")
            }
        }
        ?.body()
}
