package com.example.raon.features.search.data.remote.dto


import com.example.raon.features.search.ui.model.SearchItemUiModel
import com.google.gson.annotations.SerializedName
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

// 전체 응답 구조
data class SearchItemListDto(
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: SearchItemDataDto
)

// 페이징 및 아이템 목록 데이터
data class SearchItemDataDto(
    @SerializedName("products") val items: List<SearchItemDto>,
    @SerializedName("currentPage") val currentPage: Int,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("totalElements") val totalElements: Int
)

// 개별 아이템 상세 정보 (수정된 부분)
data class SearchItemDto(
    @SerializedName("productId") val itemId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("price") val price: Int,
    @SerializedName("thumbnail") val thumbnail: String?,
    @SerializedName("location") val location: SearchLocationDto,
    @SerializedName("seller") val seller: SearchSellerDto,
    @SerializedName("favoriteCount") val favoriteCount: Int,
    @SerializedName("createdAt") val createdAt: String,

    // 👇 아래 4개 필드가 추가되었습니다.
    @SerializedName("viewCount") val viewCount: Int,
    @SerializedName("condition") val condition: String,
    @SerializedName("tradeType") val tradeType: String,
    @SerializedName("status") val status: String
)

// 위치 정보
data class SearchLocationDto(
    @SerializedName("locationId") val locationId: Int,
    @SerializedName("address") val address: String
)

// 판매자 정보
data class SearchSellerDto(
    @SerializedName("userId") val userId: Int,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("profileImage") val profileImage: String?
)

/**
 * SearchItemDto (Data Layer) 객체를 Product (Domain Layer) 객체로 변환합니다.
 * UI에서 필요한 데이터만 추출하여 깔끔한 모델을 만듭니다.
 */
fun SearchItemDto.toDomain(): SearchItemUiModel {
    return SearchItemUiModel(
        id = this.itemId, // Long 타입으로 변환
        title = this.title,
        location = this.location.address,
        price = this.price,
        imageUrl = this.thumbnail.toString(),
        likes = this.favoriteCount,
//        timeAgo = this.createdAt,
        timeAgo = formatTimeAgo(this.createdAt),

        viewCount = this.viewCount,
        comments = 0,
        status = this.status
    )

}


/**
 * 시간 문자열을 "N년 전"과 같은 상대 시간으로 변환하는 함수
 */
private fun formatTimeAgo(createdAt: String): String {
    return try {
        val createdTime = OffsetDateTime.parse(createdAt)
        val now = OffsetDateTime.now()

        // 년, 월, 일, 시, 분 단위로 시간 차이를 계산합니다.
        val years = ChronoUnit.YEARS.between(createdTime, now)
        val months = ChronoUnit.MONTHS.between(createdTime, now)
        val days = ChronoUnit.DAYS.between(createdTime, now)
        val hours = ChronoUnit.HOURS.between(createdTime, now)
        val minutes = ChronoUnit.MINUTES.between(createdTime, now)

        // [핵심 로직] 년 > 월 > 일 > 시간 > 분 순서로 확인합니다.
        when {
            years > 0 -> "${years}년 전"
            months > 0 -> "${months}달 전"
            days > 0 -> "${days}일 전"
            hours > 0 -> "${hours}시간 전"
            minutes > 0 -> "${minutes}분 전"
            else -> "방금 전"
        }
    } catch (e: Exception) {
        // 날짜 형식이 잘못되었을 경우를 대비한 예외 처리
        "시간 정보 없음"
    }
}