package screen.flagquiz

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** 問題番号と正解数を表示するヘッダー */
@Composable
internal fun Header(questionNumber: Int, correctCount: Int, finished: Boolean, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onBack) { Text("← トップ") }
        Text(text = "国旗クイズ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            text = if (finished) "結果" else "$questionNumber / $QUESTION_COUNT  正解 $correctCount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 国旗画像。読み込み中は image = null */
@Composable
internal fun FlagCard(image: ImageBitmap?) {
    Card(modifier = Modifier.fillMaxWidth().height(240.dp)) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            if (image == null) {
                CircularProgressIndicator()
            } else {
                Image(
                    bitmap = image,
                    contentDescription = "出題中の国旗",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** 全問終了後の結果 */
@Composable
internal fun ResultContent(correctCount: Int, onRestart: () -> Unit, onBack: () -> Unit) {
    Spacer(Modifier.height(40.dp))
    Text(
        text = "$QUESTION_COUNT 問中 $correctCount 問正解",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(40.dp))
    Button(onClick = onRestart) { Text("もう一度") }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = onBack) { Text("トップへ") }
}

/** 問題の取得・回答の送信に失敗したときの表示 */
@Composable
internal fun ErrorContent(message: String, onRetry: () -> Unit) {
    Spacer(Modifier.height(40.dp))
    Text(text = message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
    Button(onClick = onRetry) { Text("再試行") }
}
