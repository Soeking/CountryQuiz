package network

import db.seed.seedCountries
import db.seed.seedFlagQuizzes
import dto.AnswerRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import repository.CountryRepository
import repository.QuizRepository

@Serializable
data class SeedResult(val insertedCountries: Int, val insertedFlagQuizzes: Int)

fun Application.configureRouting(
    countryRepository: CountryRepository = CountryRepository(),
    quizRepository: QuizRepository = QuizRepository(),
) {
    routing {
        route("/api") {
            // web クライアントからの通信だけを受け付ける
            authenticate(CLIENT_AUTH) {
                get("/countries/search") {
                    val query = call.request.queryParameters["q"].orEmpty()
                    call.respond(countryRepository.search(query))
                }
                get("/quizzes/flag/random") {
                    // 出題済みのクイズ id をカンマ区切りで受け取る  例: ?exclude=1,2,3
                    val excludeIds = call.request.queryParameters["exclude"].orEmpty()
                        .split(",")
                        .mapNotNull { it.toLongOrNull() }
                    val quiz = quizRepository.randomFlagQuiz(excludeIds)
                        ?: return@get call.respond(HttpStatusCode.NotFound)
                    call.respond(quiz)
                }
                get("/quizzes/{id}/image") {
                    val image = call.parameters["id"]?.toLongOrNull()?.let { quizRepository.findImage(it) }
                        ?: return@get call.respond(HttpStatusCode.NotFound)
                    call.respondBytes(image, ContentType.Image.PNG)
                }
                post("/quizzes/{id}/answer") {
                    val quizId = call.parameters["id"]?.toLongOrNull()
                        ?: return@post call.respond(HttpStatusCode.NotFound)
                    val request = call.receive<AnswerRequest>()
                    val result = quizRepository.judge(quizId, request.countryId)
                        ?: return@post call.respond(HttpStatusCode.NotFound)
                    call.respond(result)
                }
                post("/admin/seed") {
                    call.respond(
                        SeedResult(
                            insertedCountries = seedCountries(),
                            insertedFlagQuizzes = seedFlagQuizzes(),
                        )
                    )
                }
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
