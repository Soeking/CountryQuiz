package db.seed.json

import kotlinx.serialization.Serializable

@Serializable
data class Flag(
    val png: String,
    val svg: String,
    val alt: String
)

@Serializable
data class CountryName(
    val common: String,
    val official: String,
    /** 現地語での国名（キーは ISO 639-3 の言語コード  例: "deu"） */
    val nativeName: Map<String, CountryName> = emptyMap(),
)

@Serializable
data class Translations(
    /** 日本語訳がない国もある */
    val jpn: CountryName? = null
)

@Serializable
data class Currency(
    val name: String,
    /** 記号のない通貨もある */
    val symbol: String? = null,
)

@Serializable
data class CountryWithName(
    val flags: Flag,
    val name: CountryName,
    val translations: Translations,
    /** 通貨（キーは ISO 4217 の通貨コード  例: "JPY"）。通貨のない国もある */
    val currencies: Map<String, Currency> = emptyMap(),
)

@Serializable
data class CountryWithCode(
    val name: CountryName,
    val cca2: String,
    val cca3: String
)
