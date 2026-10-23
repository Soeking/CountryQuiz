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
    /**
     * seed で登録した問題の識別キー（管理画面で作った問題は null）
     * 管理画面で編集・削除しても残すので、seed は同じ問題を登録し直さない
     */
    val seedKey = varchar("seed_key", 120).nullable().uniqueIndex()
    /** 管理画面で削除した問題。seed で復活しないよう、行は消さずに残す */
    val deleted = bool("deleted").default(false)
}

enum class QuizTopic(val value: String) {
    FLAG("flag"),
    LANGUAGE("language"),
    CURRENCY("currency"),
}
