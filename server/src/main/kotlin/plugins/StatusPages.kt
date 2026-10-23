package plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import repository.QuizValidationException

fun Application.configureStatusPages() {
    install(StatusPages) {
        // 管理画面の入力エラーは、メッセージをそのまま画面に表示する
        exception<QuizValidationException> { call, cause ->
            call.respondText(text = cause.message.orEmpty(), status = HttpStatusCode.BadRequest)
        }
        // JSON の形式が違うなど、リクエストを読み取れなかった
        exception<BadRequestException> { call, _ ->
            call.respondText(text = "リクエストの形式が正しくありません", status = HttpStatusCode.BadRequest)
        }
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
        }
    }
}
