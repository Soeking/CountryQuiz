package screen.top

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // displayMedium だと幅の狭いスマホ（320dp）で折り返すので、1 段小さくする
            val titleStyle = if (maxWidth < 400.dp) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayMedium
            Column(
                // 高さが足りない画面（スマホの横向きなど）ではスクロールできるようにする。
                // スクロールすると高さの制約がなくなり中央寄せが効かないので、最小の高さを画面の高さにする
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "CountryQuiz",
                    style = titleStyle,
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
        modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(),
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
        modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(),
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
