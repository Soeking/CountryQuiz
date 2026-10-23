package routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import plugins.CLIENT_AUTH
import plugins.adminPassword
import repository.AdminQuizRepository
import repository.CountryRepository
import repository.QuizRepository

fun Application.configureRouting(
    countryRepository: CountryRepository = CountryRepository(),
    quizRepository: QuizRepository = QuizRepository(),
    adminQuizRepository: AdminQuizRepository = AdminQuizRepository(),
) {
    val adminPassword = adminPassword()

    routing {
        route("/api") {
            // web クライアントからの通信だけを受け付ける
            // 内側の ADMIN_AUTH と組み合わせたときに、片方だけで通らないよう Required にする
            authenticate(CLIENT_AUTH, strategy = AuthenticationStrategy.Required) {
                countryRoutes(countryRepository)
                quizRoutes(quizRepository)
                adminRoutes(adminPassword, adminQuizRepository)
            }
            // 未定義の /api/** は SPA にフォールバックさせず 404 を返す
            route("{...}") {
                handle { call.respond(HttpStatusCode.NotFound) }
            }
        }

        // web モジュールの wasm ビルド結果（resources/web）を配信する
        singlePageApplication {
            useResources = true
            filesPath = "web"
            defaultPage = "index.html"
        }
    }
}
