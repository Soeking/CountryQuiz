package dto

import kotlinx.serialization.Serializable

/** 管理者ログイン API (/api/admin/login) のリクエスト */
@Serializable
data class AdminLoginRequest(
    val password: String,
)

/** 管理画面で扱うクイズ */
@Serializable
data class AdminQuiz(
    val quizId: Long,
    /** クイズの種類（server の QuizTopic.value  例: "flag"） */
    val topic: String,
    /** 出題文（言語・通貨クイズ）。国旗クイズでは null */
    val hintText: String?,
    /** 国旗画像が登録済みか */
    val hasImage: Boolean,
    /** 正解の国（先頭がメインの正解、残りが別解） */
    val answers: List<CountryCandidate>,
)

/** 管理画面でのクイズ作成・更新 API のリクエスト */
@Serializable
data class AdminQuizRequest(
    /** 出題文（言語・通貨クイズでは必須）。国旗クイズでは無視する */
    val hintText: String? = null,
    /** 正解の国 id（先頭がメインの正解、残りが別解）。1 つ以上必要 */
    val answerCountryIds: List<Long>,
)
