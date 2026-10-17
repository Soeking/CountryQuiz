package network

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import repository.CountryRepository

fun Application.configureRouting(countryRepository: CountryRepository = CountryRepository()) {
    routing {
        route("/api") {
            get("/countries/search") {
                val query = call.request.queryParameters["q"].orEmpty()
                call.respond(countryRepository.search(query))
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
