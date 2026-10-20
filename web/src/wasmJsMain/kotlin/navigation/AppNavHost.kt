package navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import model.QuizTopic
import screen.flagquiz.FlagQuizScreen
import screen.top.TopScreen

@Serializable
private object TopRoute

@Serializable
private object FlagQuizRoute

/** 画面遷移の定義 */
@Composable
fun AppNavHost() {
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
