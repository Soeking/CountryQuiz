package net.soeki

import auth.CLIENT_HEADER_NAME
import auth.CLIENT_HEADER_VALUE
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.*

class ServerTest {

    /** web クライアントを読み込んで Cookie を受け取ったブラウザを模したクライアント */
    private suspend fun ApplicationTestBuilder.webClient() = createClient { install(HttpCookies) }.also {
        assertEquals(HttpStatusCode.OK, it.get("/").status)
    }

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
        val response = webClient().get("/api/countries/search?q=japan") {
            header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "\"code\":\"JP\"")
    }

    @Test
    fun `seed api skips already seeded countries`() = testApplication {
        configure()
        // 起動時に seed 済みなので、追加で登録される国はない
        val response = webClient().post("/api/admin/seed") {
            header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("{\"insertedCountries\":0}", response.bodyAsText())
    }

    @Test
    fun `api rejects request without session cookie`() = testApplication {
        configure()
        val response = client.get("/api/countries/search?q=japan") {
            header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `api rejects request without client header`() = testApplication {
        configure()
        assertEquals(HttpStatusCode.Unauthorized, webClient().get("/api/countries/search?q=japan").status)
    }

    @Test
    fun `unknown api path returns 404`() = testApplication {
        configure()
        assertEquals(HttpStatusCode.NotFound, client.get("/api/unknown").status)
    }
}
