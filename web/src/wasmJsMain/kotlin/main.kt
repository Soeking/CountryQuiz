import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.browser.document
import kotlinx.serialization.Serializable
import screen.FlagQuizScreen
import screen.QuizTopic
import screen.TopScreen
import theme.AppTheme
import theme.DarkModeBar
import theme.initialDarkMode
import theme.saveDarkMode

@Serializable
private object TopRoute

@Serializable
private object FlagQuizRoute

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        var darkMode by remember { mutableStateOf(initialDarkMode()) }

        AppTheme(darkMode = darkMode) {
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

@Composable
private fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = TopRoute) {
        composable<TopRoute> {
            TopScreen(
                onTopicSelected = { topic ->
                    when (topic) {
                        QuizTopic.FLAG -> navController.navigate(FlagQuizRoute)
                        // TODO: 言語・通貨クイズの画面へ遷移する
                        QuizTopic.LANGUAGE, QuizTopic.CURRENCY -> println("selected: $topic")
                    }
                },
            )
        }
        composable<FlagQuizRoute> {
            FlagQuizScreen(onBack = { navController.popBackStack() })
        }
    }
}
