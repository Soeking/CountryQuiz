package routes

import io.ktor.server.response.*
import io.ktor.server.routing.*
import repository.CountryRepository

/** 国に関する API */
fun Route.countryRoutes(countryRepository: CountryRepository) {
    get("/countries/search") {
        val query = call.request.queryParameters["q"].orEmpty()
        call.respond(countryRepository.search(query))
    }
}
