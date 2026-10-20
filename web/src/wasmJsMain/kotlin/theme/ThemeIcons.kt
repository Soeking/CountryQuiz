package theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/*
 * Material Symbols Rounded（Apache License 2.0）のアイコン
 * https://github.com/google/material-design-icons
 *
 * material-icons-extended は依存が大きいので、使うアイコンだけパスデータから定義する
 */

/** ダークモード（月） */
val DarkModeIcon: ImageVector by lazy {
    materialSymbol(
        name = "DarkMode",
        pathData = "M480-120q-151 0-255.5-104.5T120-480q0-138 90-239.5T440-838q13-2 23 3.5t16 14.5q6 9 6.5 21t-7.5 23" +
            "q-17 26-25.5 55t-8.5 61q0 90 63 153t153 63q31 0 61.5-9t54.5-25q11-7 22.5-6.5T819-479q10 5 15.5 15t3.5 24" +
            "q-14 138-117.5 229T480-120Zm0-80q88 0 158-48.5T740-375q-20 5-40 8t-40 3q-123 0-209.5-86.5T364-660" +
            "q0-20 3-40t8-40q-78 32-126.5 102T200-480q0 116 82 198t198 82Zm-10-270Z",
    )
}

/**
 * Material Symbols の SVG（24px, viewBox="0 -960 960 960"）のパスから ImageVector を作る
 * 色は Icon の tint で指定する
 */
private fun materialSymbol(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
    ).apply {
        // viewBox の y が -960 から始まるので、表示範囲に収まるようずらす
        group(translationY = 960f) {
            addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
        }
    }.build()
