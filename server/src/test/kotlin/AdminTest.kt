import auth.CLIENT_HEADER_NAME
import auth.CLIENT_HEADER_VALUE
import dto.AdminQuiz
import dto.QuizQuestion
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.deleteIfExists
import kotlin.test.*

private const val ADMIN_PASSWORD = "test-admin-password"

class AdminTest {

    /** 管理画面の操作で開発用の DB を書き換えないよう、コピーした DB を使う */
    private val dbPath: Path = Files.createTempFile("quiz-admin-test", ".db").also {
        Files.copy(Path.of("quiz.db"), it, StandardCopyOption.REPLACE_EXISTING)
    }

    @AfterTest
    fun cleanUp() {
        dbPath.deleteIfExists()
    }

    private fun ApplicationTestBuilder.configureAdmin(password: String? = ADMIN_PASSWORD) {
        configure {
            put("database.path", dbPath.toString())
            if (password != null) put("admin.password", password)
        }
    }

    /** web クライアントを読み込んで Cookie を受け取ったブラウザを模したクライアント */
    private suspend fun ApplicationTestBuilder.webClient() = createClient { install(HttpCookies) }.also {
        assertEquals(HttpStatusCode.OK, it.get("/").status)
    }

    private fun HttpRequestBuilder.json(body: String) {
        header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE)
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private suspend fun HttpClient.login(password: String = ADMIN_PASSWORD): HttpResponse =
        post("/api/admin/login") { json("{\"password\":\"$password\"}") }

    private suspend fun HttpClient.adminGet(path: String): HttpResponse =
        get(path) { header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE) }

    private suspend fun HttpClient.japanId(): Long =
        adminGet("/api/countries/search?q=日本").bodyAsText()
            .let { Json.decodeFromString<List<dto.CountryCandidate>>(it) }
            .first { it.code == "JP" }.countryId

    @Test
    fun `admin page serves web client`() = testApplication {
        configureAdmin()
        val response = client.get("/admin")
        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "web.js")
    }

    @Test
    fun `admin api requires login`() = testApplication {
        configureAdmin()
        val client = webClient()
        assertEquals(HttpStatusCode.Unauthorized, client.adminGet("/api/admin/session").status)
        assertEquals(HttpStatusCode.Unauthorized, client.adminGet("/api/admin/topics/flag/quizzes").status)
        assertEquals(HttpStatusCode.Unauthorized, client.post("/api/admin/seed") { header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE) }.status)
    }

    @Test
    fun `login rejects wrong password`() = testApplication {
        configureAdmin()
        val client = webClient()
        assertEquals(HttpStatusCode.Forbidden, client.login(password = "wrong").status)
        assertEquals(HttpStatusCode.Unauthorized, client.adminGet("/api/admin/session").status)
    }

    @Test
    fun `login is disabled without admin password`() = testApplication {
        configureAdmin(password = null)
        val client = webClient()
        assertEquals(HttpStatusCode.Forbidden, client.login(password = "").status)
    }

    @Test
    fun `admin api requires client header even after login`() = testApplication {
        configureAdmin()
        val client = webClient()
        assertEquals(HttpStatusCode.NoContent, client.login().status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/admin/session").status)
    }

    @Test
    fun `logout ends admin session`() = testApplication {
        configureAdmin()
        val client = webClient()
        client.login()
        assertEquals(HttpStatusCode.NoContent, client.adminGet("/api/admin/session").status)
        client.post("/api/admin/logout") { header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE) }
        assertEquals(HttpStatusCode.Unauthorized, client.adminGet("/api/admin/session").status)
    }

    @Test
    fun `admin can list and search quizzes`() = testApplication {
        configureAdmin()
        val client = webClient()
        client.login()

        val all = client.adminGet("/api/admin/topics/currency/quizzes").bodyAsText()
            .let { Json.decodeFromString<List<AdminQuiz>>(it) }
        assertTrue(all.size > 50, all.size.toString())

        val japan = client.adminGet("/api/admin/topics/currency/quizzes?q=日本").bodyAsText()
            .let { Json.decodeFromString<List<AdminQuiz>>(it) }
        assertEquals(listOf("¥"), japan.map { it.hintText })
        assertEquals(setOf("JP", "CN"), japan.single().answers.map { it.code }.toSet())
    }

    @Test
    fun `admin can create update and delete quiz`() = testApplication {
        configureAdmin()
        val client = webClient()
        client.login()
        val japanId = client.japanId()

        // 作成
        val created = client.post("/api/admin/topics/language/quizzes") {
            json("{\"hintText\":\" Nippon \\n\\n Nihon \",\"answerCountryIds\":[$japanId]}")
        }.let {
            assertEquals(HttpStatusCode.Created, it.status)
            Json.decodeFromString<AdminQuiz>(it.bodyAsText())
        }
        assertEquals("Nippon\nNihon", created.hintText)
        assertEquals(listOf("JP"), created.answers.map { it.code })

        // 更新
        val usId = client.adminGet("/api/countries/search?q=アメリカ").bodyAsText()
            .let { Json.decodeFromString<List<dto.CountryCandidate>>(it) }.first { it.code == "US" }.countryId
        val updated = client.put("/api/admin/quizzes/${created.quizId}") {
            json("{\"hintText\":\"Nippon\",\"answerCountryIds\":[$japanId,$usId]}")
        }.let { Json.decodeFromString<AdminQuiz>(it.bodyAsText()) }
        assertEquals("Nippon", updated.hintText)
        assertEquals(listOf("JP", "US"), updated.answers.map { it.code })

        // 削除すると一覧にも出題にも出ない
        assertEquals(HttpStatusCode.NoContent, client.delete("/api/admin/quizzes/${created.quizId}") {
            header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE)
        }.status)
        val listed = client.adminGet("/api/admin/topics/language/quizzes").bodyAsText()
            .let { Json.decodeFromString<List<AdminQuiz>>(it) }
        assertTrue(listed.none { it.quizId == created.quizId })
        val answer = client.post("/api/quizzes/${created.quizId}/answer") { json("{\"countryId\":$japanId}") }
        assertEquals(HttpStatusCode.NotFound, answer.status)
    }

    @Test
    fun `deleted seed quiz is not restored by seed`() = testApplication {
        configureAdmin()
        val client = webClient()
        client.login()
        val target = client.adminGet("/api/admin/topics/currency/quizzes?q=日本").bodyAsText()
            .let { Json.decodeFromString<List<AdminQuiz>>(it) }.single()

        client.delete("/api/admin/quizzes/${target.quizId}") { header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE) }
        val seed = client.post("/api/admin/seed") { header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE) }
        assertEquals(
            "{\"insertedCountries\":0,\"insertedFlagQuizzes\":0,\"insertedLanguageQuizzes\":0,\"insertedCurrencyQuizzes\":0}",
            seed.bodyAsText(),
        )
        val listed = client.adminGet("/api/admin/topics/currency/quizzes").bodyAsText()
            .let { Json.decodeFromString<List<AdminQuiz>>(it) }
        assertTrue(listed.none { it.hintText == "¥" })
    }

    @Test
    fun `flag quiz is served only after image upload`() = testApplication {
        configureAdmin()
        val client = webClient()
        client.login()
        val japanId = client.japanId()

        val created = client.post("/api/admin/topics/flag/quizzes") {
            json("{\"answerCountryIds\":[$japanId]}")
        }.let { Json.decodeFromString<AdminQuiz>(it.bodyAsText()) }
        assertFalse(created.hasImage)

        // PNG 以外は受け付けない
        val notPng = client.put("/api/admin/quizzes/${created.quizId}/image") {
            header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE)
            contentType(ContentType.Image.PNG)
            setBody("not png".toByteArray())
        }
        assertEquals(HttpStatusCode.BadRequest, notPng.status)

        val png = javaClass.getResourceAsStream("/web/favicon.png")?.readBytes()
            ?: byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0D, 0x0A, 0x1A, 0x0A, 0)
        val uploaded = client.put("/api/admin/quizzes/${created.quizId}/image") {
            header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE)
            contentType(ContentType.Image.PNG)
            setBody(png)
        }.let { Json.decodeFromString<AdminQuiz>(it.bodyAsText()) }
        assertTrue(uploaded.hasImage)
    }

    @Test
    fun `invalid quiz request returns message`() = testApplication {
        configureAdmin()
        val client = webClient()
        client.login()
        val japanId = client.japanId()

        val noHint = client.post("/api/admin/topics/currency/quizzes") { json("{\"hintText\":\" \",\"answerCountryIds\":[$japanId]}") }
        assertEquals(HttpStatusCode.BadRequest, noHint.status)
        assertEquals("出題文を入力してください", noHint.bodyAsText())

        val noAnswer = client.post("/api/admin/topics/currency/quizzes") { json("{\"hintText\":\"X\",\"answerCountryIds\":[]}") }
        assertEquals(HttpStatusCode.BadRequest, noAnswer.status)

        val unknownCountry = client.post("/api/admin/topics/currency/quizzes") { json("{\"hintText\":\"X\",\"answerCountryIds\":[-1]}") }
        assertEquals(HttpStatusCode.BadRequest, unknownCountry.status)
    }

    @Test
    fun `random quiz skips deleted quiz`() = testApplication {
        configureAdmin()
        val client = webClient()
        client.login()
        val all = client.adminGet("/api/admin/topics/currency/quizzes").bodyAsText()
            .let { Json.decodeFromString<List<AdminQuiz>>(it) }
        // 1 問だけ残して削除する
        all.drop(1).forEach { client.delete("/api/admin/quizzes/${it.quizId}") { header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE) } }

        repeat(3) {
            val quiz = client.adminGet("/api/quizzes/currency/random").bodyAsText()
                .let { Json.decodeFromString<QuizQuestion>(it) }
            assertEquals(all.first().quizId, quiz.quizId)
        }
    }
}
