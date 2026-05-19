package net.soeki.Network

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(val id: String, val count: Int)
