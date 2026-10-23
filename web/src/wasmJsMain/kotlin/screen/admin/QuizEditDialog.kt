package screen.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import api.ApiClient
import api.apiErrorMessage
import api.isAdminUnauthorized
import dto.AdminQuiz
import dto.AdminQuizRequest
import dto.CountryCandidate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import model.QuizTopic
import kotlin.time.Duration.Companion.milliseconds

/** 国の検索で、入力が止まってから候補を検索するまでの待ち時間 */
private val COUNTRY_SEARCH_DEBOUNCE = 200.milliseconds

/** 選んだ PNG と、そのプレビュー */
private class PickedImage(val png: ByteArray, val preview: ImageBitmap)

/**
 * 問題の作成・編集ダイアログ
 *
 * @param quiz 編集する問題。null なら新規作成
 * @param onChanged 保存・削除が終わったときに呼ばれる
 */
@Composable
internal fun QuizEditDialog(
    topic: QuizTopic,
    quiz: AdminQuiz?,
    reloadKey: Int,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var hintText by remember { mutableStateOf(quiz?.hintText.orEmpty()) }
    var answers by remember { mutableStateOf(quiz?.answers.orEmpty()) }
    var pickedImage by remember { mutableStateOf<PickedImage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    /** API を呼び、成功したら閉じる。ログインが切れていたらログイン画面に戻す */
    fun submit(action: suspend () -> Unit) {
        if (saving) return
        saving = true
        error = null
        scope.launch {
            runCatching { action() }
                .onSuccess { onChanged() }
                .onFailure {
                    if (it.isAdminUnauthorized()) return@launch onLoggedOut()
                    error = it.apiErrorMessage()
                }
            saving = false
        }
    }

    fun save() {
        if (topic == QuizTopic.FLAG && quiz?.hasImage != true && pickedImage == null) {
            error = "国旗画像を選んでください"
            return
        }
        val request = AdminQuizRequest(hintText = hintText, answerCountryIds = answers.map { it.countryId })
        submit {
            val saved = if (quiz == null) ApiClient.createQuiz(topic, request) else ApiClient.updateQuiz(quiz.quizId, request)
            pickedImage?.let { ApiClient.uploadQuizImage(saved.quizId, it.png) }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (quiz == null) "${topic.label}クイズを作成" else "${topic.label}クイズ #${quiz.quizId} を編集") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).width(480.dp)) {
                when (topic) {
                    QuizTopic.FLAG -> {
                        val modifier = Modifier.fillMaxWidth().height(160.dp)
                        pickedImage?.let { ImageOrPlaceholder(image = it.preview, placeholder = "", modifier = modifier) }
                            ?: QuizImage(quizId = quiz?.quizId ?: 0, hasImage = quiz?.hasImage == true, reloadKey = reloadKey, modifier = modifier)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                pickPngFile { png ->
                                    runCatching { decodePng(png) }
                                        .onSuccess { pickedImage = PickedImage(png, it) }
                                        .onFailure { error = "画像を読み込めませんでした" }
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) { Text("画像を選択（PNG）") }
                    }
                    QuizTopic.LANGUAGE -> OutlinedTextField(
                        value = hintText,
                        onValueChange = { hintText = it },
                        label = { Text("現地語の国名（1 行に 1 つ）") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    QuizTopic.CURRENCY -> OutlinedTextField(
                        value = hintText,
                        onValueChange = { hintText = it },
                        label = { Text("通貨記号") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(24.dp))
                Text(text = "正解の国", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = "先頭がメインの正解、2 つ目以降は別解です",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                answers.forEachIndexed { index, answer ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (index == 0) "${answer.matchedName}（メイン）" else answer.matchedName,
                            modifier = Modifier.weight(1f),
                        )
                        if (index > 0) {
                            TextButton(onClick = { answers = listOf(answer) + (answers - answer) }) { Text("メインにする") }
                        }
                        TextButton(onClick = { answers = answers - answer }) { Text("外す") }
                    }
                }
                CountryPicker(
                    excludeIds = answers.map { it.countryId }.toSet(),
                    onPicked = { answers = answers + it },
                )

                error?.let {
                    Spacer(Modifier.height(16.dp))
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                if (quiz != null) {
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider()
                    TextButton(
                        onClick = { confirmingDelete = true },
                        enabled = !saving,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("この問題を削除") }
                }
            }
        },
        confirmButton = { TextButton(onClick = ::save, enabled = !saving) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("キャンセル") } },
    )

    if (confirmingDelete && quiz != null) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("問題を削除しますか？") },
            text = { Text("#${quiz.quizId} を削除します。削除した問題は出題されなくなり、seed でも登録し直されません。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        submit { ApiClient.deleteQuiz(quiz.quizId) }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("削除") }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("キャンセル") } },
        )
    }
}

/** 国名を検索して、正解の国に追加する */
@Composable
private fun CountryPicker(excludeIds: Set<Long>, onPicked: (CountryCandidate) -> Unit) {
    var input by remember { mutableStateOf("") }
    var candidates by remember { mutableStateOf(emptyList<CountryCandidate>()) }

    LaunchedEffect(input) {
        if (input.isBlank()) {
            candidates = emptyList()
            return@LaunchedEffect
        }
        delay(COUNTRY_SEARCH_DEBOUNCE)
        candidates = runCatching { ApiClient.searchCountries(input) }.getOrDefault(emptyList())
    }

    OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        label = { Text("国を追加") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    val shown = candidates.filter { it.countryId !in excludeIds }
    if (shown.isNotEmpty()) {
        Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Column(verticalArrangement = Arrangement.Top) {
                shown.forEachIndexed { index, candidate ->
                    if (index > 0) HorizontalDivider()
                    Text(
                        text = candidate.matchedName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPicked(candidate)
                                input = ""
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
