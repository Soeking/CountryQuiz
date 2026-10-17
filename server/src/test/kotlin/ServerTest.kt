package net.soeki

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.*

class ServerTest {

    @Test
    fun `root serves web index html`() = testApplication {
        // loads default configuration
        configure()
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "web.js")
    }

    @Test
    fun `country search api returns candidates`() = testApplication {
        configure()
        val response = client.get("/api/countries/search?q=japan")
        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "\"code\":\"JP\"")
    }

    @Test
    fun `unknown api path returns 404`() = testApplication {
        configure()
        assertEquals(HttpStatusCode.NotFound, client.get("/api/unknown").status)
    }
}
