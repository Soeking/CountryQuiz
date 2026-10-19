package api

import auth.CLIENT_HEADER_NAME
import auth.CLIENT_HEADER_VALUE
import dto.AnswerRequest
import dto.AnswerResult
import dto.CountryCandidate
import dto.FlagQuiz
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.browser.window

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
     * 国旗クイズをランダムに 1 問取得する
     *
     * @param excludeIds 出題済みのクイズ id
     */
    suspend fun randomFlagQuiz(excludeIds: List<Long>): FlagQuiz = withSession {
        client.get("quizzes/flag/random") {
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
}
