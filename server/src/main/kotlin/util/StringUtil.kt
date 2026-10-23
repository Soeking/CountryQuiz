package util

import java.text.Normalizer

/**
 * 登録・検索時に共通して使う正規化処理
 * 例: "United States" → "unitedstates"
 *     "Côte d'Ivoire" → "cotedivoire"
 *     "アメリカ合衆国" → "あめりか合衆国"（カタカナはひらがなに揃える）
 */
fun normalizeSearchKey(raw: String): String =
    // 全角英数字 → 半角、半角カナ → 全角カナ
    Normalizer.normalize(raw, Normalizer.Form.NFKC)
        .lowercase()
        .replace(Regex("[\\s\\-_'`]"), "")     // スペース・ハイフン・アポストロフィ除去
        .replace(Regex("[àáâãäå]"), "a")        // アクセント正規化
        .replace(Regex("[èéêë]"), "e")
        .replace(Regex("[ìíîï]"), "i")
        .replace(Regex("[òóôõö]"), "o")
        .replace(Regex("[ùúûü]"), "u")
        .replace('ñ', 'n')
        .replace('ç', 'c')
        .katakanaToHiragana()
        // 残った英数字・ひらがな・漢字・長音記号以外を除去
        .replace(Regex("[^a-z0-9\\p{IsHiragana}\\p{IsHan}ー]"), "")

/** カタカナ（ァ〜ヶ）をひらがなに変換する。長音記号「ー」はそのまま残す */
private fun String.katakanaToHiragana(): String =
    map { if (it in 'ァ'..'ヶ') it - ('ァ' - 'ぁ') else it }.joinToString("")
