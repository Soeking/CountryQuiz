package navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kotlinx.browser.window
import model.QuizTopic
import screen.admin.AdminScreen
import screen.quiz.QuizScreen
import screen.top.TopScreen

@Serializable
private object TopRoute

@Serializable
private object AdminRoute

/** 管理画面を開く URL のパス */
private const val ADMIN_PATH = "/admin"

/** @param topicName QuizTopic の name */
@Serializable
private data class QuizRoute(val topicName: String)

/** 画面遷移の定義 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    // 管理画面はトップ画面からはたどれないので、/admin を直接開いたときだけ表示する
    val startDestination: Any = if (window.location.pathname.trimEnd('/') == ADMIN_PATH) AdminRoute else TopRoute

    NavHost(navController = navController, startDestination = startDestination) {
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
        composable<AdminRoute> {
            AdminScreen(
                onBack = {
                    // 再読み込みで管理画面に戻らないよう、URL もトップに戻す
                    window.history.replaceState(null, "", "/")
                    navController.navigate(TopRoute) { popUpTo<AdminRoute> { inclusive = true } }
                },
            )
        }
    }
}
