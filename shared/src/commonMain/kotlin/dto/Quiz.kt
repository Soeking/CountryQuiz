package dto

import kotlinx.serialization.Serializable

/**
 * 出題 API (/api/quizzes/{topic}/random) のレスポンス
 * 国旗クイズの画像は、答えが分からないよう /api/quizzes/{quizId}/image から別に取得する
 */
@Serializable
data class QuizQuestion(
    val quizId: Long,
    /** 文字で出題するクイズの出題文（言語クイズでは現地語の国名を 1 行に 1 つ）。国旗クイズでは null */
    val hintText: String? = null,
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
    /** 正解の国（正解が複数ある問題で正解した場合は、回答した国） */
    val answer: CountryCandidate,
    /** answer 以外にも正解だった国（通貨クイズの「¥」の日本・中国など） */
    val otherAnswers: List<CountryCandidate> = emptyList(),
)
