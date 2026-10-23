package screen.admin

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import api.ApiClient
import api.apiErrorMessage
import kotlinx.coroutines.launch

/** 管理者パスワードの入力欄。Enter でもログインできる */
@Composable
internal fun AdminLogin(onLoggedIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loggingIn by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun login() {
        if (loggingIn || password.isEmpty()) return
        loggingIn = true
        error = null
        scope.launch {
            runCatching { ApiClient.adminLogin(password) }
                .onSuccess { onLoggedIn() }
                .onFailure { error = it.apiErrorMessage() }
            loggingIn = false
        }
    }

    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("管理者パスワード") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier
            .width(320.dp)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                    login()
                    true
                } else {
                    false
                }
            },
    )
    Spacer(Modifier.height(16.dp))
    Button(onClick = ::login, enabled = !loggingIn && password.isNotEmpty()) { Text("ログイン") }
    error?.let {
        Spacer(Modifier.height(16.dp))
        Text(text = it, color = MaterialTheme.colorScheme.error)
    }
}
