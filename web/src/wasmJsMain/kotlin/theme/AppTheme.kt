package theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window

/** ダークモード設定を保存する localStorage のキー */
private const val DARK_MODE_STORAGE_KEY = "darkMode"

/**
 * ダークモードの初期値
 * 保存済みの設定があればそれを使い、なければ OS の設定（prefers-color-scheme）に合わせる
 */
fun initialDarkMode(): Boolean =
    runCatching { localStorage.getItem(DARK_MODE_STORAGE_KEY) }.getOrNull()?.toBooleanStrictOrNull()
        ?: window.matchMedia("(prefers-color-scheme: dark)").matches

/** ダークモード設定を保存する（プライベートブラウズなどで保存できなくても動作は続ける） */
fun saveDarkMode(darkMode: Boolean) {
    runCatching { localStorage.setItem(DARK_MODE_STORAGE_KEY, darkMode.toString()) }
}

/** アプリ全体のテーマ */
@Composable
fun AppTheme(darkMode: Boolean, content: @Composable () -> Unit) {
    val colorScheme = if (darkMode) darkColorScheme() else lightColorScheme()

    // Compose の描画範囲外（読み込み中やリサイズ時）もテーマの色になるよう、body の背景色を揃える
    SideEffect {
        document.body?.style?.backgroundColor = colorScheme.background.toCssColor()
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}

/** Compose の Color を CSS の色（#rrggbb）に変換する */
private fun Color.toCssColor(): String =
    "#" + (toArgb() and 0xFFFFFF).toString(16).padStart(6, '0')

/** 画面上部に置くダークモードの切り替えスイッチ */
@Composable
fun DarkModeBar(darkMode: Boolean, onDarkModeChange: (Boolean) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = DarkModeIcon,
                contentDescription = "ダークモード",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Switch(checked = darkMode, onCheckedChange = onDarkModeChange)
        }
    }
}
