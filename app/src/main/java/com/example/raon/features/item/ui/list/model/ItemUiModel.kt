package com.example.raon.features.item.ui.list.model


// Item 목록 화면의 각 아이템을 나타내는 UI 모델
// 서버에서 받은 DTO를 이 모델로 변환하여 UI에 사용
data class ItemUiModel( // 이름 변경
    val id: Int,
    val title: String,
    val location: String,
    val timeAgo: String,
    val price: Int,
    val imageUrl: String,
    val comments: Int,  // 뎃글 개수
    val likes: Int,     // 좋아요 수, 관심 글 수
    val viewCount: Int,  // 조회수
    val status: String

)