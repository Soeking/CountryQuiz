package util

import kotlin.test.Test
import kotlin.test.assertEquals

class StringUtilTest {

    @Test
    fun `alphabet is lowercased and symbols are removed`() {
        assertEquals("unitedstates", normalizeSearchKey("United States"))
        assertEquals("cotedivoire", normalizeSearchKey("Côte d'Ivoire"))
        assertEquals("japan", normalizeSearchKey("ＪＡＰＡＮ"))
    }

    @Test
    fun `japanese is kept and katakana is converted to hiragana`() {
        assertEquals("日本", normalizeSearchKey("日本"))
        assertEquals("あめりか合衆国", normalizeSearchKey("アメリカ合衆国"))
        assertEquals("あめりか合衆国", normalizeSearchKey("ｱﾒﾘｶ合衆国"))
        assertEquals("にゅーじーらんど", normalizeSearchKey("ニュージーランド"))
        assertEquals("ぼすにあへるつぇごびな", normalizeSearchKey("ボスニア・ヘルツェゴビナ"))
    }
}
