package com.example.raon.features.chat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// features/chat/data/local/ChatDao.kt

// Room에서 사용할 DAO 인터페이스 -> Sql처럼 DB를 다루는것

@Dao
interface ChatDao {
    // --- 채팅 메시지 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("SELECT * FROM chat_messages WHERE roomId = :roomId ORDER BY sendTime ASC")
    fun getMessages(roomId: Long): Flow<List<ChatMessageEntity>>

    // --- 채팅방 목록 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRooms(rooms: List<ChatRoomEntity>)

    @Query("SELECT * FROM chat_rooms ORDER BY lastMessageTime DESC")
    fun getChatRooms(): Flow<List<ChatRoomEntity>>

    // 🔽🔽🔽 [필수 추가] 채팅 목록 실시간 갱신을 위한 함수 🔽🔽🔽
    /**
     * 채팅방의 마지막 메시지, 시간, 안 읽은 개수를 업데이트합니다.
     * 안 읽은 개수(unreadCount)는 기존 값 + 1을 해줍니다.
     */
    @Query(
        """
        UPDATE chat_rooms 
        SET 
            lastMessage = :lastMessage, 
            lastMessageTime = :lastMessageTime, 
            unreadCount = unreadCount + 1 
        WHERE 
            chatroomId = :roomId
    """
    )
    suspend fun updateChatRoomSummary(roomId: Long, lastMessage: String, lastMessageTime: String)


    // ▼▼▼ [신규 추가] MainViewModel에서 읽음 처리를 위해 호출할 함수 ▼▼▼
    /**
     * 특정 채팅방의 안 읽은 메시지 개수(unreadCount)를 0으로 설정합니다.
     */
    @Query("UPDATE chat_rooms SET unreadCount = 0 WHERE chatroomId = :chatId")
    suspend fun markRoomAsRead(chatId: Long)


    /**
     * 특정 채팅방의 모든 메시지(chat_messages)를 '읽음'으로 설정합니다.
     * (ChatMessageEntity의 isRead가 Boolean이므로 1을 사용)
     */
    @Query("UPDATE chat_messages SET isRead = 1 WHERE roomId = :chatId")
    suspend fun markMessagesAsReadInDb(chatId: Long)

}