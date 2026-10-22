package db.seed

import db.seed.json.CountryWithName
import db.tables.Country
import db.tables.Quiz
import db.tables.QuizAnswer
import db.tables.QuizTopic
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/**
 * 通貨クイズにする記号を使っている国の上限
 * 「$」（65 か国）や「€」（36 か国）のように多くの国で使われる記号は、当てずっぽうでも正解できるので出題しない
 */
private const val MAX_COUNTRIES_PER_SYMBOL = 3

/**
 * country.json の記号が現地で実際に使われている表記と違う通貨の、現地での表記（キーは通貨コード）
 */
private val LOCAL_SYMBOLS = mapOf(
    // country.json では "Fr." / "Fr"。値札では CHF と書くのが普通
    "CHF" to "CHF",
    // country.json では "B/."。パナマでは実際には米ドルが流通していて、値札は "$" で書かれる
    "PAB" to "$",
    // country.json では "Esc"。現地では "$" を小数点の位置に置いて 1.000$00 のように書く
    "CVE" to "1.000$00",
    // country.json では "T"
    "WST" to "WS$",
    // country.json では "G"
    "HTG" to "HTG",
    // country.json ではラテン文字の "den"。現地ではキリル文字で書く
    "MKD" to "ден",
    // country.json では "m"
    "TMT" to "TMT",
)

/**
 * resources/seed/country.json の通貨記号から通貨クイズを登録する
 * 記号を使っている国はすべて正解にする（1 か国目を Quiz.countryId、残りを QuizAnswer に登録する）
 * 登録済みの記号はスキップするので、何度呼んでもよい（出題しなくなった記号の問題は削除する）
 * seedCountries の後に呼ぶこと
 *
 * @return 新しく登録した通貨クイズの数
 */
fun seedCurrencyQuizzes(): Int {
    val countryNameJson = object {}.javaClass.getResourceAsStream("/seed/country.json")?.bufferedReader()?.readText()
        ?: error("Cannot read country.json")
    val countries = json.decodeFromString<List<CountryWithName>>(countryNameJson)

    val insertedCount = transaction {
        val countryIds = Country.selectAll()
            .associate { it[Country.officialName] to it[Country.id].value }

        // 通貨コード → その通貨を使っている国
        val currencyCountryIds = mutableMapOf<String, MutableSet<Long>>()
        // 記号 → その記号の通貨コード
        val symbolCurrencyCodes = mutableMapOf<String, MutableSet<String>>()
        countries.forEach { country ->
            val countryId = countryIds[country.name.official] ?: return@forEach
            country.currencies.forEach { (code, currency) ->
                currencyCountryIds.getOrPut(code) { linkedSetOf() } += countryId
                val symbol = (LOCAL_SYMBOLS[code] ?: currency.symbol)
                    ?.let(::normalizeSymbol)
                    ?.takeIf { it.isNotEmpty() }
                    ?: return@forEach
                symbolCurrencyCodes.getOrPut(symbol) { linkedSetOf() } += code
            }
        }

        // 国によって記号の表記が違う通貨もある（モロッコ・ディルハムの "د.م." と "DH" など）ので、
        // 記号の表記ではなく、記号が表す通貨を使っている国をすべて正解にする
        val quizSymbols = symbolCurrencyCodes
            .mapValues { (_, codes) -> codes.flatMapTo(linkedSetOf()) { currencyCountryIds.getValue(it) } }
            .filterValues { it.size <= MAX_COUNTRIES_PER_SYMBOL }

        // LOCAL_SYMBOLS で表記を変えた記号など、出題しなくなった記号の問題を消す（別解は CASCADE で消える）
        val deletedCount = Quiz.deleteWhere {
            (Quiz.topic eq QuizTopic.CURRENCY) and (Quiz.hintText notInList quizSymbols.keys.toList())
        }
        if (deletedCount > 0) println("Deleted $deletedCount currency quizzes.")

        val seededSymbols = Quiz
            .select(Quiz.hintText)
            .where { Quiz.topic eq QuizTopic.CURRENCY }
            .mapNotNull { it[Quiz.hintText] }
            .toSet()

        quizSymbols
            .filterKeys { it !in seededSymbols }
            .onEach { (symbol, answerCountryIds) ->
                val quizId = Quiz.insertAndGetId {
                    it[topic] = QuizTopic.CURRENCY
                    it[countryId] = answerCountryIds.first()
                    it[hintText] = symbol
                }
                answerCountryIds.drop(1).forEach { answerCountryId ->
                    QuizAnswer.insert {
                        it[QuizAnswer.quizId] = quizId.value
                        it[QuizAnswer.countryId] = answerCountryId
                    }
                }
            }
            .size
    }

    println("Seeded $insertedCount currency quizzes.")
    return insertedCount
}

/** 記号の前後の空白を除き、連続する空白を 1 つにまとめる  例: "Rs  රු" → "Rs රු" */
private fun normalizeSymbol(symbol: String): String =
    symbol.trim().replace(Regex("\\s+"), " ")
