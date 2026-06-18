package db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/** クイズ問題 */
object Quiz : LongIdTable("quiz") {
    val topic = enumerationByName<QuizTopic>("topic", 20)
    val countryId = long("country_id")
        .references(Country.id, onDelete = ReferenceOption.SET_NULL)
    val image = blob("image").nullable()
    val hintText = text("hint_text").nullable()
}

enum class QuizTopic(val value: String) {
    FLAG("flag"),
    LANGUAGE("language"),
    CURRENCY("currency"),
}
