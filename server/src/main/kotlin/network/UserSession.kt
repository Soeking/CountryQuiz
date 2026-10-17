package network

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(val count: Int = 0)
