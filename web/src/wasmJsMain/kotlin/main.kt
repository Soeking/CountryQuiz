import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.browser.document
import kotlinx.serialization.Serializable
import screen.FlagQuizScreen
import screen.QuizTopic
import screen.TopScreen

@Serializable
private object TopRoute

@Serializable
private object FlagQuizRoute

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        MaterialTheme {
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
    }
}
