package com.example.raon.features.chat.ui

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raon.core.common.AppConstants
import com.example.raon.core.common.CurrentScreenManager
import com.example.raon.core.common.toInstant
import com.example.raon.core.common.toKSTLocalDateTime
import com.example.raon.core.common.toRelativeTimeString
import com.example.raon.core.network.ApiResult
import com.example.raon.core.network.repository.ImageStorageRepository
import com.example.raon.features.chat.data.local.ChatRoomEntity
import com.example.raon.features.chat.data.remote.StompService
import com.example.raon.features.chat.data.remote.dto.ChatMessageDto
import com.example.raon.features.chat.data.remote.dto.UserInChatDetailDto
import com.example.raon.features.chat.data.remote.dto.ai.FraudDetectionRequestDto
import com.example.raon.features.chat.data.remote.dto.ai.MessageInFraudRequestDto
import com.example.raon.features.chat.data.remote.dto.ai.toDomainModel
import com.example.raon.features.chat.data.remote.dto.toDomainModel
import com.example.raon.features.chat.domain.model.ChatMessage
import com.example.raon.features.chat.domain.repository.ChatRepository
import com.example.raon.features.user.domain.repository.UserRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

// ==============================================================================================
// [Domain Model] AI 이미지 분석 결과를 UI에 전달하기 위한 데이터 클래스
// ==============================================================================================
data class ImageAnalysisResult(
    val imageUrl: String,
    val result: String,
    val similarImages: List<String> = emptyList()
)

// ==============================================================================================
// [Domain Model] AI 사기 탐지 결과를 UI에 전달하기 위한 데이터 클래스
// ==============================================================================================
data class FraudDetectionResult(
    val level: String, // "SAFE", "WARNING", "DANGER"
    val message: String // 서버에서 받은 상세 경고 메시지
)

// ==============================================================================================
// [UI State]
// ==============================================================================================

// 화면 상단 바 상품 정보 data class
data class ChatProductInfo(
    // ▼▼▼ [수정된 부분] itemId 추가 ▼▼▼
    val itemId: Int,
    val productName: String,
    val price: Int?,
    val status: String,
    val viewableThumbnailUrl: String?
)

// ▼▼▼ [ 1. ChatUiState 수정 (페이징 상태 추가) ] ▼▼▼
data class ChatUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    // val fraudWarningMessage: String? = null, // <- 기존 필드 제거
    val fraudDetectionResult: FraudDetectionResult? = null, // <- 새 필드 추가
    val productInfo: ChatProductInfo? = null,
    val opponentNickname: String? = null,
    val isCurrentUserBuyer: Boolean = false,

    val isAnalyzingImage: Boolean = false,
    val imageAnalysisResult: List<ImageAnalysisResult>? = null,
    val isDetectingFraud: Boolean = false,

    // --- Paging 상태 추가 ---
    val isPageLoading: Boolean = false, // 👈 페이지 로드 중 스피너
    val isLastPage: Boolean = false     // 👈 마지막 페이지 여부
)
// ▲▲▲ [ 1. ChatUiState 수정 완료 ] ▲▲▲

// ==============================================================================================
//  [ViewModel]
// ==============================================================================================

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val imageStorageRepository: ImageStorageRepository,
    private val stompService: StompService,
    private val currentScreenManager: CurrentScreenManager,

    // ▼▼▼ [수정 2] 'savedStateHandle'을 생성자 파라미터에서 클래스 프로퍼티(private val)로 변경 ▼▼▼
    private val savedStateHandle: SavedStateHandle
    // ▲▲▲ [수정 완료] ▲▲▲

) : ViewModel() {

    val chatRoomId: Long = savedStateHandle.get<String>("chatRoomId")?.toLongOrNull() ?: -1L

    private val gson = Gson()
    private val _myUserId = MutableStateFlow<Int?>(null)
    val myUserId = _myUserId.asStateFlow()
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    // ▼▼▼ [ 2. (수정 1단계) 이 두 줄을 추가 ] ▼▼▼
    // ChatRoomEntity 생성을 위해 판매자와 구매자 정보를 저장할 변수
    private var _sellerInfo: UserInChatDetailDto? = null
    private var _buyerInfo: UserInChatDetailDto? = null
    // ▲▲▲ [ 2. 추가 완료 ] ▲▲▲

    // ▼▼▼ [ 3. ViewModel에 Paging 변수 추가 ] ▼▼▼
    private var currentPage = 0
    private val CHAT_PAGE_SIZE = 20 // 👈 페이지당 메시지 수 (서버와 동일하게)
    // ▲▲▲ [ 3. Paging 변수 추가 완료 ] ▲▲▲

    init {
        Log.d("ChatViewModel", "0. Initializing with chatId: $chatRoomId")

        // ViewModel 생성 시 (채팅방 진입) 현재 방 ID 설정
        currentScreenManager.setCurrentChatRoom(chatRoomId)


        Log.d("ChatViewModel", "0. Initializing with chatId: $chatRoomId")
        viewModelScope.launch {
            _myUserId.value = userRepository.getUserProfile().first()?.userId
            Log.d("ChatViewModel", "My User ID loaded: ${_myUserId.value}")

            if (chatRoomId != -1L && _myUserId.value != null) {
                loadInitialData()
                // [수정됨] 함수 이름 변경 및 연결 로직 제거
                observeStompMessages()
            } else {
                // [참고] 만약 -1L일 때(새 채팅) 상품 정보를 로드해야 한다면,
                // 여기서 'itemId'를 savedStateHandle에서 꺼내 별도 함수(예: loadProductInfo(itemId))를 호출해야 합니다.
                // 현재 로직은 -1L이면 아무 정보도 로드하지 않습니다.
                val errorMsg =
                    if (chatRoomId == -1L) "Invalid chat room ID." else "Could not load user info."
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                Log.e("ChatViewModel", "Initialization failed: $errorMsg")
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            Log.d("ChatViewModel", "1. Starting initial data load...")

            try {
                val chatDetailsData =
                    when (val detailsResult = chatRepository.getChatRoomDetails(chatRoomId)) {
                        is ApiResult.Success -> detailsResult.data.data
                            ?: throw Exception("Chat details data is null")

                        is ApiResult.Error -> throw Exception("Chat details load failed: ${detailsResult.code}")
                        is ApiResult.Exception -> throw detailsResult.e
                    }

                // ▼▼▼ [ 4. (수정 2단계) 이 두 줄을 추가 ] ▼▼▼
                // 1단계에서 추가한 변수에 API 응답 데이터 저장
                _sellerInfo = chatDetailsData.seller
                _buyerInfo = chatDetailsData.buyer
                // ▲▲▲ [ 4. 추가 완료 ] ▲▲▲

                val opponentUser: UserInChatDetailDto
                val isBuyer: Boolean
                if (_myUserId.value == chatDetailsData.seller.userId) {
                    opponentUser = chatDetailsData.buyer
                    isBuyer = false
                } else {
                    opponentUser = chatDetailsData.seller
                    isBuyer = true
                }
                val actualOpponentNickname = opponentUser.nickname

                val productInfoDeferred = async {
                    var thumbnailUrl: String? = null
                    val thumbnailKey = chatDetailsData.product.thumbnail
                        ?.removePrefix(AppConstants.S3_BASE_URL)

                    if (thumbnailKey != null) {
                        thumbnailUrl =
                            imageStorageRepository.getPresignedImageUrl(thumbnailKey).getOrNull()
                    }

                    val productStatusText = when (chatDetailsData.product.status) {
                        "AVAILABLE" -> "판매중"
                        "RESERVED" -> "예약중"
                        "SOLD" -> "판매완료"
                        else -> chatDetailsData.product.status ?: "상태 없음"
                    }

                    // ▼▼▼ [수정된 부분] itemId 값을 채워서 ChatProductInfo 생성 ▼▼▼
                    ChatProductInfo(
                        itemId = chatDetailsData.product.productId.toInt(), // 이 부분은 실제 DTO의 상품 ID 필드명으로 맞춰주세요.
                        productName = chatDetailsData.product.title ?: "상품 이름 없음",
                        price = chatDetailsData.product.price,
                        status = productStatusText,
                        viewableThumbnailUrl = thumbnailUrl
                    )
                }

                val messagesDeferred = async { loadInitialMessages() } // 👈 [ 5. 수정 ]
                val loadedProductInfo = productInfoDeferred.await()
                messagesDeferred.await()

                _uiState.update {
                    it.copy(
                        // isLoading = false, 👈 [ 5. 수정 ] loadInitialMessages로 이동
                        productInfo = loadedProductInfo,
                        opponentNickname = actualOpponentNickname,
                        isCurrentUserBuyer = isBuyer
                    )
                }
                markMessagesAsRead()

            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Error during initial data load", e)
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "채팅방 정보를 불러오는 중 오류 발생")
                }
            }
        }
    }

    // [수정됨] 함수 이름 변경 및 connectStomp 제거
    private fun observeStompMessages() {
        viewModelScope.launch {
            try {
                // [수정됨] ⛔ 연결(connect) 로직 제거 ⛔
                // chatRepository.connectStomp(chatRoomId = chatRoomId)

                // 구독(observe)만 수행
                chatRepository.observeMessages(chatRoomId)
                    .catch { e ->
                        Log.e("ChatViewModel", "❌ STOMP message observation error", e)
                        _uiState.update { it.copy(errorMessage = "실시간 메시지 수신 오류") }
                    }
                    .collect { jsonString ->
                        try {
                            val chatMessageDto =
                                gson.fromJson(jsonString, ChatMessageDto::class.java)

                            // [수정됨] 이 채팅방의 메시지인지 필터링
                            if (chatMessageDto.chatId != chatRoomId) return@collect

                            val chatMessage = chatMessageDto.toDomainModel(myUserId.value)

                            if (!chatMessage.isFromMe) {
                                viewModelScope.launch { markMessagesAsRead() }
                            }

                            _uiState.update { currentState ->
                                if (currentState.messages.none { it.messageId == chatMessage.messageId }) {
                                    val updatedMessages =
                                        (currentState.messages + listOf(chatMessage))
                                            .sortedWith(compareBy<ChatMessage> { msg ->
                                                msg.originalTimestamp.toInstant()
                                            }.thenBy { it.messageId })
                                    currentState.copy(messages = updatedMessages)
                                } else {
                                    currentState
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ChatViewModel", "❌ STOMP message parsing failed: $jsonString", e)
                        }
                    }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ STOMP observation failed", e)
                _uiState.update { it.copy(errorMessage = "실시간 채팅 수신 실패") }
            }
        }
    }

    // ▼▼▼ [ 6. loadInitialMessages 수정 (페이징 로직 추가) ] ▼▼▼
    /**
     * 첫 페이지(page = 0) 메시지를 로드합니다.
     */
    private suspend fun loadInitialMessages() {
        // 페이지 상태 초기화
        currentPage = 0
        _uiState.update { it.copy(isLastPage = false) }

        when (val result = chatRepository.getMessageList(chatRoomId, page = 0)) {
            is ApiResult.Success -> {
                val messageDtos = result.data.data?.messages ?: emptyList()
                _uiState.update { currentState ->
                    val newMessages = messageDtos.map { dto ->
                        dto.toDomainModel(myUserId.value)
                    }
                    val updatedMessages = (currentState.messages + newMessages)
                        .distinctBy { it.messageId }
                        .sortedWith(compareBy<ChatMessage> { msg ->
                            msg.originalTimestamp.toInstant()
                        }.thenBy { it.messageId })

                    currentState.copy(
                        messages = updatedMessages,
                        isLoading = false, // 👈 로딩 상태 여기서 종료
                        isLastPage = messageDtos.size < CHAT_PAGE_SIZE // 👈 마지막 페이지인지 확인
                    )
                }
            }

            is ApiResult.Error -> {
                Log.e("ChatViewModel", "❌ Error loading initial messages: ${result.code}")
                _uiState.update { it.copy(isLoading = false, errorMessage = "메시지 로딩 실패") }
            }

            is ApiResult.Exception -> {
                Log.e("ChatViewModel", "❌ Exception loading initial messages", result.e)
                _uiState.update { it.copy(isLoading = false, errorMessage = "메시지 로딩 중 오류 발생") }
            }
        }
    }
    // ▲▲▲ [ 6. 수정 완료 ] ▲▲▲

    // ▼▼▼ [ 7. loadMoreMessages 함수 신규 추가 ] ▼▼▼
    /**
     * 다음 페이지 메시지를 로드합니다 (스크롤 시).
     */
    fun loadMoreMessages() {
        // 이미 로딩 중이거나 마지막 페이지면 중단
        if (_uiState.value.isPageLoading || _uiState.value.isLastPage) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPageLoading = true) }
            currentPage++ // 다음 페이지
            Log.d("ChatPaging", "Loading page: $currentPage")

            when (val result = chatRepository.getMessageList(chatRoomId, page = currentPage)) {
                is ApiResult.Success -> {
                    val messageDtos = result.data.data?.messages ?: emptyList()
                    _uiState.update { currentState ->
                        val newMessages = messageDtos.map { dto ->
                            dto.toDomainModel(myUserId.value)
                        }

                        // [중요] 새 메시지(과거)를 기존 메시지 *앞에* 추가
                        val updatedMessages = (newMessages + currentState.messages)
                            .distinctBy { it.messageId }
                            .sortedWith(compareBy<ChatMessage> { msg ->
                                msg.originalTimestamp.toInstant()
                            }.thenBy { it.messageId })

                        currentState.copy(
                            messages = updatedMessages,
                            isPageLoading = false,
                            isLastPage = messageDtos.size < CHAT_PAGE_SIZE // 👈 마지막 페이지 확인
                        )
                    }
                }

                is ApiResult.Error -> {
                    Log.e("ChatPaging", "❌ Error loading page $currentPage: ${result.code}")
                    _uiState.update { it.copy(isPageLoading = false) }
                    currentPage-- // 실패 시 페이지 원복
                }

                is ApiResult.Exception -> {
                    Log.e("ChatPaging", "❌ Exception loading page $currentPage", result.e)
                    _uiState.update { it.copy(isPageLoading = false) }
                    currentPage-- // 실패 시 페이지 원복
                }
            }
        }
    }
    // ▲▲▲ [ 7. 신규 추가 완료 ] ▲▲▲

    fun sendMessage(text: String) {
        // [참고] 현재 로직은 chatRoomId가 -1L(새 채팅)이면
        // 메시지 전송을 막고 있습니다. (init 로직과 이 return 구문)
        // 만약 -1L일 때 "채팅방 생성" API를 호출해야 한다면
        // 이 부분을 수정해야 합니다. (예: sendFirstMessage(itemId, text))
        if (text.isBlank() || chatRoomId == -1L) return
        val currentMyId = _myUserId.value ?: return

        // ▼▼▼ [수정 4] "첫 메시지"인지 판단하기 위해 현재 메시지 목록 상태 저장 ▼▼▼
        // (메시지가 비어있었다면, 이게 첫 메시지임)
        val isFirstMessage = _uiState.value.messages.isEmpty()
        // ▲▲▲ [수정 완료] ▲▲▲

        val now = Instant.now()
        val originalTimestamp = now.toString()
        val displayTimestamp = now.toKSTLocalDateTime().toRelativeTimeString()

        val optimisticMessage = ChatMessage(
            messageId = System.currentTimeMillis(),
            chatRoomId = chatRoomId,
            senderId = currentMyId,
            senderNickname = "나",
            senderProfileUrl = null,
            content = text,
            imageUrl = null,
            timestamp = displayTimestamp,
            isFromMe = true,
            originalTimestamp = originalTimestamp
        )

        _uiState.update { currentState ->
            val updatedMessages = (currentState.messages + optimisticMessage)
                .sortedWith(compareBy<ChatMessage> { msg ->
                    msg.originalTimestamp.toInstant()
                }.thenBy { it.messageId })
            currentState.copy(messages = updatedMessages)
        }

        // ▼▼▼ [수정 5] _didSendMessage.value = true 대신, isFirstMessage일 때만 플래그 설정 ▼▼▼
        if (isFirstMessage) {
            // MainViewModel이 감지할 수 있도록 SavedStateHandle에 값을 씁니다.
            savedStateHandle.set("new_chat_created", true)
            Log.d("ChatRoomViewModel", "첫 메시지 전송 감지. 'new_chat_created' 플래그 설정!")
        }
        // ▲▲▲ [수정 완료] ▲▲▲

        // ▼▼▼ [ 3. (수정 3단계) 이 블록을 통째로 수정 ] ▼▼▼
        viewModelScope.launch {
            // 1. API 호출 결과를 변수로 받습니다.
            val result = chatRepository.sendMessage(chatRoomId, text)

            // 2. API 호출이 성공했는지 확인합니다.
            if (result is ApiResult.Success) {

                // 3. [핵심] 첫 메시지였는지 확인합니다.
                if (isFirstMessage) {
                    val sentMessageDto = result.data.data // 방금 보낸 메시지 정보
                    val productInfo = _uiState.value.productInfo // 상단 바 상품 정보
                    val seller = _sellerInfo // 2단계에서 저장한 판매자 정보
                    val buyer = _buyerInfo   // 2단계에서 저장한 구매자 정보

                    // 4. Entity 생성에 필요한 모든 정보가 있는지 확인
                    if (sentMessageDto != null && productInfo != null && seller != null && buyer != null) {

                        // 5. ChatRoomEntity 객체 생성 (사용자가 제공한 Entity 양식에 맞게)
                        val newRoomEntity = ChatRoomEntity(
                            chatroomId = this@ChatRoomViewModel.chatRoomId, // 현재 채팅방 ID
                            opponentProfileUrl = productInfo.viewableThumbnailUrl, // 상품 썸네일
                            lastMessage = sentMessageDto.content, // 방금 보낸 메시지
                            lastMessageTime = sentMessageDto.sentAt, // 방금 보낸 시간
                            unreadCount = 0, // 내가 보냈으므로 안읽은 개수 0
                            productId = productInfo.itemId, // 상품 ID
                            sellerId = seller.userId,
                            buyerId = buyer.userId,
                            sellerNickname = seller.nickname,
                            buyerNickname = buyer.nickname
                        )

                        // 6. Repository를 통해 Room DB에 저장(캐시)
                        // (ChatRepositoryImpl에 이미 구현된 cacheSingleChatRoom 함수 호출)
                        chatRepository.cacheSingleChatRoom(newRoomEntity)
                        Log.d("ChatRoomViewModel", "✅ 첫 메시지 전송 성공, ChatRoomEntity 캐시 완료!")

                    } else {
                        Log.w(
                            "ChatRoomViewModel",
                            "⚠️ ChatRoomEntity 캐시 실패: 필요한 정보 부족 (MessageDto, ProductInfo, Seller, Buyer)"
                        )
                    }
                }

            } else {
                // (선택사항) 메시지 전송 실패 처리 (예: 낙관적 UI 롤백)
                Log.e("ChatRoomViewModel", "❌ 메시지 전송 API 실패: $result")
                _uiState.update {
                    it.copy(messages = it.messages.filterNot { msg -> msg.messageId == optimisticMessage.messageId })
                }
            }
        }
        // ▲▲▲ [ 3. 수정 완료 ] ▲▲▲
    }

    /**
     * 사기 탐지 API 호출 및 결과 처리 (수정됨)
     */
    fun detectFraud() {
        viewModelScope.launch {
            val currentMyId = myUserId.value ?: return@launch
            val currentMessages = _uiState.value.messages
            if (currentMessages.isEmpty()) {
                _uiState.update {
                    it.copy(
                        fraudDetectionResult = FraudDetectionResult(
                            level = "WARNING",
                            message = "분석할 대화 내용이 없습니다."
                        )
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isDetectingFraud = true) }

            try {
                val messageDtos = currentMessages.mapNotNull { chatMessage ->
                    MessageInFraudRequestDto(
                        messageId = chatMessage.messageId,
                        senderId = chatMessage.senderId,
                        content = chatMessage.content,
                        sentAt = chatMessage.originalTimestamp
                    )
                }
                val requestDto =
                    FraudDetectionRequestDto(requesterId = currentMyId, messages = messageDtos)

                when (val result = chatRepository.detectFraud(chatRoomId, requestDto)) {
                    is ApiResult.Success -> {
                        val fraudData = result.data.data
                        if (fraudData != null) {
                            val domainResult = FraudDetectionResult(
                                level = fraudData.result, // SAFE, WARNING, DANGER
                                message = fraudData.message // 경고 텍스트
                            )
                            _uiState.update { it.copy(fraudDetectionResult = domainResult) }
                        } else {
                            // 데이터는 없지만 성공한 경우 처리
                            _uiState.update {
                                it.copy(
                                    fraudDetectionResult = FraudDetectionResult(
                                        level = "SAFE",
                                        message = "사기 징후가 감지되지 않았습니다."
                                    )
                                )
                            }
                        }
                    }

                    is ApiResult.Error -> {
                        val msg = "분석 오류 (API: ${result.code})"
                        _uiState.update {
                            it.copy(
                                fraudDetectionResult = FraudDetectionResult(
                                    level = "DANGER",
                                    message = msg
                                )
                            )
                        }
                    }

                    is ApiResult.Exception -> {
                        val msg = "분석 오류 (네트워크)"
                        _uiState.update {
                            it.copy(
                                fraudDetectionResult = FraudDetectionResult(
                                    level = "DANGER",
                                    message = msg
                                )
                            )
                        }
                    }
                }
            } finally {
                _uiState.update { it.copy(isDetectingFraud = false) }
            }
        }
    }

    /**
     * AI 이미지 분석 요청 및 결과 처리
     */
    fun startImageAnalysis() {
        // ⚠️ ImageAnalysisResult가 null이 아닌 경우, 이미 분석이 완료되었거나 진행 중인 경우를 막습니다.
        if (_uiState.value.isAnalyzingImage || _uiState.value.imageAnalysisResult != null) return

        val currentChatRoomId = chatRoomId
        if (currentChatRoomId == -1L) return // 유효하지 않은 채팅방 ID

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingImage = true, errorMessage = null) }

            try {
                // ▼▼▼ [수정된 부분] Repository의 analyzeImages 함수 호출 ▼▼▼
                when (val result = chatRepository.analyzeImages(currentChatRoomId)) {
                    is ApiResult.Success -> {
                        // DTO를 UI 상태에서 사용할 Domain Model로 변환
                        val domainResults = result.data.data?.toDomainModel()

                        _uiState.update {
                            it.copy(
                                isAnalyzingImage = false,
                                imageAnalysisResult = domainResults
                            )
                        }
                        Log.d("ChatViewModel", "✅ Image analysis successful: $domainResults")
                    }

                    is ApiResult.Error -> {
                        val errorMsg = "이미지 분석 오류 (API: ${result.code})"
                        Log.e("ChatViewModel", "❌ Image analysis failed: ${result.code}")
                        _uiState.update {
                            it.copy(isAnalyzingImage = false, errorMessage = errorMsg)
                        }
                    }

                    is ApiResult.Exception -> {
                        val errorMsg = "이미지 분석 중 네트워크 오류"
                        Log.e("ChatViewModel", "❌ Image analysis exception", result.e)
                        _uiState.update {
                            it.copy(isAnalyzingImage = false, errorMessage = errorMsg)
                        }
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "이미지 분석 중 알 수 없는 오류 발생"
                Log.e("ChatViewModel", "❌ Unexpected error during image analysis", e)
                _uiState.update {
                    it.copy(isAnalyzingImage = false, errorMessage = errorMsg)
                }
            }
        }
    }

    fun dismissImageAnalysis() {
        _uiState.update { it.copy(imageAnalysisResult = null) }
    }

    fun closeWarningBanner() {
        // 경고 배너를 닫을 때 fraudDetectionResult를 null로 설정
        _uiState.update { it.copy(fraudDetectionResult = null) }
    }

    private fun markMessagesAsRead() {
        if (chatRoomId == -1L) return
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(chatRoomId)
        }
    }

    // [수정됨] ⛔ STOMP 연결 해제(disconnect) 로직 제거 ⛔
    override fun onCleared() {
        currentScreenManager.clearCurrentChatRoom() // 채팅방 나가면 currentScreen 화면 초기
        super.onCleared()
        // viewModelScope.launch {
        //     Log.d("ChatViewModel", "onCleared: Disconnecting STOMP...")
        //     chatRepository.disconnectStomp() // <- 제거
        // }
    }
}