package db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

/**
 * 国名候補
 * 1カ国につき複数の表記を登録する
 * 例: United States / America / Amerika / USA
 */
object CountryName : Table("country_name") {
    val id = integer("id").autoIncrement()
    val countryId = integer("country_id")
        .references(Country.id, onDelete = ReferenceOption.CASCADE)
    /** 表示用の名称  例: "United States" */
    val name = varchar("name", 100)
    /**
     * 検索・照合専用キー
     * 小文字化 + スペース・記号除去した文字列を保存する
     * 例: "unitedstates" / "america" / "usa"
     */
    val searchKey = varchar("search_key", 100)

    override val primaryKey = PrimaryKey(id)
}
