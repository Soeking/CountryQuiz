package screen.quiz

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import api.ApiClient
import dto.AnswerResult
import dto.QuizQuestion
import kotlinx.coroutines.launch
import model.QuizTopic

/** 1 回のプレイで出題する問題数 */
internal const val QUESTION_COUNT = 10

/** 出題中の問題の状態 */
private sealed interface QuestionState {
    data object Loading : QuestionState
    data class Error(val message: String) : QuestionState
    data class Answering(val quizId: Long, val content: QuestionContent) : QuestionState
    data class Answered(val quizId: Long, val content: QuestionContent, val result: AnswerResult) : QuestionState
}

/**
 * クイズ画面
 * 問題（国旗・現地語の国名など）を見て国名を入力し、候補から選んで回答する。QUESTION_COUNT 問で結果を表示する
 *
 * @param topic 出題するクイズの種類
 * @param onBack トップ画面に戻るときに呼ばれる
 */
@Composable
fun QuizScreen(topic: QuizTopic, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val askedQuizIds = remember { mutableStateListOf<Long>() }
    var correctCount by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<QuestionState>(QuestionState.Loading) }
    var finished by remember { mutableStateOf(false) }

    fun loadNext() {
        state = QuestionState.Loading
        scope.launch {
            state = runCatching {
                val quiz = ApiClient.randomQuiz(topic = topic, excludeIds = askedQuizIds)
                val content = loadContent(quiz)
                askedQuizIds += quiz.quizId
                QuestionState.Answering(quizId = quiz.quizId, content = content)
            }.getOrElse {
                println("Failed to load ${topic.name} quiz: $it")
                QuestionState.Error("問題を取得できませんでした\n$it")
            }
        }
    }

    fun answer(quizId: Long, content: QuestionContent, countryId: Long) {
        scope.launch {
            runCatching { ApiClient.answer(quizId, countryId) }
                .onSuccess { result ->
                    if (result.correct) correctCount++
                    state = QuestionState.Answered(quizId = quizId, content = content, result = result)
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
                    title = "${topic.label}クイズ",
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
                        QuestionState.Loading -> QuestionCard(content = null)
                        is QuestionState.Error -> ErrorContent(message = current.message, onRetry = ::loadNext)
                        is QuestionState.Answering -> {
                            QuestionCard(content = current.content)
                            Spacer(Modifier.height(24.dp))
                            AnswerInput(
                                // 問題が変わったら入力欄をリセットする
                                quizId = current.quizId,
                                onAnswer = { answer(current.quizId, current.content, it) },
                            )
                        }
                        is QuestionState.Answered -> {
                            QuestionCard(content = current.content)
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

/** 出題文があれば文字の問題、なければ画像の問題として読み込む */
private suspend fun loadContent(quiz: QuizQuestion): QuestionContent =
    quiz.hintText
        ?.let { QuestionContent.Text(lines = it.lines()) }
        ?: QuestionContent.Image(
            image = org.jetbrains.skia.Image.makeFromEncoded(ApiClient.quizImage(quiz.quizId)).toComposeImageBitmap(),
        )
