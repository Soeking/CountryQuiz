package screen.quiz

import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
internal fun Header(title: String, questionNumber: Int, correctCount: Int, finished: Boolean, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onBack) { Text("← トップ") }
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            text = if (finished) "結果" else "$questionNumber / $QUESTION_COUNT  正解 $correctCount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 問題として表示する内容 */
internal sealed interface QuestionContent {
    /** 国旗クイズの画像 */
    data class Image(val image: ImageBitmap) : QuestionContent

    /** 言語クイズの現地語の国名（1 行に 1 つ） */
    data class Text(val lines: List<String>) : QuestionContent
}

/** 問題の表示。読み込み中は content = null */
@Composable
internal fun QuestionCard(content: QuestionContent?) {
    Card(modifier = Modifier.fillMaxWidth().height(240.dp)) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            when (content) {
                null -> CircularProgressIndicator()
                is QuestionContent.Image -> Image(
                    bitmap = content.image,
                    contentDescription = "出題中の国旗",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                is QuestionContent.Text -> Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    content.lines.forEach { line ->
                        Text(
                            text = line,
                            // 公用語の多い国（最大 8 行）でも収まるよう、行が多いときは小さくする
                            style = if (content.lines.size <= 3) MaterialTheme.typography.headlineMedium
                            else MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
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
