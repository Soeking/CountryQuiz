package db.seed

import datasource.CountryWithCode
import datasource.CountryWithName
import db.tables.Country
import db.tables.CountryName
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.insertIgnoreAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import util.normalizeSearchKey

private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

fun seedCountries() {
    val countryCodeJson =
        object {}.javaClass.getResourceAsStream("/seed/country_code.json")?.bufferedReader()?.readText()
            ?: error("Cannot read countries.json")
    val countryNameJson = object {}.javaClass.getResourceAsStream("/seed/country.json")?.bufferedReader()?.readText()
        ?: error("Cannot read countries.json")
    val countryWithCodes = json.decodeFromString<List<CountryWithCode>>(countryCodeJson)
    val countryWithNames = json.decodeFromString<List<CountryWithName>>(countryNameJson)

    transaction {
        countryWithCodes.forEach { code ->
            // countries テーブルに挿入
            val insertedId = Country.insertIgnoreAndGetId {
                it[code2] = code.cca2
                it[code3] = code.cca3
                it[officialName] = code.name.official
            }?.value ?: return@forEach

            countryWithNames.filter { it.name.official == code.name.official }.forEach { name ->
                CountryName.insertIgnore {
                    it[CountryName.countryId] = insertedId
                    it[CountryName.name] = name.translations.jpn.official
                    it[CountryName.searchKey] = normalizeSearchKey(name.translations.jpn.official)
                }
                CountryName.insertIgnore {
                    it[CountryName.countryId] = insertedId
                    it[CountryName.name] = name.name.official
                    it[CountryName.searchKey] = normalizeSearchKey(name.name.official)
                }
            }
        }
    }

    println("Seeded ${countryWithCodes.size} countries.")
}
