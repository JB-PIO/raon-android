package com.example.raon.features.chat.domain.model


// 앱 전체(ViewModel, UI)에서 사용할 데이터 모델
data class ChatMessage(
    val messageId: Long,
    val chatRoomId: Long,
    val senderId: Int,
    val senderNickname: String,
    val senderProfileUrl: String?,
    val content: String,
    val imageUrl: String?,
    val timestamp: String,
    val isFromMe: Boolean,
    val originalTimestamp: String // 정렬 및 API 전송용 원본 시간 추가
    
)
