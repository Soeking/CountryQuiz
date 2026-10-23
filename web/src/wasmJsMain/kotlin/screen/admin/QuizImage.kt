package screen.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import api.ApiClient
import kotlinx.browser.document
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get

/**
 * 登録済みの国旗画像
 *
 * @param reloadKey 変わったら読み込み直す（画像を差し替えたとき用）
 */
@Composable
internal fun QuizImage(quizId: Long, hasImage: Boolean, reloadKey: Int, modifier: Modifier = Modifier) {
    val image by produceState<ImageBitmap?>(initialValue = null, quizId, hasImage, reloadKey) {
        value = if (!hasImage) null else runCatching { decodePng(ApiClient.quizImage(quizId)) }.getOrNull()
    }
    ImageOrPlaceholder(image = image, placeholder = if (hasImage) "" else "画像なし", modifier = modifier)
}

@Composable
internal fun ImageOrPlaceholder(image: ImageBitmap?, placeholder: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(bitmap = image, contentDescription = "国旗", contentScale = ContentScale.Fit)
        } else {
            Text(text = placeholder, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** PNG を表示用の画像にする。読み込めない画像なら例外になる */
internal fun decodePng(bytes: ByteArray): ImageBitmap =
    org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()

/**
 * ブラウザのファイル選択ダイアログで PNG を選ばせる
 * ブラウザはユーザー操作の中でしかダイアログを開かないので、クリック処理から直接呼ぶこと
 */
internal fun pickPngFile(onPicked: (ByteArray) -> Unit) {
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = "image/png"
    input.addEventListener("change") {
        val file = input.files?.get(0) ?: return@addEventListener
        val reader = FileReader()
        reader.addEventListener("load") {
            val buffer = reader.result?.unsafeCast<ArrayBuffer>() ?: return@addEventListener
            val bytes = Int8Array(buffer)
            onPicked(ByteArray(bytes.length) { bytes[it] })
        }
        reader.readAsArrayBuffer(file)
    }
    input.click()
}
