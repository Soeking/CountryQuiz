package db.tables

import org.jetbrains.exposed.v1.core.Table

object Topic : Table("topic") {
    val id = integer(name = "id").autoIncrement()
    val name = varchar("name", 255)

    override val primaryKey = PrimaryKey(id)
}