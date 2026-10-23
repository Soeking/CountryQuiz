package screen.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import api.ApiClient
import kotlinx.coroutines.launch

/** ログイン状態 */
private enum class AdminState { CHECKING, LOGGED_OUT, LOGGED_IN }

/**
 * 管理者専用のクイズ編集画面（/admin で開く）
 * 未ログインならパスワードを入力するログイン画面を表示する
 *
 * @param onBack トップ画面に戻るときに呼ばれる
 */
@Composable
fun AdminScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(AdminState.CHECKING) }

    LaunchedEffect(Unit) {
        state = if (runCatching { ApiClient.isAdminLoggedIn() }.getOrDefault(false)) AdminState.LOGGED_IN
        else AdminState.LOGGED_OUT
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = onBack) { Text("← トップ") }
                    Text(text = "クイズ管理", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = {
                            scope.launch {
                                runCatching { ApiClient.adminLogout() }
                                state = AdminState.LOGGED_OUT
                            }
                        },
                        enabled = state == AdminState.LOGGED_IN,
                    ) { Text(if (state == AdminState.LOGGED_IN) "ログアウト" else "") }
                }
                Spacer(Modifier.height(24.dp))

                when (state) {
                    AdminState.CHECKING -> CircularProgressIndicator()
                    AdminState.LOGGED_OUT -> AdminLogin(onLoggedIn = { state = AdminState.LOGGED_IN })
                    AdminState.LOGGED_IN -> QuizManager(onLoggedOut = { state = AdminState.LOGGED_OUT })
                }
            }
        }
    }
}
