package db.tables

import org.jetbrains.exposed.v1.core.Table

/** 国マスタ */
object Country : Table("country") {
    val id = integer("id").autoIncrement()
    val code2 = varchar("code", 2).uniqueIndex()
    val code3 = varchar("code", 3).uniqueIndex()
    val japaneseName = varchar("name", 50)

    override val primaryKey = PrimaryKey(id)
}
