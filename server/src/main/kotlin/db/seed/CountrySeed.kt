package db.seed

import datasource.CountryWithCode
import datasource.CountryWithName
import db.tables.Country
import db.tables.CountryName
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.insertIgnoreAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import util.normalizeSearchKey

private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/**
 * resources/seed の JSON から国マスタと国名候補を登録する
 * 登録済みの国はスキップするので、何度呼んでもよい
 *
 * @return 新しく登録した国の数
 */
fun seedCountries(): Int {
    val countryCodeJson =
        object {}.javaClass.getResourceAsStream("/seed/country_code.json")?.bufferedReader()?.readText()
            ?: error("Cannot read countries.json")
    val countryNameJson = object {}.javaClass.getResourceAsStream("/seed/country.json")?.bufferedReader()?.readText()
        ?: error("Cannot read countries.json")
    val countryWithCodes = json.decodeFromString<List<CountryWithCode>>(countryCodeJson)
    val countryWithNames = json.decodeFromString<List<CountryWithName>>(countryNameJson)

    val insertedCount = transaction {
        countryWithCodes.count { code ->
            // countries テーブルに挿入
            val insertedId = Country.insertIgnoreAndGetId {
                it[code2] = code.cca2
                it[code3] = code.cca3
                it[officialName] = code.name.official
            }?.value ?: return@count false

            countryWithNames.filter { it.name.official == code.name.official }.forEach { name ->
                name.translations.jpn?.let { jpn ->
                    CountryName.insertIgnore {
                        it[CountryName.countryId] = insertedId
                        it[CountryName.name] = jpn.official
                        it[CountryName.searchKey] = normalizeSearchKey(jpn.official)
                    }
                }
                CountryName.insertIgnore {
                    it[CountryName.countryId] = insertedId
                    it[CountryName.name] = name.name.official
                    it[CountryName.searchKey] = normalizeSearchKey(name.name.official)
                }
            }
            true
        }
    }

    println("Seeded $insertedCount of ${countryWithCodes.size} countries.")
    refreshSearchKeys()
    return insertedCount
}

/**
 * 登録済みの国名候補の search_key を、現在の normalizeSearchKey で作り直す
 * 正規化ルールを変えたときに、既存データを再登録せずに追従させるため
 */
private fun refreshSearchKeys() {
    val updatedCount = transaction {
        CountryName.selectAll()
            .map { Triple(it[CountryName.id].value, it[CountryName.name], it[CountryName.searchKey]) }
            .count { (id, name, searchKey) ->
                val normalized = normalizeSearchKey(name)
                if (normalized == searchKey) return@count false
                CountryName.update({ CountryName.id eq id }) { it[CountryName.searchKey] = normalized }
                true
            }
    }
    if (updatedCount > 0) println("Refreshed $updatedCount search keys.")
}
