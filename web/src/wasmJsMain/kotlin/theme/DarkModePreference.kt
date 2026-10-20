package theme

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
