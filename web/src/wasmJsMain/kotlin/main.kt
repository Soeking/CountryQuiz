import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import component.DarkModeBar
import kotlinx.browser.document
import navigation.AppNavHost
import theme.AppTheme
import theme.initialDarkMode
import theme.saveDarkMode

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        var darkMode by remember { mutableStateOf(initialDarkMode()) }

        AppTheme(darkMode = darkMode) {
            // 画面遷移のクロスフェード中は両画面が半透明になるので、下にテーマの背景を敷いて白く透けないようにする
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    DarkModeBar(
                        darkMode = darkMode,
                        onDarkModeChange = {
                            darkMode = it
                            saveDarkMode(it)
                        },
                    )
                    AppNavHost()
                }
            }
        }
    }
}
