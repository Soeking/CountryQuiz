package db

import db.tables.Country
import db.tables.CountryName
import db.tables.Quiz
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseFactory {

    fun init(dbPath: String = "./quiz.db") {
        Database.connect(
            // SQLite は外部キー制約がデフォルトで無効なので、接続ごとに有効化する
            url = "jdbc:sqlite:$dbPath?foreign_keys=on",
            driver = "org.sqlite.JDBC",
        )

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Country,
                CountryName,
                Quiz
            )
        }
    }
}
