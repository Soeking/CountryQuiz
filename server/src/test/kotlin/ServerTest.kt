package net.soeki

import auth.CLIENT_HEADER_NAME
import auth.CLIENT_HEADER_VALUE
import dto.AnswerResult
import dto.FlagQuiz
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
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
    fun `country search api accepts japanese`() = testApplication {
        configure()
        val response = webClient().get("/api/countries/search?q=日本") {
            header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "\"code\":\"JP\"")
    }

    @Test
    fun `country search api matches partially and prefers prefix match`() = testApplication {
        configure()
        val client = webClient()
        // 「合衆国」は「アメリカ合衆国」の途中に含まれる
        val partial = client.get("/api/countries/search?q=合衆国") {
            header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE)
        }
        assertContains(partial.bodyAsText(), "\"code\":\"US\"")

        val candidates = client.get("/api/countries/search?q=ja") {
            header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE)
        }.let { Json.decodeFromString<List<dto.CountryCandidate>>(it.bodyAsText()) }
        // 前方一致の Jamaica / Japan が、部分一致（Azerbaijan など）より先に並ぶ
        assertTrue(candidates.take(2).all { it.matchedName.lowercase().startsWith("ja") }, candidates.toString())
    }

    @Test
    fun `seed api skips already seeded countries`() = testApplication {
        configure()
        // 起動時に seed 済みなので、追加で登録される国・国旗クイズはない
        val response = webClient().post("/api/admin/seed") {
            header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("{\"insertedCountries\":0,\"insertedFlagQuizzes\":0}", response.bodyAsText())
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
    fun `flag quiz api returns image and judges answer`() = testApplication {
        configure()
        val client = webClient()

        val quiz = client.get("/api/quizzes/flag/random") {
            header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE)
        }.let { Json.decodeFromString<FlagQuiz>(it.bodyAsText()) }

        val image = client.get("/api/quizzes/${quiz.quizId}/image") {
            header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE)
        }
        assertEquals(HttpStatusCode.OK, image.status)
        assertEquals(ContentType.Image.PNG, image.contentType())
        // PNG のシグネチャ
        assertContentEquals(byteArrayOf(0x89.toByte(), 'P'.code.toByte()), image.bodyAsBytes().take(2).toByteArray())

        suspend fun answer(countryId: Long) = client.post("/api/quizzes/${quiz.quizId}/answer") {
            header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE)
            contentType(ContentType.Application.Json)
            setBody("{\"countryId\":$countryId}")
        }.let { Json.decodeFromString<AnswerResult>(it.bodyAsText()) }

        val wrong = answer(countryId = -1)
        assertFalse(wrong.correct)
        assertTrue(answer(countryId = wrong.answer.countryId).correct)
    }

    @Test
    fun `flag quiz api excludes given quiz ids`() = testApplication {
        configure()
        val client = webClient()
        val first = client.get("/api/quizzes/flag/random") {
            header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE)
        }.let { Json.decodeFromString<FlagQuiz>(it.bodyAsText()) }

        repeat(10) {
            val next = client.get("/api/quizzes/flag/random?exclude=${first.quizId}") {
                header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE)
            }.let { Json.decodeFromString<FlagQuiz>(it.bodyAsText()) }
            assertNotEquals(first.quizId, next.quizId)
        }
    }

    @Test
    fun `unknown api path returns 404`() = testApplication {
        configure()
        assertEquals(HttpStatusCode.NotFound, client.get("/api/unknown").status)
    }
}
