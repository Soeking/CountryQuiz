package screen.flagquiz

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import api.ApiClient
import dto.CountryCandidate
import kotlinx.coroutines.delay

/** 入力が止まってから候補を検索するまでの待ち時間 */
private const val SEARCH_DEBOUNCE_MILLIS = 200L

/** 回答できない国 id（「わからない」を選んだときに正解だけ受け取るために使う） */
private const val NO_ANSWER_COUNTRY_ID = -1L

/**
 * 国名の入力欄と候補リスト
 * 候補をクリック、または ↑↓ で選んで Enter を押すと回答する
 */
@Composable
internal fun AnswerInput(quizId: Long, onAnswer: (countryId: Long) -> Unit) {
    // IME の変換中かどうか（composition）を見るため、String ではなく TextFieldValue で持つ
    var input by remember(quizId) { mutableStateOf(TextFieldValue("")) }
    var candidates by remember(quizId) { mutableStateOf(emptyList<CountryCandidate>()) }
    var selectedIndex by remember(quizId) { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }

    // 問題が出たらすぐ入力できるようにする
    LaunchedEffect(quizId) { focusRequester.requestFocus() }

    LaunchedEffect(input.text) {
        if (input.text.isBlank()) {
            candidates = emptyList()
            return@LaunchedEffect
        }
        delay(SEARCH_DEBOUNCE_MILLIS)
        candidates = runCatching { ApiClient.searchCountries(input.text) }.getOrDefault(emptyList())
        selectedIndex = 0
    }

    OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        label = { Text("国名を入力") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                // 変換中の Enter・矢印キーは IME の操作なので横取りしない
                if (event.type != KeyEventType.KeyDown || input.composition != null || candidates.isEmpty()) {
                    return@onPreviewKeyEvent false
                }
                when (event.key) {
                    Key.DirectionDown -> {
                        selectedIndex = (selectedIndex + 1) % candidates.size
                        true
                    }
                    Key.DirectionUp -> {
                        selectedIndex = (selectedIndex - 1 + candidates.size) % candidates.size
                        true
                    }
                    Key.Enter, Key.NumPadEnter -> {
                        onAnswer(candidates[selectedIndex].countryId)
                        true
                    }
                    else -> false
                }
            },
    )

    if (candidates.isNotEmpty()) {
        Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            candidates.forEachIndexed { index, candidate ->
                if (index > 0) HorizontalDivider()
                Text(
                    text = candidate.matchedName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (index == selectedIndex) MaterialTheme.colorScheme.secondaryContainer
                            else Color.Transparent
                        )
                        .clickable { onAnswer(candidate.countryId) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    TextButton(onClick = { onAnswer(NO_ANSWER_COUNTRY_ID) }) { Text("わからない") }
}
