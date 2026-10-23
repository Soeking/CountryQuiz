package screen.top

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import model.QuizTopic

/**
 * トップ画面
 * タイトルとクイズの種類を選ぶボタンを表示する
 *
 * @param onTopicSelected クイズの種類が選ばれたときに呼ばれる
 */
@Composable
fun TopScreen(onTopicSelected: (QuizTopic) -> Unit) {
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
            Spacer(Modifier.height(40.dp))

            QuizTopic.entries.forEach { topic ->
                TopicButton(topic = topic, onClick = { onTopicSelected(topic) })
                Spacer(Modifier.height(16.dp))
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
