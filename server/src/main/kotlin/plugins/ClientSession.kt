package plugins

import kotlinx.serialization.Serializable

/** web クライアントを配信したときに発行するセッション（Cookie は署名済み） */
@Serializable
data class ClientSession(val id: String)
