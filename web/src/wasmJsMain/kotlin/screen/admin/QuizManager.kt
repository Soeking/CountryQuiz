package screen.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import api.ApiClient
import api.apiErrorMessage
import api.isAdminUnauthorized
import dto.AdminQuiz
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import model.QuizTopic
import kotlin.time.Duration.Companion.milliseconds

/** 入力が止まってから一覧を検索するまでの待ち時間 */
private val SEARCH_DEBOUNCE = 300.milliseconds

/** 編集中の問題 */
private sealed interface Editing {
    data object New : Editing
    data class Existing(val quiz: AdminQuiz) : Editing
}

/**
 * クイズの一覧・検索と、作成・編集ダイアログの表示
 *
 * @param onLoggedOut 管理者ログインが切れていたときに呼ばれる
 */
@Composable
internal fun ColumnScope.QuizManager(onLoggedOut: () -> Unit) {
    var topic by remember { mutableStateOf(QuizTopic.FLAG) }
    var query by remember { mutableStateOf("") }
    var quizzes by remember { mutableStateOf<List<AdminQuiz>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Editing?>(null) }
    // 保存・削除後に一覧（と国旗のサムネイル）を読み込み直すためのキー
    var reloadKey by remember { mutableIntStateOf(0) }
    var seeding by remember { mutableStateOf(false) }
    var seedMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun runSeed() {
        if (seeding) return
        seeding = true
        seedMessage = null
        scope.launch {
            runCatching { ApiClient.runSeed() }
                .onSuccess {
                    seedMessage = "seed を実行しました（追加: 国 ${it.insertedCountries} 件、国旗 ${it.insertedFlagQuizzes} 問、" +
                        "言語 ${it.insertedLanguageQuizzes} 問、通貨 ${it.insertedCurrencyQuizzes} 問）"
                    reloadKey++
                }
                .onFailure {
                    if (it.isAdminUnauthorized()) return@launch onLoggedOut()
                    seedMessage = "seed に失敗しました: ${it.apiErrorMessage()}"
                }
            seeding = false
        }
    }

    LaunchedEffect(topic, query, reloadKey) {
        if (query.isNotEmpty()) delay(SEARCH_DEBOUNCE)
        runCatching { ApiClient.adminQuizzes(topic, query) }
            .onSuccess {
                quizzes = it
                error = null
            }
            .onFailure {
                if (it.isAdminUnauthorized()) return@LaunchedEffect onLoggedOut()
                error = it.apiErrorMessage()
            }
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = seedMessage ?: "country.json から未登録の問題を追加します（削除した問題は戻りません）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        OutlinedButton(onClick = ::runSeed, enabled = !seeding) {
            if (seeding) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (seeding) "実行中…" else "seed を実行")
        }
    }
    Spacer(Modifier.height(16.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuizTopic.entries.forEach { entry ->
            FilterChip(
                selected = entry == topic,
                onClick = {
                    if (entry == topic) return@FilterChip
                    topic = entry
                    quizzes = null
                },
                label = { Text(entry.label) },
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("正解の国名・出題文で検索") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Button(onClick = { editing = Editing.New }) { Text("新規作成") }
    }
    Spacer(Modifier.height(12.dp))

    error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
    val current = quizzes
    if (current == null) {
        CircularProgressIndicator()
    } else {
        Text(
            text = "${current.size} 件",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(current, key = { it.quizId }) { quiz ->
                QuizRow(
                    topic = topic,
                    quiz = quiz,
                    reloadKey = reloadKey,
                    onClick = { editing = Editing.Existing(quiz) },
                )
                HorizontalDivider()
            }
        }
    }

    editing?.let { target ->
        QuizEditDialog(
            topic = topic,
            quiz = (target as? Editing.Existing)?.quiz,
            reloadKey = reloadKey,
            onDismiss = { editing = null },
            onChanged = {
                editing = null
                reloadKey++
            },
            onLoggedOut = onLoggedOut,
        )
    }
}

/** 一覧の 1 行。国旗クイズはサムネイル、それ以外は出題文と正解の国を表示する */
@Composable
private fun QuizRow(topic: QuizTopic, quiz: AdminQuiz, reloadKey: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "#${quiz.quizId}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp),
        )
        Box(modifier = Modifier.width(160.dp), contentAlignment = Alignment.CenterStart) {
            if (topic == QuizTopic.FLAG) {
                QuizImage(quizId = quiz.quizId, hasImage = quiz.hasImage, reloadKey = reloadKey, modifier = Modifier.size(width = 72.dp, height = 48.dp))
            } else {
                Text(
                    text = quiz.hintText.orEmpty().lines().joinToString(" / "),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = quiz.answers.firstOrNull()?.matchedName ?: "（正解の国なし）", style = MaterialTheme.typography.bodyLarge)
            if (quiz.answers.size > 1) {
                Text(
                    text = "別解: " + quiz.answers.drop(1).joinToString("、") { it.matchedName },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
