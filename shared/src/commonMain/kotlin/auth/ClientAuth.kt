package auth

/**
 * web クライアントからの API 呼び出しであることを示すヘッダー
 * カスタムヘッダー付きのリクエストはクロスオリジンだと preflight が必要になるため、
 * CORS を許可していない他サイトのページからは送れない
 */
const val CLIENT_HEADER_NAME = "X-CountryQuiz-Client"
const val CLIENT_HEADER_VALUE = "web"
