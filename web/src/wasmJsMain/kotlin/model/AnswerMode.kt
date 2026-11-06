package model

/**
 * 回答のしかた
 *
 * @param choiceCount 出題 API に要求する選択肢の数。入力モードでは 0
 */
enum class AnswerMode(val label: String, val choiceCount: Int) {
    /** 国名を入力し、候補から選んで回答する */
    INPUT("入力", 0),

    /** 4 つの選択肢から選んで回答する */
    CHOICE("4択", 4),
}
