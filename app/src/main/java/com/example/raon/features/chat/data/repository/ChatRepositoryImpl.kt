package com.example.raon.features.chat.data.repository

import android.util.Log
import com.example.raon.core.network.ApiResult
import com.example.raon.core.network.dto.ApiResponse
import com.example.raon.core.network.handleApi
import com.example.raon.features.chat.data.local.ChatDao
import com.example.raon.features.chat.data.local.ChatMessageEntity
import com.example.raon.features.chat.data.local.ChatRoomEntity
import com.example.raon.features.chat.data.remote.StompService
import com.example.raon.features.chat.data.remote.api.ChatApiService
import com.example.raon.features.chat.data.remote.dto.ChatMessageDto
import com.example.raon.features.chat.data.remote.dto.ChatRoomDetailResponse
import com.example.raon.features.chat.data.remote.dto.ChatRoomInfo
import com.example.raon.features.chat.data.remote.dto.ChatRoomListDto
import com.example.raon.features.chat.data.remote.dto.MessageDto
import com.example.raon.features.chat.data.remote.dto.MessageListDto
import com.example.raon.features.chat.data.remote.dto.SendMessageRequestDto
import com.example.raon.features.chat.data.remote.dto.SendMessageResponseDto
import com.example.raon.features.chat.data.remote.dto.ai.FraudData
import com.example.raon.features.chat.data.remote.dto.ai.FraudDetectionRequestDto
import com.example.raon.features.chat.data.remote.dto.ai.ImageAnalysisResponseDto
import com.example.raon.features.chat.domain.model.ChatMessage
import com.example.raon.features.chat.domain.model.ChatRoom
import com.example.raon.features.chat.domain.repository.ChatRepository
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject


/**
 * ChatRepository의 실제 구현체.
 * [최종 수정] 모든 DTO, Entity, Domain Model 충돌을 해결한 버전.
 */
class ChatRepositoryImpl @Inject constructor(
    private val chatApiService: ChatApiService,
    private val stompService: StompService,
    private val chatDao: ChatDao,
    private val gson: Gson
) : ChatRepository {

    // 💡 [추가] STOMP 캐싱 로직이 현재 실행 중인지 확인하는 플래그
    private var isStompCachingRunning = false

    // ▼▼▼ 1. HTTP GET으로 과거 메시지를 불러오는 실제 구현 ▼▼▼
    override suspend fun getMessageList(
        chatId: Long,
        page: Int
    ): ApiResult<ApiResponse<MessageListDto>> {
        val result = handleApi { chatApiService.getMessages(chatId, page) }

        // 성공 시 'MessageDto'를 Room에 저장
        if (result is ApiResult.Success) {
            // DTO 구조: result.data.data.messages (List<MessageDto>)
            result.data?.data?.messages?.let { messageDtoList ->
                try {
                    // 'MessageDto.toEntity()' 매퍼 사용 (하단 정의)
                    val entities = messageDtoList.map { it.toEntity() }
                    chatDao.insertMessages(entities) // (DAO에 OnConflictStrategy.REPLACE 필요)
                } catch (e: Exception) {
                    Log.e("ChatRepository", "getMessageList DB 저장 실패", e)
                }
            }
        }
        return result
    }


    // 서버에 get chat 요청을 보냄 -> 채팅방 관련 상세 데이터를 줌
    override suspend fun getChatRoomDetails(chatId: Long): ApiResult<ChatRoomDetailResponse> {
        Log.d("ChatRepository_getChat", "🚀 Fetching chat room details for chatId: $chatId")
        val result = handleApi { chatApiService.getChatRoomDetails(chatId) }

        // 성공 시 'ChatMessageDto'를 Room에 저장
        if (result is ApiResult.Success) {
            // DTO 구조: result.data.messages (List<ChatMessageDto>)
//            result.data.message.let { chatMessageDtoList ->
//                try {
//                    // 'ChatMessageDto.toEntity()' 매퍼 사용 (하단 정의)
//                    val entities = chatMessageDtoList.map { it.toEntity() }
//                    chatDao.insertMessages(entities) // (DAO에 OnConflictStrategy.REPLACE 필요)
//                    Log.d("ChatRepository", " Room DB에 ${entities.size}개 메시지 덮어쓰기 완료")
//                } catch (e: Exception) {
//                    Log.e("ChatRepository", "getChatRoomDetails DB 저장 실패", e)
//                }
//            }
        }
        Log.d("ChatRepository_getChat", "✅ Chat room details result: $result")
        return result
    }


    // 서버로 채팅을 보내는 Repository 함수
    override suspend fun sendMessage(
        chatRoomId: Long,
        chatMessage: String
    ): ApiResult<ApiResponse<SendMessageResponseDto>> {
        val requestDto = SendMessageRequestDto(content = chatMessage)
        val result = handleApi { chatApiService.sendMessage(chatRoomId, requestDto) }

        // 🔽 [수정] 성공 시 'SendMessageResponseDto'를 Room에 저장
        if (result is ApiResult.Success) {
            // DTO 구조: result.data.data (SendMessageResponseDto)
            result.data.data?.let { responseDto ->
                try {
                    // 🔽 [충돌 해결] 'SendMessageResponseDto.toEntity()' 매퍼 사용 (하단 정의)
                    val entity = responseDto.toEntity()
                    chatDao.insertMessage(entity) // (DAO에 OnConflictStrategy.REPLACE 필요)

                    val updatedRows = chatDao.updateMyChatRoomSummary(
                        roomId = entity.roomId,
                        lastMessage = entity.content,
                        lastMessageTime = entity.sendTime, // Entity의 원본 시간

                    )
                    Log.d("ChatRepository", "✅ 전송 성공 메시지 Room DB 저장 완료")
                } catch (e: Exception) {
                    Log.e("ChatRepository", "sendMessage DB 저장 실패", e)
                }
            }
        }
        return result
    }


    // [수정] ViewModel이 Presigned URL을 처리해야 하므로, Room 저장 로직 제거
    override suspend fun getChatRoomList(page: Int): ApiResult<ApiResponse<ChatRoomListDto>> {
        val result = handleApi { chatApiService.getChats(page) }

        // 🔽 [수정] 성공 시 'ChatRoomInfo'를 Room에 저장하는 로직 '제거'
        // Presigned URL 처리를 ViewModel에서 한 뒤, cacheChatRoomList를 호출할 것임.
        // if (result is ApiResult.Success) {
        //    ... (기존 Room 저장 로직 모두 제거) ...
        // }

        return result
    }

    // --- STOMP 관련 함수 구현 ---

    override suspend fun connectStomp(chatRoomId: Long) {
        stompService.connectAndSubscribe(chatRoomId)
    }

    /**
     * 이 함수는 이제 DB 업데이트 로직을 '제거'하고,
     * 원본 STOMP Flow를 그대로 반환합니다.
     */
    override fun observeMessages(chatId: Long): Flow<String> {
        Log.d(
            "ChatRepository",
            "🚀 Observing messages (No DB save) for : ${stompService.messages}"
        )
        // DAO 로직을 모두 제거하고 stompService.messages를 직접 반환
        return stompService.messages
    }

    /**
     * STOMP 메시지를 DB에 저장하는, 단일 책임을 가진 함수.
     * [수정됨] 새 채팅방 감지 및 추가 로직을 포함합니다.
     */
//    override suspend fun cacheStompMessages() {
//
//        // 1. [수정] 이미 실행 중이면 로그를 남기고 즉시 종료합니다.
//        if (isStompCachingRunning) {
//            Log.d("ChatRepository", "⚠️ STOMP 메시지 캐싱이 이미 실행 중입니다. 중복 호출을 무시합니다.")
//            return
//        }
//
//        // 2. [수정] 플래그를 설정하고, try-finally 구문을 사용하여 종료 시 플래그를 해제합니다.
//        isStompCachingRunning = true
//        Log.d("ChatRepository", "🚀 Starting STOMP message caching...")
//        try {
//            // stompService.messages를 구독하여 DB에 저장
//            stompService.messages.collect { messagePayload ->
//                // 1. (DB 저장 로직)
//                try {
//                    // STOMP는 'ChatMessageDto' 형식을 사용한다고 가정
//                    val messageDto = gson.fromJson(messagePayload, ChatMessageDto::class.java)
//                    val entity = messageDto.toEntity()
//
//                    chatDao.insertMessage(entity) // (DAO에 OnConflictStrategy.REPLACE 필요)
//                    Log.d("ChatRepository", "STOMP 메시지 DB 저장 성공")
//
//                    // ▼▼▼ [수정] 2단계: '스마트' 업데이트 로직 (새 채팅방 감지) ▼▼▼
//                    // 2-1. 채팅방 목록 테이블 업데이트 시도 (ChatDao.kt가 :Int를 반환한다고 가정)
//                    val updatedRows = chatDao.updateChatRoomSummary(
//                        roomId = entity.roomId,
//                        lastMessage = entity.content,
//                        lastMessageTime = entity.sendTime // Entity의 원본 시간
//                    )
//
//                    // 2-2. [핵심] 만약 업데이트된 행이 0개라면 (updatedRows == 0),
//                    //      이것은 '새로운 채팅방'이라는 의미입니다.
//                    if (updatedRows == 0) {
//                        Log.d(
//                            "ChatRepository",
//                            "🔥 STOMP: 새로운 채팅방(${entity.roomId}) 감지! Placeholder Entity를 생성합니다."
//                        )
//
//
//                        // 2-3. 임시 채팅방 정보를 생성하여 Room DB에 INSERT
//                        createPlaceholderChatRoom(entity)
//                    } else {
//                        Log.d("ChatRepository", "STOMP: 기존 채팅방(${entity.roomId}) 요약 DB 업데이트 성공")
//                    }
//                    // ▲▲▲ [수정] 완료 ▲▲▲
//
//                } catch (e: Exception) {
//                    Log.e("ChatRepository", "STOMP 메시지 파싱 또는 DB 저장 실패", e)
//                } finally {
//                    // 3. 💡 [수정] 예외나 취소로 collect가 종료되면 플래그를 해제합니다.
//                    isStompCachingRunning = false
//                    Log.d("ChatRepository", "STOMP message caching collector terminated.")
//                }
//            }
//        } catch (e: CancellationException) {
//            Log.d("ChatRepository", "STOMP caching이 취소되었습니다.")
//            throw e
//        } catch (e: Exception) {
//            Log.e("ChatRepository", "STOMP caching collect 실패", e)
//        }
//    }


    /**
     * STOMP 메시지를 DB에 저장하는, 단일 책임을 가진 함수.
     * [수정됨] 새 채팅방 감지 시 'getChatRoomDetails'를 호출하여 썸네일 등을 가져옵니다.
     */
    override suspend fun cacheStompMessages() {

        // 1. [수정] 이미 실행 중이면 로그를 남기고 즉시 종료합니다.
        if (isStompCachingRunning) {
            Log.d("ChatRepository", "⚠️ STOMP 메시지 캐싱이 이미 실행 중입니다. 중복 호출을 무시합니다.")
            return
        }

        // 2. [수정] 플래그를 설정하고, try-finally 구문을 사용하여 종료 시 플래그를 해제합니다.
        isStompCachingRunning = true
        Log.d("ChatRepository", "🚀 Starting STOMP message caching...")
        try {
            // stompService.messages를 구독하여 DB에 저장
            stompService.messages.collect { messagePayload ->
                // 1. (DB 저장 로직)
                try {
                    // STOMP는 'ChatMessageDto' 형식을 사용한다고 가정
                    val messageDto = gson.fromJson(messagePayload, ChatMessageDto::class.java)
                    val entity = messageDto.toEntity()

                    chatDao.insertMessage(entity) // (DAO에 OnConflictStrategy.REPLACE 필요)
                    Log.d("ChatRepository", "STOMP 메시지 DB 저장 성공")

                    // ▼▼▼ [수정] 2단계: '스마트' 업데이트 로직 (새 채팅방 감지) ▼▼▼
                    // 2-1. 채팅방 목록 테이블 업데이트 시도 (ChatDao.kt가 :Int를 반환한다고 가정)
                    val updatedRows = chatDao.updateChatRoomSummary(
                        roomId = entity.roomId,
                        lastMessage = entity.content,
                        lastMessageTime = entity.sendTime // Entity의 원본 시간
                    )

                    // 2-2. [핵심] 만약 업데이트된 행이 0개라면 (updatedRows == 0),
                    //      이것은 '새로운 채팅방'이라는 의미입니다.
                    if (updatedRows == 0) {
                        Log.d(
                            "ChatRepository",
                            "🔥 STOMP: 새로운 채팅방(${entity.roomId}) 감지! 서버에서 상세 정보를 요청합니다."
                        )

                        // 2-3. [!! 변경된 로직 !!] 'getChatRoomDetails' 호출
                        when (val apiResult = getChatRoomDetails(entity.roomId)) {

                            is ApiResult.Success -> {
                                // 2-4. API 호출 성공. 응답의 'data' 필드(ChatRoomDetailDataDto)를 가져옴
                                val responseData = apiResult.data?.data // (ChatRoomDetailDataDto?)

                                if (responseData != null) {
                                    try {
                                        // 2-5. API 정보 + STOMP 정보를 조합하여 'ChatRoomEntity' 생성
                                        val newRoomEntity = ChatRoomEntity(
                                            // --- API(responseData)에서 가져오는 정보 ---
                                            chatroomId = responseData.chatId,
                                            // ▼▼▼ 요청하신 썸네일 URL ▼▼▼
                                            opponentProfileUrl = responseData.product.thumbnail,
                                            productId = responseData.product.productId.toInt(),
                                            sellerId = responseData.seller.userId,
                                            buyerId = responseData.buyer.userId,
                                            sellerNickname = responseData.seller.nickname,
                                            buyerNickname = responseData.buyer.nickname,

                                            // --- STOMP(entity)에서 가져오는 정보 ---
                                            lastMessage = entity.content,
                                            lastMessageTime = entity.sendTime,
                                            unreadCount = 1 // 새 방, 새 메시지이므로 1
                                        )

                                        chatDao.insertChatRooms(listOf(newRoomEntity))
                                        Log.d(
                                            "ChatRepository",
                                            "✅ STOMP: 새 채팅방(${entity.roomId}) 상세 정보 Room DB 저장 완료"
                                        )

                                    } catch (e: Exception) {
                                        // DTO 필드가 null이거나 (e.g. responseData.seller.nickname)
                                        // DB 저장 중 예외 발생 시 Fallback
                                        Log.e(
                                            "ChatRepository",
                                            "STOMP: 새 채팅방 상세 정보 파싱 또는 DB 저장 실패. Placeholder 생성.",
                                            e
                                        )
                                        createPlaceholderChatRoom(entity) // Fallback
                                    }
                                } else {
                                    // API 호출은 성공했으나 (code: "...") 응답의 data 필드가 null인 경우
                                    Log.e(
                                        "ChatRepository",
                                        "STOMP: 새 채팅방(${entity.roomId}) 상세 정보 응답 데이터가 null입니다. Placeholder 생성."
                                    )
                                    createPlaceholderChatRoom(entity) // Fallback
                                }
                            }

                            is ApiResult.Error -> {
                                // 2-6. API 호출 실패 시 (e.g. 404, 500), 기존처럼 Placeholder 생성 (Fallback)
                                Log.e(
                                    "ChatRepository",
                                    "STOMP: 새 채팅방(${entity.roomId}) 상세 정보 요청 실패. Placeholder를 생성합니다. Error: ${apiResult.errorBody}"
                                )
                                createPlaceholderChatRoom(entity)
                            }

                            is ApiResult.Exception -> {
                                // 2-7. API 호출 중 네트워크 예외 발생 시, Placeholder 생성 (Fallback)
                                Log.e(
                                    "ChatRepository",
                                    "STOMP: 새 채팅방(${entity.roomId}) 상세 정보 요청 중 예외 발생. Placeholder를 생성합니다.",
                                    apiResult.e
                                )
                                createPlaceholderChatRoom(entity)
                            }
                        }

                    } else {
                        Log.d("ChatRepository", "STOMP: 기존 채팅방(${entity.roomId}) 요약 DB 업데이트 성공")
                    }
                    // ▲▲▲ [수정] 완료 ▲▲▲

                } catch (e: Exception) {
                    Log.e("ChatRepository", "STOMP 메시지 파싱 또는 DB 저장 실패", e)
                } finally {
                    // 3. 💡 [수정] 예외나 취소로 collect가 종료되면 플래그를 해제합니다.
                    isStompCachingRunning = false
                    Log.d("ChatRepository", "STOMP message caching collector terminated.")
                }
            }
        } catch (e: CancellationException) {
            Log.d("ChatRepository", "STOMP caching이 취소되었습니다.")
            throw e
        } catch (e: Exception) {
            Log.e("ChatRepository", "STOMP caching collect 실패", e)
        }
    }


    // ▼▼▼ [신규] '최소한의 변경'을 위한 새 임시 채팅방 생성 함수 추가 ▼▼▼
    /**
     * STOMP 메시지만으로 '임시' ChatRoomEntity를 생성합니다.
     * (이미지, 닉네임, 상품ID 등은 없음)
     */
    private suspend fun createPlaceholderChatRoom(message: ChatMessageEntity) {
        // 주의: 내 닉네임(Buyer/Seller 중 누구인지)을 알 수 없으므로 임시로 '...' 처리
        // ViewModel에서 정식 목록을 불러올 때 이 정보가 채워집니다.
        val placeholderRoom = ChatRoomEntity(
            chatroomId = message.roomId,
            opponentProfileUrl = null, // Presigned URL 정보가 없으므로 null
            lastMessage = message.content,
            lastMessageTime = message.sendTime,
            unreadCount = 1, // 새 메시지이므로 1
            productId = 0, // 정보가 없으므로 0
            sellerId = 0, // 정보가 없으므로 0
            buyerId = 0, // 정보가 없으므로 0
            sellerNickname = message.senderName, // STOMP 메시지에서 받은 닉네임
            buyerNickname = "..." // 임시값
        )

        try {
            chatDao.insertChatRooms(listOf(placeholderRoom))
            Log.d("ChatRepository", "✅ STOMP: Placeholder 채팅방(${message.roomId}) Room DB 저장 완료")
        } catch (e: Exception) {
            Log.e("ChatRepository", "❌ STOMP: Placeholder 채팅방 저장 실패", e)
        }
    }
    // ▲▲▲ [신규] 추가 완료 ▲▲▲


    override suspend fun disconnectStomp() {
        stompService.disconnect()
    }

    // --- (이하 코드는 Room과 관련 없으므로 수정 없음) ---

    override suspend fun detectFraud(
        userId: Long,
        request: FraudDetectionRequestDto
    ): ApiResult<ApiResponse<FraudData>> {
        Log.d("ChatRepo_Fraud", "🚀 detectFraud called with userId: $userId")
        Log.d("ChatRepo_Fraud", "🚀 detectFraud called with request: $request")
        val result = handleApi {
            chatApiService.detectFraud(userId, request)
        }
        Log.d("ChatRepo_Fraud", "✅ Response received: $result")
        return result
    }

    override suspend fun analyzeImages(chatRoomId: Long): ApiResult<ApiResponse<ImageAnalysisResponseDto>> {
        Log.d("ChatRepo_Image", "🚀 Requesting image analysis for chat: $chatRoomId")
        val result = handleApi { chatApiService.analyzeImages(chatRoomId) }
        Log.d("ChatRepo_Image", "✅ Image analysis response: $result")
        return result
    }

    override suspend fun markMessagesAsRead(chatId: Long): ApiResult<ApiResponse<Unit>> {
        Log.d("ChatRepository", "🚀 Mark messages as read for chatId: $chatId")
        val result = handleApi { chatApiService.markMessagesAsRead(chatId) }

        // 🔽 [신규] API 호출 성공 시, 로컬 Room DB도 '읽음'으로 처리
        if (result is ApiResult.Success) {
            try {
                // SSoT 일관성을 위해 markRoomAsReadInDb 호출
                markRoomAsReadInDb(chatId)
            } catch (e: Exception) {
                Log.e("ChatRepository", "markMessagesAsRead DB 업데이트 실패", e)
            }
        }
        Log.d("ChatRepository", "✅ Mark messages as read result: $result")
        return result
    }

    // --- 🔽🔽🔽 [필수] SSoT용 함수 2개의 실제 구현 🔽🔽🔽 ---

    /**
     * [신설] Room DB로부터 특정 채팅방의 메시지 목록을 Flow로 관찰합니다.
     * (ViewModel이 이 함수를 호출해야 함)
     */
    override fun getMessagesFromDb(chatId: Long): Flow<List<ChatMessage>> {
        return chatDao.getMessages(chatId) // (DAO의 @Query 함수 호출)
            .map { entityList ->
                // Entity 리스트를 Domain Model 리스트로 변환
                // 🔽 [충돌 해결] 'ChatMessageEntity.toDomainModel()' 매퍼 사용 (하단 정의)
                entityList.map { it.toDomainModel() }
            }
    }

    /**
     * [신설] Room DB로부터 전체 채팅방 목록을 Flow로 관찰합니다.
     * (ViewModel이 이 함수를 호출해야 함)
     */
    override fun getChatRoomsFromDb(): Flow<List<ChatRoom>> {
        return chatDao.getChatRooms() // (DAO의 @Query 함수 호출)
            .map { entityList ->
                // Entity 리스트를 Domain Model 리스트로 변환
                // 🔽 [충돌 해결] 'ChatRoomEntity.toDomainModel()' 매퍼 사용 (하단 정의)
                entityList.map { it.toDomainModel() }
            }
    }

    // --- ▼▼▼ [신규] SSoT용 쓰기 함수 2개 실제 구현 ▼▼▼ ---

    /**
     * [신규] ViewModel에서 Presigned URL 처리가 완료된 목록을 Room에 저장(캐시)합니다.
     */
    override suspend fun cacheChatRoomList(chatRooms: List<ChatRoomInfo>) {
        try {
            // 🔽 [충돌 해결] 'ChatRoomInfo.toEntity()' 매퍼 사용
            val entities = chatRooms.map { it.toEntity() }
            chatDao.insertChatRooms(entities) // (DAO에 OnConflictStrategy.REPLACE 필요)
            Log.d("ChatRepository", "✅ (ViewModel) Room DB에 ${entities.size}개 채팅방 덮어쓰기 완료")
        } catch (e: Exception) {
            Log.e("ChatRepository", "cacheChatRoomList DB 저장 실패", e)
        }
    }

    // ▼▼▼ [ 1. 이 함수 추가 ] ▼▼▼
    /**
     * [신규] ViewModel이 '수동으로 생성한' 새 채팅방 Entity 1개를 Room에 저장합니다.
     */
    override suspend fun cacheSingleChatRoom(roomEntity: ChatRoomEntity) {
        try {
            // ChatDao의 insertChatRooms(List<...>) 함수를 사용합니다.
            chatDao.insertChatRooms(listOf(roomEntity))
            Log.d(
                "ChatRepository",
                "✅ (ViewModel) Room DB에 새 채팅방 1개(${roomEntity.chatroomId}) 저장 완료"
            )
        } catch (e: Exception) {
            Log.e("ChatRepository", "cacheSingleChatRoom DB 저장 실패", e)
        }
    }
    // ▲▲▲ [ 수정 완료 ] ▲▲▲


    /**
     * [신규] ViewModel이 특정 채팅방을 Room에서 '읽음' 처리합니다.
     * (ChatDao에 @Query("UPDATE chatroom_table SET unreadCount = 0 WHERE chatroomId = :chatId") fun markRoomAsRead(chatId: Long) 추가 필요)
     *
     * [수정됨] 채팅방 목록(unreadCount)과 개별 메시지(isRead)를 모두 '읽음' 처리합니다.
     */
    override suspend fun markRoomAsReadInDb(chatId: Long) {
        try {
            // 1. [복원] 채팅방 목록(chat_rooms)의 unreadCount를 0으로 설정
            chatDao.markRoomAsRead(chatId) // (DAO에 @Query 함수 필요)
            Log.d("ChatRepository", "✅ Room DB unread count for $chatId set to 0")

            // 2. [기존] 해당 채팅방의 모든 메시지(chat_messages)를 '읽음' 처리
            chatDao.markMessagesAsReadInDb(chatId)
            Log.d("ChatRepository", "✅ Room DB messages for $chatId marked as read")

        } catch (e: Exception) {
            Log.e("ChatRepository", "markRoomAsReadInDb DB 업데이트 실패", e)
        }
    }

    // ▼▼▼ [로그아웃/회원탈퇴 시 추가] ▼▼▼

    override suspend fun deleteAllLocalChatRooms() {
        chatDao.deleteAllChatRooms()
    }

    override suspend fun deleteAllLocalChatMessages() {
        chatDao.deleteAllChatMessages()
    }

}


// --- 🔽🔽🔽 [충돌 해결] Mapper 함수 (실제 코드에 100% 맞춤) 🔽🔽🔽 ---
// (이 함수들은 별도 Mapper.kt 파일로 분리하는 것을 강력히 권장합니다)


// [매퍼 1] 'MessageListDto.kt'의 'MessageDto' -> 'ChatMessageEntity'
// (getMessageList API 호출 시 사용)
fun MessageDto.toEntity(): ChatMessageEntity {
    return ChatMessageEntity(
        messageId = this.messageId, // Entity의 String PK
        roomId = this.chatId,
        senderId = this.sender.userId.toLong(), // Entity의 Long
        senderName = this.sender.nickname,
        senderProfileUrl = this.sender.profileImage,
        content = this.content ?: "", // Entity의 Non-null String
        sendTime = this.sentAt, // Entity의 String (정렬용)
        messageType = "MESSAGE", // DTO에 없으므로 기본값
        isRead = this.isRead,
        imageUrl = this.imageUrl // 🔽 [충돌 해결] Entity에 추가한 imageUrl 필드
    )
}

// [매퍼 2] 'ChatMessageDto.kt'의 'ChatMessageDto' -> 'ChatMessageEntity'
// (getChatRoomDetails API 및 STOMP 수신 시 사용)
fun ChatMessageDto.toEntity(): ChatMessageEntity {
    return ChatMessageEntity(
        messageId = this.messageId, // Entity의 String PK
        roomId = this.chatId,
        senderId = this.sender.userId.toLong(), // Entity의 Long
        senderName = this.sender.nickname,
        senderProfileUrl = this.sender.profileImage,
        content = this.content,
        sendTime = this.sentAt, // Entity의 String (정렬용)
        messageType = if (this.imageUrl != null) "IMAGE" else "MESSAGE",
        isRead = false, // STOMP로 받은 건 기본적으로 '안 읽음'
        imageUrl = this.imageUrl // 🔽 [충돌 해결] DTO에 없으므로 null
    )
}

// [매퍼 3] 'SendMessageResponseDto.kt' -> 'ChatMessageEntity'
// (sendMessage API 호출 시 사용)
fun SendMessageResponseDto.toEntity(): ChatMessageEntity {
    return ChatMessageEntity(
        messageId = this.messageId, // Entity의 String PK
        roomId = this.chatId,
        senderId = this.sender.userId.toLong(), // Entity의 Long
        senderName = this.sender.nickname,
        senderProfileUrl = this.sender.profileImage,
        content = this.content,
        sendTime = this.sentAt, // Entity의 String (정렬용)
        messageType = if (this.imageUrl != null) "IMAGE" else "MESSAGE",
        isRead = true, // 내가 보낸 건 항상 읽음
        imageUrl = this.imageUrl // 🔽 [충돌 해결] DTO에 없으므로 null
    )
}

// [매퍼 4] 'ChatRoomListDto.kt'의 'ChatRoomInfo' -> 'ChatRoomEntity'
// (getChatRoomList API 호출 시 사용)
fun ChatRoomInfo.toEntity(): ChatRoomEntity {
    return ChatRoomEntity(
        chatroomId = this.chatId, // Entity의 Long PK

        // [수정] 'opponentName' 대신 양쪽 닉네임을 모두 저장
        sellerNickname = this.seller.nickname,
        buyerNickname = this.buyer.nickname,

        opponentProfileUrl = this.viewableThumbnailUrl, // "상품 썸네일"
        lastMessage = this.lastMessage?.content ?: "대화 내용이 없습니다.",
        lastMessageTime = this.lastMessage?.sentAt ?: this.createdAt, // Entity의 String (정렬용)
        unreadCount = this.unreadCount,

        productId = this.product.productId.toInt(),
        sellerId = this.seller.userId,
        buyerId = this.buyer.userId
    )
}


// --- (DB -> Domain Model 매퍼) ---

/**
 * [매퍼 5] 'ChatMessageEntity' -> 'ChatMessage' (Domain Model)
 * SSoT의 핵심. DB에서 읽은 데이터를 UI용 모델로 변환합니다.
 */
fun ChatMessageEntity.toDomainModel(): ChatMessage {
    return ChatMessage(
        messageId = this.messageId ?: 0L, // Domain의 Long
        chatRoomId = this.roomId,
        senderId = this.senderId.toInt(), // Domain의 Int
        senderNickname = this.senderName,
        senderProfileUrl = this.senderProfileUrl,
        content = this.content,
        imageUrl = this.imageUrl, // 🔽 [충돌 해결] Entity의 imageUrl 전달

        // 'isFromMe'는 ViewModel에서 계산 (여기서는 임시로 false)
        isFromMe = false,

        // 'timestamp'(상대시간)는 ViewModel에서 계산 (여기서는 원본 시간 전달)
        timestamp = this.sendTime,

        // 'originalTimestamp'(정렬용)는 그대로 전달
        originalTimestamp = this.sendTime
    )
}

/**
 * [매퍼 6] 'ChatRoomEntity' -> 'ChatRoom' (Domain Model)
 * SSoT의 핵심. DB에서 읽은 데이터를 UI용 모델로 변환합니다.
 */
fun ChatRoomEntity.toDomainModel(): ChatRoom {
    return ChatRoom(
        roomId = this.chatroomId, // Domain의 Long

        // [수정] 'opponentName' 대신 양쪽 닉네임을 모두 전달
        sellerNickname = this.sellerNickname,
        buyerNickname = this.buyerNickname,

        opponentProfileUrl = this.opponentProfileUrl,
        lastMessage = this.lastMessage,
        lastMessageTime = this.lastMessageTime,
        unreadCount = this.unreadCount,

        productId = this.productId,
        sellerId = this.sellerId,
        buyerId = this.buyerId
    )
}