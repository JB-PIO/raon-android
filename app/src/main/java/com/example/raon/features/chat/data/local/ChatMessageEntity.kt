package com.example.raon.features.chat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// features/chat/data/local/ChatMessageEntity.kt

// features/chat/data/local/ChatMessageEntity.kt

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val messageId: Long,
    val roomId: Long,
    val senderId: Long,
    val senderName: String,
    val senderProfileUrl: String?,
    val content: String,
    val sendTime: String, // 🔽🔽🔽 [수정] DTO의 'originalTimestamp' (UTC 시간)을 여기에 저장
    val messageType: String,    // 이미지인지, 택스트인지
    val isRead: Boolean,
    val imageUrl: String? // 🔽🔽🔽 [필수 추가] 이 필드가 빠져있었습니다.
)