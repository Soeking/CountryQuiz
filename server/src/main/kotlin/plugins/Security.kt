package plugins

import auth.CLIENT_HEADER_NAME
import auth.CLIENT_HEADER_VALUE
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import java.security.SecureRandom
import java.util.UUID

/** API を web クライアントからの通信に限定する認証プロバイダー名 */
const val CLIENT_AUTH = "client"

private const val SESSION_MAX_AGE_SECONDS = 24L * 60 * 60

/**
 * web クライアントからの通信だけを API に通すための設定
 *
 * 1. web クライアント（index.html など）を配信するときに、署名付きの HttpOnly Cookie を発行する
 * 2. API では Cookie の署名と、クライアント専用ヘッダーの両方を確認する
 *
 * ブラウザに配るものは誰でも取り出せるので、curl 等での模倣を完全には防げない。
 * 他サイトからの呼び出しや、ページを経由しない雑なアクセスを弾くのが目的。
 */
fun Application.configureSecurity() {
    val signKey = environment.config.propertyOrNull("security.sessionSignKey")?.getString()
        ?.takeIf { it.isNotBlank() }
        ?.let { hex(it) }
        // 未設定なら起動ごとに生成する（再起動すると発行済みの Cookie は無効になる）
        ?: ByteArray(32).also { SecureRandom().nextBytes(it) }
    val secureCookie = environment.config.propertyOrNull("security.secureCookie")?.getString().toBoolean()

    install(Sessions) {
        cookie<ClientSession>("client_session") {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.secure = secureCookie
            cookie.maxAgeInSeconds = SESSION_MAX_AGE_SECONDS
            cookie.extensions["SameSite"] = "Strict"
            transform(SessionTransportTransformerMessageAuthentication(signKey))
        }
    }

    // API 以外（＝ web クライアントの配信）へのアクセス時にセッションを発行する
    intercept(ApplicationCallPipeline.Plugins) {
        if (!call.request.path().startsWith("/api") && call.sessions.get<ClientSession>() == null) {
            call.sessions.set(ClientSession(id = UUID.randomUUID().toString()))
        }
    }

    authentication {
        session<ClientSession>(CLIENT_AUTH) {
            validate { session ->
                session.takeIf { request.headers[CLIENT_HEADER_NAME] == CLIENT_HEADER_VALUE }
            }
            challenge {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}
