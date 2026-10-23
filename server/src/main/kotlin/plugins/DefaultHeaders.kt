package plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.defaultheaders.*

/**
 * 検索エンジンにインデックスさせない
 * HTML 以外（API・画像など）も対象にするため、meta タグではなくレスポンスヘッダーで全レスポンスに付ける
 * robots.txt で Disallow するとクロールされずにこのヘッダーが読まれないので、robots.txt は置かない
 */
fun Application.configureDefaultHeaders() {
    install(DefaultHeaders) {
        header("X-Robots-Tag", "noindex, nofollow")
    }
}
