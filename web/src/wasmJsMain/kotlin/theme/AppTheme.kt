package theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.browser.document

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
