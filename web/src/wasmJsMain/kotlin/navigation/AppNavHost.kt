package navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import model.QuizTopic
import screen.quiz.QuizScreen
import screen.top.TopScreen

@Serializable
private object TopRoute

/** @param topicName QuizTopic の name */
@Serializable
private data class QuizRoute(val topicName: String)

/** 画面遷移の定義 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = TopRoute) {
        composable<TopRoute> {
            TopScreen(
                onTopicSelected = { topic ->
                    navController.navigate(QuizRoute(topic.name))
                },
            )
        }
        composable<QuizRoute> { entry ->
            QuizScreen(
                topic = QuizTopic.valueOf(entry.toRoute<QuizRoute>().topicName),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
