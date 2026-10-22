package routes

import db.tables.QuizTopic
import dto.AnswerRequest
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import repository.QuizRepository

/** クイズの出題・回答に関する API */
fun Route.quizRoutes(quizRepository: QuizRepository) {
    // topic は QuizTopic.value  例: /quizzes/flag/random, /quizzes/language/random
    get("/quizzes/{topic}/random") {
        val topic = QuizTopic.entries.find { it.value == call.parameters["topic"] }
            ?: return@get call.respond(HttpStatusCode.NotFound)
        // 出題済みのクイズ id をカンマ区切りで受け取る  例: ?exclude=1,2,3
        val excludeIds = call.request.queryParameters["exclude"].orEmpty()
            .split(",")
            .mapNotNull { it.toLongOrNull() }
        val quiz = quizRepository.randomQuiz(topic, excludeIds)
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
}
