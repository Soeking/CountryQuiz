package api

import auth.CLIENT_HEADER_NAME
import auth.CLIENT_HEADER_VALUE
import dto.AdminLoginRequest
import dto.AdminQuiz
import dto.AdminQuizRequest
import dto.AnswerRequest
import dto.AnswerResult
import dto.CountryCandidate
import dto.QuizQuestion
import dto.SeedResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.browser.window
import model.QuizTopic

/**
 * server の API を呼び出すクライアント
 * 配信元と同じオリジンに送るので、配信時に発行されたセッション Cookie も一緒に送られる
 * Cookie が無効（期限切れ・server 再起動で署名鍵が変わった）で 401 になった場合は、取り直して 1 回だけ再試行する
 */
object ApiClient {

    private val client = HttpClient(Js) {
        expectSuccess = true
        install(ContentNegotiation) { json() }
        defaultRequest {
            url("${window.location.origin}/api/")
            header(CLIENT_HEADER_NAME, CLIENT_HEADER_VALUE)
        }
    }

    /**
     * API を呼び出し、401 ならセッション Cookie を取り直して 1 回だけ再試行する
     */
    private suspend fun <T> withSession(request: suspend () -> T): T =
        try {
            request()
        } catch (e: ClientRequestException) {
            if (e.response.status != HttpStatusCode.Unauthorized) throw e
            refreshSession()
            request()
        }

    /** API 以外のパスにアクセスすると、server が新しいセッション Cookie を発行する */
    private suspend fun refreshSession() {
        client.get("${window.location.origin}/")
    }

    /** 国名の入力候補を取得する */
    suspend fun searchCountries(query: String): List<CountryCandidate> = withSession {
        client.get("countries/search") { parameter("q", query) }.body()
    }

    /**
     * 指定した種類のクイズをランダムに 1 問取得する
     *
     * @param excludeIds 出題済みのクイズ id
     */
    suspend fun randomQuiz(topic: QuizTopic, excludeIds: List<Long>): QuizQuestion = withSession {
        client.get("quizzes/${topic.apiPath}/random") {
            if (excludeIds.isNotEmpty()) parameter("exclude", excludeIds.joinToString(","))
        }.body()
    }

    /** クイズの画像（PNG）を取得する */
    suspend fun quizImage(quizId: Long): ByteArray = withSession {
        client.get("quizzes/$quizId/image").bodyAsBytes()
    }

    /** 回答して判定結果を受け取る */
    suspend fun answer(quizId: Long, countryId: Long): AnswerResult = withSession {
        client.post("quizzes/$quizId/answer") {
            contentType(ContentType.Application.Json)
            setBody(AnswerRequest(countryId = countryId))
        }.body()
    }

    /** 管理者としてログイン中か */
    suspend fun isAdminLoggedIn(): Boolean =
        try {
            withSession { client.get("admin/session") }
            true
        } catch (e: ClientRequestException) {
            if (e.response.status != HttpStatusCode.Unauthorized) throw e
            false
        }

    /** 管理者としてログインする。パスワードが違う場合は 403 の ClientRequestException になる */
    suspend fun adminLogin(password: String) {
        withSession {
            client.post("admin/login") {
                contentType(ContentType.Application.Json)
                setBody(AdminLoginRequest(password = password))
            }
        }
    }

    suspend fun adminLogout() {
        withSession { client.post("admin/logout") }
    }

    /**
     * 管理画面のクイズ一覧
     *
     * @param query 正解の国名か出題文で絞り込む。空なら全件
     */
    suspend fun adminQuizzes(topic: QuizTopic, query: String): List<AdminQuiz> = withSession {
        client.get("admin/topics/${topic.apiPath}/quizzes") {
            if (query.isNotBlank()) parameter("q", query)
        }.body()
    }

    suspend fun createQuiz(topic: QuizTopic, request: AdminQuizRequest): AdminQuiz = withSession {
        client.post("admin/topics/${topic.apiPath}/quizzes") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateQuiz(quizId: Long, request: AdminQuizRequest): AdminQuiz = withSession {
        client.put("admin/quizzes/$quizId") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun deleteQuiz(quizId: Long) {
        withSession { client.delete("admin/quizzes/$quizId") }
    }

    /**
     * seed を実行して、country.json から未登録の国・問題を登録する
     * 国旗画像の取得に失敗した国があれば、ここで再取得する
     */
    suspend fun runSeed(): SeedResult = withSession {
        client.post("admin/seed").body()
    }

    /** 国旗画像（PNG）を登録する */
    suspend fun uploadQuizImage(quizId: Long, png: ByteArray): AdminQuiz = withSession {
        client.put("admin/quizzes/$quizId/image") {
            contentType(ContentType.Image.PNG)
            setBody(png)
        }.body()
    }
}

/** 画面に表示するエラーメッセージ。server が本文でメッセージを返した場合はそれを使う */
suspend fun Throwable.apiErrorMessage(): String =
    (this as? ClientRequestException)
        ?.let { e -> runCatching { e.response.bodyAsText() }.getOrNull()?.takeIf { it.isNotBlank() } ?: e.response.status.toString() }
        ?: toString()

/** 管理者ログインが切れている（または未ログイン）ことによるエラーか */
fun Throwable.isAdminUnauthorized(): Boolean =
    this is ClientRequestException && response.status == HttpStatusCode.Unauthorized
