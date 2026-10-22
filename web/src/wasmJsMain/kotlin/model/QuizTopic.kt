package model

/**
 * トップ画面で選べるクイズの種類（server の QuizTopic に対応）
 *
 * @param apiPath 出題 API のパス（/api/quizzes/{apiPath}/random）。server の QuizTopic.value と同じ
 */
enum class QuizTopic(val label: String, val description: String, val apiPath: String) {
    FLAG("国旗", "国旗を見て国名を当てる", "flag"),
    LANGUAGE("言語", "現地の言葉で書かれた国名から国を当てる", "language"),
    CURRENCY("通貨", "通貨から国名を当てる", "currency"),
}
