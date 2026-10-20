package screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import api.ApiClient
import dto.AnswerResult
import dto.CountryCandidate
import kotlinx.browser.document
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

/** 1 回のプレイで出題する問題数 */
private const val QUESTION_COUNT = 10

/** 入力が止まってから候補を検索するまでの待ち時間 */
private const val SEARCH_DEBOUNCE_MILLIS = 200L

/** 回答できない国 id（「わからない」を選んだときに正解だけ受け取るために使う） */
private const val NO_ANSWER_COUNTRY_ID = -1L

/** 出題中の問題の状態 */
private sealed interface QuestionState {
    data object Loading : QuestionState
    data class Error(val message: String) : QuestionState
    data class Answering(val quizId: Long, val image: ImageBitmap) : QuestionState
    data class Answered(val quizId: Long, val image: ImageBitmap, val result: AnswerResult) : QuestionState
}

/**
 * 国旗クイズ画面
 * 国旗を見て国名を入力し、候補から選んで回答する。QUESTION_COUNT 問で結果を表示する
 *
 * @param onBack トップ画面に戻るときに呼ばれる
 */
@Composable
fun FlagQuizScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val askedQuizIds = remember { mutableStateListOf<Long>() }
    var correctCount by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<QuestionState>(QuestionState.Loading) }
    var finished by remember { mutableStateOf(false) }

    fun loadNext() {
        state = QuestionState.Loading
        scope.launch {
            state = runCatching {
                val quiz = ApiClient.randomFlagQuiz(excludeIds = askedQuizIds)
                val image = org.jetbrains.skia.Image.makeFromEncoded(ApiClient.quizImage(quiz.quizId))
                    .toComposeImageBitmap()
                askedQuizIds += quiz.quizId
                QuestionState.Answering(quizId = quiz.quizId, image = image)
            }.getOrElse {
                println("Failed to load flag quiz: $it")
                QuestionState.Error("問題を取得できませんでした\n$it")
            }
        }
    }

    fun answer(quizId: Long, image: ImageBitmap, countryId: Long) {
        scope.launch {
            runCatching { ApiClient.answer(quizId, countryId) }
                .onSuccess { result ->
                    if (result.correct) correctCount++
                    state = QuestionState.Answered(quizId = quizId, image = image, result = result)
                }
                .onFailure {
                    println("Failed to send answer: $it")
                    state = QuestionState.Error("回答を送信できませんでした\n$it")
                }
        }
    }

    fun restart() {
        askedQuizIds.clear()
        correctCount = 0
        finished = false
        loadNext()
    }

    LaunchedEffect(Unit) { loadNext() }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Header(
                    questionNumber = askedQuizIds.size,
                    correctCount = correctCount,
                    finished = finished,
                    onBack = onBack,
                )
                Spacer(Modifier.height(24.dp))

                if (finished) {
                    ResultContent(correctCount = correctCount, onRestart = ::restart, onBack = onBack)
                } else {
                    when (val current = state) {
                        QuestionState.Loading -> FlagCard(image = null)
                        is QuestionState.Error -> ErrorContent(message = current.message, onRetry = ::loadNext)
                        is QuestionState.Answering -> {
                            FlagCard(image = current.image)
                            Spacer(Modifier.height(24.dp))
                            AnswerInput(
                                // 問題が変わったら入力欄をリセットする
                                quizId = current.quizId,
                                onAnswer = { answer(current.quizId, current.image, it) },
                            )
                        }
                        is QuestionState.Answered -> {
                            FlagCard(image = current.image)
                            Spacer(Modifier.height(24.dp))
                            AnswerResultContent(
                                result = current.result,
                                isLast = askedQuizIds.size >= QUESTION_COUNT,
                                onNext = {
                                    if (askedQuizIds.size >= QUESTION_COUNT) finished = true else loadNext()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(questionNumber: Int, correctCount: Int, finished: Boolean, onBack: () -> Unit) {
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
private fun FlagCard(image: ImageBitmap?) {
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

/**
 * 国名の入力欄と候補リスト
 * 候補をクリック、または ↑↓ で選んで Enter を押すと回答する
 */
@Composable
private fun AnswerInput(quizId: Long, onAnswer: (countryId: Long) -> Unit) {
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

@Composable
private fun AnswerResultContent(result: AnswerResult, isLast: Boolean, onNext: () -> Unit) {
    // Enter の連打で 2 問進まないよう、1 回だけ呼ぶ
    var advanced by remember { mutableStateOf(false) }
    fun next() {
        if (advanced) return
        advanced = true
        onNext()
    }

    // 回答すると入力欄（IME 用の隠し input 要素）が消え、ブラウザのフォーカスが Compose の canvas から外れる。
    // その状態ではキー入力が Compose に届かないので、document で直接 Enter を受け取る
    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = { event ->
            val keyEvent = event as? KeyboardEvent
            // 押しっぱなし（キーリピート）では進めない
            if (keyEvent != null && keyEvent.key == "Enter" && !keyEvent.repeat) {
                keyEvent.preventDefault()
                next()
            }
        }
        // canvas 側でイベントが止められても受け取れるよう、キャプチャフェーズで登録する
        document.addEventListener("keydown", listener, true)
        onDispose { document.removeEventListener("keydown", listener, true) }
    }

    Text(
        text = if (result.correct) "正解！" else "不正解",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = if (result.correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
    )
    Spacer(Modifier.height(8.dp))
    Text(text = "正解は「${result.answer.matchedName}」", style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(24.dp))
    Button(onClick = ::next) { Text(if (isLast) "結果を見る" else "次の問題") }
}

@Composable
private fun ResultContent(correctCount: Int, onRestart: () -> Unit, onBack: () -> Unit) {
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

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Spacer(Modifier.height(40.dp))
    Text(text = message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
    Button(onClick = onRetry) { Text("再試行") }
}
