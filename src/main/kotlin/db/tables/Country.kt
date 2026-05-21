package db.tables

import org.jetbrains.exposed.v1.core.Table

/** 国マスタ */
object Country : Table("country") {
    val id = integer("id").autoIncrement()
    val code = varchar("code", 2).uniqueIndex()
    val japaneseName = varchar("name", 50)

    override val primaryKey = PrimaryKey(id)
}
