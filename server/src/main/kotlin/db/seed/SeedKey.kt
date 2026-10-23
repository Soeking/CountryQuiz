package db.seed

import db.tables.Quiz
import db.tables.QuizTopic
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

/** 国ごとに 1 問登録するクイズ（国旗・言語）の seed キー */
internal fun countrySeedKey(topic: QuizTopic, countryId: Long) = "${topic.value}:country:$countryId"

/** 記号ごとに 1 問登録する通貨クイズの seed キー */
internal fun currencySeedKey(symbol: String) = "${QuizTopic.CURRENCY.value}:symbol:$symbol"

/** 登録済み（削除済みを含む）の seed キー */
internal fun JdbcTransaction.seededKeys(topic: QuizTopic): Set<String> =
    Quiz.select(Quiz.seedKey)
        .where { Quiz.topic eq topic }
        .mapNotNull { it[Quiz.seedKey] }
        .toSet()

/**
 * seed_key 列を追加する前に seed で登録した問題に、seed キーを付ける
 * 当時は管理画面がなかったので、既存の問題はすべて seed で登録したものとみなせる
 */
internal fun JdbcTransaction.backfillSeedKeys() {
    Quiz.selectAll()
        .where { Quiz.seedKey.isNull() }
        .map { Triple(it[Quiz.id].value, it[Quiz.topic], it[Quiz.countryId] to it[Quiz.hintText]) }
        .forEach { (id, topic, content) ->
            val (countryId, hintText) = content
            val key = when (topic) {
                QuizTopic.FLAG, QuizTopic.LANGUAGE -> countrySeedKey(topic, countryId)
                QuizTopic.CURRENCY -> currencySeedKey(hintText ?: return@forEach)
            }
            Quiz.update({ Quiz.id eq id }) { it[seedKey] = key }
        }
}
