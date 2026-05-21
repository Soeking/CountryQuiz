package db

import db.tables.Country
import db.tables.CountryName
import db.tables.Quiz
import db.tables.Topic
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseFactory {

    fun init(dbPath: String = "./quiz.db") {
        Database.connect(
            url = "jdbc:sqlite:$dbPath",
            driver = "org.sqlite.JDBC",
        )

        transaction {
            // SQLite は外部キー制約がデフォルトで無効なので有効化する
            exec("PRAGMA foreign_keys = ON;")

            SchemaUtils.createMissingTablesAndColumns(
                Country,
                CountryName,
                Topic,
                Quiz
            )
        }
    }
}
