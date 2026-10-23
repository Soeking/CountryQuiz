package db

import db.seed.backfillSeedKeys
import db.tables.Country
import db.tables.CountryName
import db.tables.Quiz
import db.tables.QuizAnswer
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseFactory {

    fun init(dbPath: String) {
        Database.connect(
            // SQLite は外部キー制約がデフォルトで無効なので、接続ごとに有効化する
            url = "jdbc:sqlite:$dbPath?foreign_keys=on",
            driver = "org.sqlite.JDBC",
        )

        transaction {
            val hadSeedKey = columnNames(Quiz.tableName).let { it.isEmpty() || Quiz.seedKey.name in it }
            SchemaUtils.createMissingTablesAndColumns(
                Country,
                CountryName,
                Quiz,
                QuizAnswer,
            )
            if (!hadSeedKey) backfillSeedKeys()
        }
    }

    /** テーブルの列名。テーブルがなければ空 */
    private fun JdbcTransaction.columnNames(tableName: String): Set<String> =
        exec("PRAGMA table_info($tableName)") { rs ->
            buildSet { while (rs.next()) add(rs.getString("name")) }
        }.orEmpty()
}
