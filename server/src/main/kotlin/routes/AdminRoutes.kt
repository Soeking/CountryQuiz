package routes

import db.seed.seedCountries
import db.seed.seedCurrencyQuizzes
import db.seed.seedFlagQuizzes
import db.seed.seedLanguageQuizzes
import db.tables.QuizTopic
import dto.AdminLoginRequest
import dto.AdminQuizRequest
import dto.SeedResult
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.delay
import plugins.ADMIN_AUTH
import plugins.AdminPassword
import plugins.AdminSession
import repository.AdminQuizRepository

/** ログインに失敗したときの待ち時間（パスワードの総当たりを遅くする） */
private const val LOGIN_FAILURE_DELAY_MILLIS = 1000L

/** 国旗画像の最大サイズ */
private const val MAX_IMAGE_BYTES = 2L * 1024 * 1024

/** 管理用 API。login / logout 以外は管理者ログインが必要 */
fun Route.adminRoutes(adminPassword: AdminPassword, adminQuizRepository: AdminQuizRepository) {
    route("/admin") {
        post("/login") {
            val request = call.receive<AdminLoginRequest>()
            if (!adminPassword.matches(request.password)) {
                delay(LOGIN_FAILURE_DELAY_MILLIS)
                // 401 は web クライアントがセッション切れとみなして再試行するので、403 で返す
                return@post call.respondText(
                    text = if (adminPassword.configured) "パスワードが違います" else "管理者パスワード（ADMIN_PASSWORD）が設定されていません",
                    status = HttpStatusCode.Forbidden,
                )
            }
            call.sessions.set(AdminSession(issuedAtMillis = System.currentTimeMillis()))
            call.respond(HttpStatusCode.NoContent)
        }
        post("/logout") {
            call.sessions.clear<AdminSession>()
            call.respond(HttpStatusCode.NoContent)
        }

        // 既定の FirstSuccessful だと、外側の CLIENT_AUTH が通った時点で認証済みになるので Required にする（外側も Required）
        authenticate(ADMIN_AUTH, strategy = AuthenticationStrategy.Required) {
            // ログイン中かどうかの確認用
            get("/session") { call.respond(HttpStatusCode.NoContent) }

            post("/seed") {
                call.respond(
                    SeedResult(
                        insertedCountries = seedCountries(),
                        insertedFlagQuizzes = seedFlagQuizzes(),
                        insertedLanguageQuizzes = seedLanguageQuizzes(),
                        insertedCurrencyQuizzes = seedCurrencyQuizzes(),
                    )
                )
            }

            // topic は QuizTopic.value  例: /admin/topics/flag/quizzes?q=日本
            route("/topics/{topic}/quizzes") {
                get {
                    val topic = call.quizTopic() ?: return@get call.respond(HttpStatusCode.NotFound)
                    call.respond(adminQuizRepository.list(topic, call.request.queryParameters["q"].orEmpty()))
                }
                post {
                    val topic = call.quizTopic() ?: return@post call.respond(HttpStatusCode.NotFound)
                    val quiz = adminQuizRepository.create(topic, call.receive<AdminQuizRequest>())
                    call.respond(HttpStatusCode.Created, quiz)
                }
            }

            route("/quizzes/{id}") {
                put {
                    val quizId = call.quizId() ?: return@put call.respond(HttpStatusCode.NotFound)
                    val quiz = adminQuizRepository.update(quizId, call.receive<AdminQuizRequest>())
                        ?: return@put call.respond(HttpStatusCode.NotFound)
                    call.respond(quiz)
                }
                delete {
                    val quizId = call.quizId() ?: return@delete call.respond(HttpStatusCode.NotFound)
                    if (!adminQuizRepository.delete(quizId)) return@delete call.respond(HttpStatusCode.NotFound)
                    call.respond(HttpStatusCode.NoContent)
                }
                // 本文は PNG 画像そのもの
                put("/image") {
                    val quizId = call.quizId() ?: return@put call.respond(HttpStatusCode.NotFound)
                    if ((call.request.contentLength() ?: 0) > MAX_IMAGE_BYTES) {
                        return@put call.respondText("画像は 2MB 以下にしてください", status = HttpStatusCode.PayloadTooLarge)
                    }
                    val quiz = adminQuizRepository.setImage(quizId, call.receive<ByteArray>())
                        ?: return@put call.respond(HttpStatusCode.NotFound)
                    call.respond(quiz)
                }
            }
        }
    }
}

private fun RoutingCall.quizTopic(): QuizTopic? =
    QuizTopic.entries.find { it.value == parameters["topic"] }

private fun RoutingCall.quizId(): Long? = parameters["id"]?.toLongOrNull()
