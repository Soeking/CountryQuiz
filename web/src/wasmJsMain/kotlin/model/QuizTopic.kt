package model

/** トップ画面で選べるクイズの種類（server の QuizTopic に対応） */
enum class QuizTopic(val label: String, val description: String) {
    FLAG("国旗", "国旗を見て国名を当てる"),
    LANGUAGE("言語", "公用語から国名を当てる"),
    CURRENCY("通貨", "通貨から国名を当てる"),
}
