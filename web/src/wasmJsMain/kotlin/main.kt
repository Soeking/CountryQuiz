import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import screen.TopScreen

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        MaterialTheme {
            TopScreen(
                onTopicSelected = { topic ->
                    // TODO: クイズ画面へ遷移する
                    println("selected: $topic")
                },
            )
        }
    }
}
