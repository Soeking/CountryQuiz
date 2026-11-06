package screen.top

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import model.AnswerMode
import model.QuizTopic

/**
 * トップ画面
 * タイトルと回答モードの切り替え、クイズの種類を選ぶボタンを表示する
 *
 * @param onTopicSelected クイズの種類が選ばれたときに、選んでいる回答モードと一緒に呼ばれる
 */
@Composable
fun TopScreen(onTopicSelected: (QuizTopic, AnswerMode) -> Unit) {
    // クイズから戻ってきても、前回選んだモードのままにする
    var mode by rememberSaveable { mutableStateOf(AnswerMode.INPUT) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "CountryQuiz",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "クイズの種類を選んでください",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))

            AnswerModeSelector(selected = mode, onSelect = { mode = it })
            Spacer(Modifier.height(24.dp))

            QuizTopic.entries.forEach { topic ->
                TopicButton(topic = topic, onClick = { onTopicSelected(topic, mode) })
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/**
 * 回答モード（入力 / 4択）の切り替え
 *
 * material3 の SegmentedButton は、日本語のフォールバックフォントの読み込みが終わってもラベルが再描画されず
 * 文字化け（豆腐）したままになるので、通常のボタンを並べて作る
 */
@Composable
private fun AnswerModeSelector(selected: AnswerMode, onSelect: (AnswerMode) -> Unit) {
    Row(
        modifier = Modifier.width(320.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnswerMode.entries.forEach { mode ->
            val modifier = Modifier.weight(1f)
            if (mode == selected) {
                FilledTonalButton(onClick = { onSelect(mode) }, modifier = modifier) { Text(mode.label) }
            } else {
                OutlinedButton(onClick = { onSelect(mode) }, modifier = modifier) { Text(mode.label) }
            }
        }
    }
}

@Composable
private fun TopicButton(topic: QuizTopic, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.width(320.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = topic.label, style = MaterialTheme.typography.titleLarge)
            Text(text = topic.description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
