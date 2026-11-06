package db.seed

import db.seed.json.CountryWithCode
import db.seed.json.CountryWithName
import db.tables.Country
import db.tables.CountryName
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.SortOrder
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
    syncDisplayNames(countryWithCodes, countryWithNames)
    refreshSearchKeys()
    return insertedCount
}

/**
 * 登録済みの国の表示名（国ごとに id が最小の国名候補）を、country.json の日本語の正式名称に合わせる
 * country.json の誤訳（例:「ジャージの得意分野」）を直したときに、登録済みの国にも反映するため
 * search_key は直後の refreshSearchKeys で作り直す
 */
private fun syncDisplayNames(countryWithCodes: List<CountryWithCode>, countryWithNames: List<CountryWithName>) {
    val updatedCount = transaction {
        countryWithCodes.count { code ->
            val expected = countryWithNames.firstOrNull { it.name.official == code.name.official }
                ?.let { it.translations.jpn?.official ?: it.name.official }
                ?: return@count false
            val countryId = Country.selectAll().where { Country.code2 eq code.cca2 }.firstOrNull()?.get(Country.id)?.value
                ?: return@count false
            val displayName = CountryName.selectAll()
                .where { CountryName.countryId eq countryId }
                .orderBy(CountryName.id to SortOrder.ASC)
                .firstOrNull()
                ?: return@count false
            if (displayName[CountryName.name] == expected) return@count false
            CountryName.update({ CountryName.id eq displayName[CountryName.id] }) { it[CountryName.name] = expected }
            true
        }
    }
    if (updatedCount > 0) println("Updated $updatedCount country display names.")
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
