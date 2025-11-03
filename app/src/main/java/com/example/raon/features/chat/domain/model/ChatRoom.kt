package com.example.raon.features.chat.domain.model

data class ChatRoom(
    val roomId: Long,
    val opponentProfileUrl: String?,
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int,

    // ▼▼▼ [신규] 이 3개 필드가 모델에 포함되어야 합니다. ▼▼▼
    val productId: Int,
    val sellerId: Int,
    val buyerId: Int,
    val sellerNickname: String,
    val buyerNickname: String,
)