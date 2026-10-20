package routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import plugins.CLIENT_AUTH
import repository.CountryRepository
import repository.QuizRepository

fun Application.configureRouting(
    countryRepository: CountryRepository = CountryRepository(),
    quizRepository: QuizRepository = QuizRepository(),
) {
    routing {
        route("/api") {
            // web クライアントからの通信だけを受け付ける
            authenticate(CLIENT_AUTH) {
                countryRoutes(countryRepository)
                quizRoutes(quizRepository)
                adminRoutes()
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
