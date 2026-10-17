package db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/** 国マスタ */
object Country : LongIdTable("country") {
    val code2 = varchar("code2", 2).uniqueIndex()
    val code3 = varchar("code3", 3).uniqueIndex()
    val officialName = varchar("name", 100)
}
