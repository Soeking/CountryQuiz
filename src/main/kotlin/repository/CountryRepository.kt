package repository

import db.tables.Country
import db.tables.CountryName
import org.jetbrains.exposed.v1.core.Join
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import util.normalizeSearchKey

data class CountryCandidate(
    val countryId: Int,
    val code: String,
    /** 表示用の名称（ヒットした表記を返す） */
    val matchedName: String,
)

class CountryRepository {

    /**
     * 入力文字列を正規化して country_names を前方一致検索する
     * 同一国が複数ヒットしても 1 件にまとめて返す
     *
     * @param input  ユーザーの入力文字列（正規化前）
     * @param limit  最大表示候補数（デフォルト 5）
     */
    fun search(input: String, limit: Int = 5): List<CountryCandidate> {
        val normalized = normalizeSearchKey(input)
        if (normalized.isEmpty()) return emptyList()

        return transaction {
            Country
                .join(
                    otherTable  = CountryName,
                    joinType    = JoinType.INNER,
                    onColumn    = Country.id,
                    otherColumn = CountryName.countryId,
                )
                .selectAll()
                .where { CountryName.searchKey like "$normalized%" }
                .orderBy(CountryName.searchKey)
                .distinctBy { it[Country.id] }
                .take(limit)
                .map {
                    CountryCandidate(
                        countryId   = it[Country.id],
                        code        = it[Country.code],
                        matchedName = it[CountryName.name],
                    )
                }
        }
    }

    private fun Join.select(columns: Any) {}

    /**
     * country_id から候補を1件取得する
     */
    fun findById(id: Int): CountryCandidate? {
        return transaction {
            Country
                .join(
                    otherTable  = CountryName,
                    joinType    = JoinType.INNER,
                    onColumn    = Country.id,
                    otherColumn = CountryName.countryId,
                )
                .selectAll()
                .where { Country.id eq id }
                .firstOrNull()
                ?.let {
                    CountryCandidate(
                        countryId   = it[Country.id],
                        code        = it[Country.code],
                        matchedName = it[CountryName.name],
                    )
                }
        }
    }
}
