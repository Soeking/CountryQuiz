package dto

import kotlinx.serialization.Serializable

/**
 * 国旗クイズ API (/api/quizzes/flag/random) のレスポンス
 * 答えが分からないよう、画像は /api/quizzes/{quizId}/image から別に取得する
 */
@Serializable
data class FlagQuiz(
    val quizId: Long,
)

/** 回答 API (/api/quizzes/{quizId}/answer) のリクエスト */
@Serializable
data class AnswerRequest(
    val countryId: Long,
)

/** 回答 API (/api/quizzes/{quizId}/answer) のレスポンス */
@Serializable
data class AnswerResult(
    val correct: Boolean,
    /** 正解の国 */
    val answer: CountryCandidate,
)
