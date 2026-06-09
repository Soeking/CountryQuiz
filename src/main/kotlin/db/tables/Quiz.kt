package db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

/** クイズ問題 */
object Quiz : Table("quiz") {
    val id = integer("id").autoIncrement()
    val topic = enumerationByName<QuizTopic>("topic", 20)
    val countryId = integer("country_id")
        .references(Country.id, onDelete = ReferenceOption.SET_NULL)
    val image = blob("image").nullable()
    val hintText = text("hint_text").nullable()

    override val primaryKey = PrimaryKey(id)
}

enum class QuizTopic(val value: String) {
    FLAG("flag"),
    LANGUAGE("language"),
    CURRENCY("currency"),
}
