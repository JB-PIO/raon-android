package com.example.raon.features.chat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// features/chat/data/local/ChatRoomEntity.kt

@Entity(tableName = "chat_rooms")
data class ChatRoomEntity(
    @PrimaryKey
    val chatroomId: Long,
    val opponentProfileUrl: String?,
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int,
    val productId: Int,
    val sellerId: Int,
    val buyerId: Int,
    val sellerNickname: String,
    val buyerNickname: String,
)