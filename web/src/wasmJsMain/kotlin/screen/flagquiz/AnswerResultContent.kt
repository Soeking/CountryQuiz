package screen.flagquiz

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dto.AnswerResult
import kotlinx.browser.document
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

/**
 * 回答後に表示する正誤判定
 * 「次の問題」ボタンか Enter で次へ進む
 */
@Composable
internal fun AnswerResultContent(result: AnswerResult, isLast: Boolean, onNext: () -> Unit) {
    // Enter の連打で 2 問進まないよう、1 回だけ呼ぶ
    var advanced by remember { mutableStateOf(false) }
    fun next() {
        if (advanced) return
        advanced = true
        onNext()
    }

    // 回答すると入力欄（IME 用の隠し input 要素）が消え、ブラウザのフォーカスが Compose の canvas から外れる。
    // その状態ではキー入力が Compose に届かないので、document で直接 Enter を受け取る
    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = { event ->
            val keyEvent = event as? KeyboardEvent
            // 押しっぱなし（キーリピート）では進めない
            if (keyEvent != null && keyEvent.key == "Enter" && !keyEvent.repeat) {
                keyEvent.preventDefault()
                next()
            }
        }
        // canvas 側でイベントが止められても受け取れるよう、キャプチャフェーズで登録する
        document.addEventListener("keydown", listener, true)
        onDispose { document.removeEventListener("keydown", listener, true) }
    }

    Text(
        text = if (result.correct) "正解！" else "不正解",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = if (result.correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
    )
    Spacer(Modifier.height(8.dp))
    Text(text = "正解は「${result.answer.matchedName}」", style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(24.dp))
    Button(onClick = ::next) { Text(if (isLast) "結果を見る" else "次の問題") }
}
