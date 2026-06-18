package db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/** 国マスタ */
object Country : LongIdTable("country") {
    val code2 = varchar("code", 2).uniqueIndex()
    val code3 = varchar("code", 3).uniqueIndex()
    val officialName = varchar("name", 50)
}
