package db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/**
 * クイズの別解
 * 通貨クイズの「¥」（日本・中国）のように正解が複数ある問題で、Quiz.countryId 以外に正解とする国を登録する
 */
object QuizAnswer : LongIdTable("quiz_answer") {
    val quizId = long("quiz_id")
        .references(Quiz.id, onDelete = ReferenceOption.CASCADE)
    val countryId = long("country_id")
        .references(Country.id, onDelete = ReferenceOption.CASCADE)

    init {
        uniqueIndex(quizId, countryId)
    }
}
