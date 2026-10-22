package routes

import db.seed.seedCountries
import db.seed.seedFlagQuizzes
import db.seed.seedLanguageQuizzes
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class SeedResult(
    val insertedCountries: Int,
    val insertedFlagQuizzes: Int,
    val insertedLanguageQuizzes: Int,
)

/** 管理用 API */
fun Route.adminRoutes() {
    post("/admin/seed") {
        call.respond(
            SeedResult(
                insertedCountries = seedCountries(),
                insertedFlagQuizzes = seedFlagQuizzes(),
                insertedLanguageQuizzes = seedLanguageQuizzes(),
            )
        )
    }
}
