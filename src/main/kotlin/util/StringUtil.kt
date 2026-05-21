package util

/**
 * 登録・検索時に共通して使う正規化処理
 * 例: "United States" → "unitedstates"
 *     "Côte d'Ivoire" → "cotedivoire"
 */
fun normalizeSearchKey(raw: String): String =
    raw.lowercase()
        .replace(Regex("[\\s\\-_'`]"), "")     // スペース・ハイフン・アポストロフィ除去
        .replace(Regex("[àáâãäå]"), "a")        // アクセント正規化
        .replace(Regex("[èéêë]"), "e")
        .replace(Regex("[ìíîï]"), "i")
        .replace(Regex("[òóôõö]"), "o")
        .replace(Regex("[ùúûü]"), "u")
        .replace(Regex("[ñ]"), "n")
        .replace(Regex("[ç]"), "c")
        .replace(Regex("[^a-z0-9]"), "")        // 残った非英数字を除去
