package screen.quiz

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import api.ApiClient
import dto.AnswerResult
import dto.CountryCandidate
import dto.QuizQuestion
import kotlinx.coroutines.launch
import model.AnswerMode
import model.QuizTopic

/** これより幅が狭い画面（スマホの縦向き）は詰めたレイアウトにする */
private val COMPACT_WIDTH = 600.dp

/** 問題カードの高さ（画面の高さに対する割合と、その上限・下限） */
private const val QUESTION_HEIGHT_RATIO = 0.3f
private val MIN_QUESTION_HEIGHT = 120.dp
private val MAX_QUESTION_HEIGHT = 240.dp

/** 1 回のプレイで出題する問題数 */
internal const val QUESTION_COUNT = 10

/** 出題中の問題の状態 */
private sealed interface QuestionState {
    data object Loading : QuestionState
    data class Error(val message: String) : QuestionState
    /** @param choices 4 択モードの選択肢。入力モードでは空 */
    data class Answering(val quizId: Long, val content: QuestionContent, val choices: List<CountryCandidate>) : QuestionState
    data class Answered(val quizId: Long, val content: QuestionContent, val result: AnswerResult) : QuestionState
}

/**
 * クイズ画面
 * 問題（国旗・現地語の国名など）を見て、国名を入力して候補から選ぶか、4 択から選んで回答する。QUESTION_COUNT 問で結果を表示する
 *
 * @param topic 出題するクイズの種類
 * @param mode 回答のしかた
 * @param onBack トップ画面に戻るときに呼ばれる
 */
@Composable
fun QuizScreen(topic: QuizTopic, mode: AnswerMode, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val askedQuizIds = remember { mutableStateListOf<Long>() }
    var correctCount by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<QuestionState>(QuestionState.Loading) }
    var finished by remember { mutableStateOf(false) }

    fun loadNext() {
        state = QuestionState.Loading
        scope.launch {
            state = runCatching {
                val quiz = ApiClient.randomQuiz(topic = topic, excludeIds = askedQuizIds, choiceCount = mode.choiceCount)
                val content = loadContent(quiz)
                askedQuizIds += quiz.quizId
                QuestionState.Answering(quizId = quiz.quizId, content = content, choices = quiz.choices)
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
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // スマホ（幅が狭い）では余白を詰め、ヘッダーを 2 段にする
            val compactWidth = maxWidth < COMPACT_WIDTH
            val gap = if (compactWidth) 16.dp else 24.dp
            // 高さが低い画面（スマホ・横向き・ソフトキーボード表示中）では問題カードを小さくして、4 択や入力欄が収まるようにする
            val questionHeight = (maxHeight * QUESTION_HEIGHT_RATIO).coerceIn(MIN_QUESTION_HEIGHT, MAX_QUESTION_HEIGHT)

            Column(
                // 画面に収まらないとき（ソフトキーボード表示中など）はスクロールできるようにする
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(if (compactWidth) 16.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Header(
                        title = "${topic.label}クイズ（${mode.label}）",
                        questionNumber = askedQuizIds.size,
                        correctCount = correctCount,
                        finished = finished,
                        compact = compactWidth,
                        onBack = onBack,
                    )
                    Spacer(Modifier.height(gap))

                    if (finished) {
                        ResultContent(correctCount = correctCount, onRestart = ::restart, onBack = onBack)
                    } else {
                        when (val current = state) {
                            QuestionState.Loading -> QuestionCard(content = null, height = questionHeight)
                            is QuestionState.Error -> ErrorContent(message = current.message, onRetry = ::loadNext)
                            is QuestionState.Answering -> {
                                QuestionCard(content = current.content, height = questionHeight)
                                Spacer(Modifier.height(gap))
                                val onAnswer = { countryId: Long -> answer(current.quizId, current.content, countryId) }
                                when (mode) {
                                    AnswerMode.INPUT -> AnswerInput(
                                        // 問題が変わったら入力欄をリセットする
                                        quizId = current.quizId,
                                        onAnswer = onAnswer,
                                    )
                                    AnswerMode.CHOICE -> AnswerChoices(
                                        quizId = current.quizId,
                                        choices = current.choices,
                                        onAnswer = onAnswer,
                                    )
                                }
                            }
                            is QuestionState.Answered -> {
                                QuestionCard(content = current.content, height = questionHeight)
                                Spacer(Modifier.height(gap))
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
}

/** 出題文があれば文字の問題、なければ画像の問題として読み込む */
private suspend fun loadContent(quiz: QuizQuestion): QuestionContent =
    quiz.hintText
        ?.let { QuestionContent.Text(lines = it.lines()) }
        ?: QuestionContent.Image(
            image = org.jetbrains.skia.Image.makeFromEncoded(ApiClient.quizImage(quiz.quizId)).toComposeImageBitmap(),
        )
