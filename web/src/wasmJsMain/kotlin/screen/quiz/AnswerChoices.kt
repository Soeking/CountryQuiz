package screen.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dto.CountryCandidate
import kotlinx.browser.document
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

/**
 * 4 択モードの選択肢
 * ボタンをクリックするか、数字キー（1〜選択肢の数）で回答する
 */
@Composable
internal fun AnswerChoices(quizId: Long, choices: List<CountryCandidate>, onAnswer: (countryId: Long) -> Unit) {
    // 連打・キーとクリックの同時押しで 2 回回答しないよう、1 問につき 1 回だけ呼ぶ
    var answered by remember(quizId) { mutableStateOf(false) }
    val currentChoices by rememberUpdatedState(choices)
    val currentOnAnswer by rememberUpdatedState(onAnswer)
    fun select(countryId: Long) {
        if (answered) return
        answered = true
        currentOnAnswer(countryId)
    }

    // 入力欄がないので、ブラウザのフォーカスが Compose の canvas にあるとは限らない。document で直接数字キーを受け取る
    DisposableEffect(quizId) {
        val listener: (Event) -> Unit = { event ->
            val keyEvent = event as? KeyboardEvent
            val index = keyEvent?.takeIf { !it.repeat }?.key?.toIntOrNull()?.minus(1)
            if (index != null && index in currentChoices.indices) {
                keyEvent.preventDefault()
                select(currentChoices[index].countryId)
            }
        }
        // canvas 側でイベントが止められても受け取れるよう、キャプチャフェーズで登録する
        document.addEventListener("keydown", listener, true)
        onDispose { document.removeEventListener("keydown", listener, true) }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEachIndexed { index, choice ->
            OutlinedButton(
                onClick = { select(choice.countryId) },
                enabled = !answered,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${index + 1}. ${choice.matchedName}",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}
