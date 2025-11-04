package com.example.raon.features.chat.domain.repository

//import com.example.raon.features.chat.data.remote.dto.ChatRoomListDTO
// [신규] MainViewModel이 DTO를 전달하므로 Import 필요
import com.example.raon.core.network.ApiResult
import com.example.raon.core.network.dto.ApiResponse
import com.example.raon.features.chat.data.local.ChatRoomEntity // [수정 1] import 추가
import com.example.raon.features.chat.data.remote.dto.ChatRoomDetailResponse
import com.example.raon.features.chat.data.remote.dto.ChatRoomInfo
import com.example.raon.features.chat.data.remote.dto.ChatRoomListDto
import com.example.raon.features.chat.data.remote.dto.MessageListDto
import com.example.raon.features.chat.data.remote.dto.SendMessageResponseDto
import com.example.raon.features.chat.data.remote.dto.ai.FraudData
import com.example.raon.features.chat.data.remote.dto.ai.FraudDetectionRequestDto
import com.example.raon.features.chat.data.remote.dto.ai.ImageAnalysisResponseDto
import com.example.raon.features.chat.domain.model.ChatMessage
import com.example.raon.features.chat.domain.model.ChatRoom
import kotlinx.coroutines.flow.Flow


// 채팅 데이터에 접근하기 위한 인터페이스
interface ChatRepository {


    //서버로 메시지를 전송합니다.
    suspend fun sendMessage(
        chatRoomId: Long,   // 채팅방 번호
        chatMessage: String // 채팅 메시지
    ): ApiResult<ApiResponse<SendMessageResponseDto>>


    // 서버에서 채팅방 리스트를 가여오는 함수
    suspend fun getChatRoomList(page: Int): ApiResult<ApiResponse<ChatRoomListDto>>


    suspend fun getChatRoomDetails(chatId: Long): ApiResult<ChatRoomDetailResponse>


    // HTTP로 과거 채팅 메시지를 가져오는 함수
    suspend fun getMessageList(chatId: Long, page: Int): ApiResult<ApiResponse<MessageListDto>>


    // --- STOMP 실시간 채팅을 위한 함수들 추가 ---
    /**
     * STOMP 세션을 연결하고 특정 채팅방의 메시지를 구독합니다.
     * @param chatRoomId 구독할 채팅방 ID
     */
    suspend fun connectStomp(chatRoomId: Long)

    /**
     * [수정] 구독 중인 STOMP 메시지 흐름(Flow)을 '단순히' 제공합니다.
     * (이 함수는 더 이상 DB에 쓰지 않습니다)
     */
    fun observeMessages(chatId: Long): Flow<String>

    /**
     * [신설] STOMP 메시지를 구독하며 Room DB에 저장(캐시)하는 함수.
     * 이 함수는 SSoT를 위해 앱에서 '단 한 번만' 호출되어야 합니다.
     */
    suspend fun cacheStompMessages()


    /**
     * STOMP를 통해 실시간 메시지를 전송합니다.
     * @param recipientId 메시지를 받을 상대방의 사용자 ID
     * @param message 보낼 메시지 내용
     */
//    suspend fun sendStompMessage(chatRoomId: Long, message: String)


    // [ Stomp 세션 연결 해제 ]
    suspend fun disconnectStomp()


    // [ ai 채팅 사기 탐지 함수 ]
    suspend fun detectFraud(
        userId: Long,
        request: FraudDetectionRequestDto
    ): ApiResult<ApiResponse<FraudData>> // <--- ✅ 'ApiResult'로 감싸주세요.


    // [ AI 이미지 분석 함수 ]
    suspend fun analyzeImages(chatRoomId: Long): ApiResult<ApiResponse<ImageAnalysisResponseDto>>


    // [ 메시지 읽음 처리 함수]
    suspend fun markMessagesAsRead(chatId: Long): ApiResult<ApiResponse<Unit>>


    // 🔽🔽🔽 SSoT용 함수 2개 추가 (인터페이스 선언) 🔽🔽🔽
    /**
     * [신설] Room DB로부터 특정 채팅방의 메시지 목록을 Flow로 관찰합니다.
     */
    fun getMessagesFromDb(chatId: Long): Flow<List<ChatMessage>>

    /**
     * [신설] Room DB로부터 전체 채팅방 목록을 Flow로 관찰합니다.
     */
    fun getChatRoomsFromDb(): Flow<List<ChatRoom>> // <- 'ChatRoom' Import로 에러 해결


    // ▼▼▼ [신규] MainViewModel이 호출할 SSoT 쓰기 함수 ▼▼▼

    /**
     * [신설] ViewModel에서 Presigned URL 처리가 완료된 목록을 Room에 저장(캐시)합니다.
     */
    suspend fun cacheChatRoomList(chatRooms: List<ChatRoomInfo>)

    /**
     * [신설] ViewModel이 특정 채팅방을 Room에서 '읽음' 처리합니다.
     */
    suspend fun markRoomAsReadInDb(chatId: Long)

    // ▼▼▼ [ 2. 이 함수 추가 ] ▼▼▼
    /**
     * [신규] ViewModel이 '수동으로 생성한' 새 채팅방 Entity 1개를 Room에 저장합니다.
     */
    suspend fun cacheSingleChatRoom(roomEntity: ChatRoomEntity)
    // ▲▲▲ [ 수정 완료 ] ▲▲▲


    // ▼▼▼ [로그아웃/회원탈퇴 시 추가] ▼▼▼

    /**
     * 로컬 DB의 모든 채팅방 목록을 삭제합니다.
     */
    suspend fun deleteAllLocalChatRooms()

    /**
     * 로컬 DB의 모든 채팅 메시지를 삭제합니다.
     */
    suspend fun deleteAllLocalChatMessages()
}