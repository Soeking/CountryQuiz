package plugins

import kotlinx.serialization.Serializable

/**
 * 管理者ログイン時に発行するセッション（Cookie は署名済み）
 *
 * @param issuedAtMillis 発行日時。Cookie の有効期限とは別に、server 側でも期限切れを判定する
 */
@Serializable
data class AdminSession(val issuedAtMillis: Long)
