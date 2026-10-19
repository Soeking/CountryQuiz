package repository

import db.tables.Country
import db.tables.CountryName
import dto.CountryCandidate
import org.jetbrains.exposed.v1.core.Join
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import util.normalizeSearchKey

class CountryRepository {

    /**
     * 入力文字列を正規化して country_names を部分一致検索する（前方一致を優先して並べる）
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
                // 正規化で % や _ は除去済みなので、エスケープは不要
                .where { CountryName.searchKey like "%$normalized%" }
                .orderBy(CountryName.searchKey)
                // 前方一致する表記を優先する
                .sortedByDescending { it[CountryName.searchKey].startsWith(normalized) }
                .distinctBy { it[Country.id] }
                .take(limit)
                .map {
                    CountryCandidate(
                        countryId   = it[Country.id].value,
                        code        = it[Country.code2],
                        matchedName = it[CountryName.name],
                    )
                }
        }
    }

    private fun Join.select(columns: Any) {}

    /**
     * country_id から候補を1件取得する
     */
    fun findById(id: Long): CountryCandidate? {
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
                        countryId   = it[Country.id].value,
                        code        = it[Country.code2],
                        matchedName = it[CountryName.name],
                    )
                }
        }
    }
}
