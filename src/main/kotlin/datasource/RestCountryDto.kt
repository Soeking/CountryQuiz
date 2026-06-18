package datasource

import kotlinx.serialization.Serializable

@Serializable
data class Flag(
    val svg: String,
    val alt: String
)

@Serializable
data class CountryName(
    val common: String,
    val official: String
)

@Serializable
data class Translations(
    val jpn: CountryName
)

@Serializable
data class CountryWithName(
    val flags: Flag,
    val name: CountryName,
    val translations: Translations
)

@Serializable
data class CountryWithCode(
    val name: CountryName,
    val cca2: String,
    val cca3: String
)
