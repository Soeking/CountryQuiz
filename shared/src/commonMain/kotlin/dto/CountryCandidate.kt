package dto

import kotlinx.serialization.Serializable

/** 国名検索 API (/api/countries/search) のレスポンス要素 */
@Serializable
data class CountryCandidate(
    val countryId: Long,
    val code: String,
    /** 表示用の名称（ヒットした表記を返す） */
    val matchedName: String,
)
