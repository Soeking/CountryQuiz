import db.DatabaseFactory
import db.seed.seedCountries
import db.seed.seedFlagQuizzes
import io.ktor.server.application.*
import io.ktor.server.cio.*
import plugins.configureSecurity
import plugins.configureSerialization
import plugins.configureStatusPages
import routes.configureRouting

fun main(args: Array<String>) = EngineMain.main(args)

/** application.yaml の ktor.application.modules から呼ばれる */
fun Application.module() {
    DatabaseFactory.init(environment.config.property("database.path").getString())
    seedCountries()
    seedFlagQuizzes()

    configureSerialization()
    configureStatusPages()
    configureSecurity()
    configureRouting()
}
