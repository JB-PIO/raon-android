package com.example.raon.features.chat.ui

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raon.core.common.AppConstants
import com.example.raon.core.common.toInstant
import com.example.raon.core.common.toKSTLocalDateTime
import com.example.raon.core.common.toRelativeTimeString
import com.example.raon.core.network.ApiResult
import com.example.raon.core.network.repository.ImageStorageRepository
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

// 데이터 클래스는 변경 없음
data class ImageAnalysisResult(
    val imageUrl: String,
    val result: String,
    val similarImages: List<String> = emptyList()
)

// 화면 상단 바 상품 정보 data class
data class ChatProductInfo(
    // ▼▼▼ [수정된 부분] itemId 추가 ▼▼▼
    val itemId: Int,
    val productName: String,
    val price: Int?,
    val status: String,
    val viewableThumbnailUrl: String?
)

// 채팅 화면 전체 UI 상태
data class ChatUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val fraudWarningMessage: String? = null,
    val productInfo: ChatProductInfo? = null,
    val opponentNickname: String? = null,
    val isCurrentUserBuyer: Boolean = false,

    val isAnalyzingImage: Boolean = false,
    val imageAnalysisResult: List<ImageAnalysisResult>? = null,
    val isDetectingFraud: Boolean = false
)

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val imageStorageRepository: ImageStorageRepository,
    private val stompService: StompService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val chatRoomId: Long = savedStateHandle.get<String>("chatRoomId")?.toLongOrNull() ?: -1L

    private val gson = Gson()
    private val _myUserId = MutableStateFlow<Int?>(null)
    val myUserId = _myUserId.asStateFlow()
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        Log.d("ChatViewModel", "0. Initializing with chatId: $chatRoomId")
        viewModelScope.launch {
            _myUserId.value = userRepository.getUserProfile().first()?.userId
            Log.d("ChatViewModel", "My User ID loaded: ${_myUserId.value}")

            if (chatRoomId != -1L && _myUserId.value != null) {
                loadInitialData()
                connectAndObserveStomp()
            } else {
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

                val messagesDeferred = async { loadInitialMessages() }
                val loadedProductInfo = productInfoDeferred.await()
                messagesDeferred.await()

                _uiState.update {
                    it.copy(
                        isLoading = false,
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

    private fun connectAndObserveStomp() {
        viewModelScope.launch {
            try {
                chatRepository.connectStomp(chatRoomId = chatRoomId)
                chatRepository.observeMessages(chatRoomId)
                    .catch { e ->
                        Log.e("ChatViewModel", "❌ STOMP message observation error", e)
                        _uiState.update { it.copy(errorMessage = "실시간 메시지 수신 오류") }
                    }
                    .collect { jsonString ->
                        try {
                            val chatMessageDto =
                                gson.fromJson(jsonString, ChatMessageDto::class.java)
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
                Log.e("ChatViewModel", "❌ STOMP connection failed", e)
                _uiState.update { it.copy(errorMessage = "실시간 채팅 서버 연결 실패") }
            }
        }
    }

    private suspend fun loadInitialMessages() {
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

                    currentState.copy(messages = updatedMessages)
                }
            }

            is ApiResult.Error -> {
                Log.e("ChatViewModel", "❌ Error loading initial messages: ${result.code}")
                _uiState.update { it.copy(errorMessage = "메시지 로딩 실패") }
            }

            is ApiResult.Exception -> {
                Log.e("ChatViewModel", "❌ Exception loading initial messages", result.e)
                _uiState.update { it.copy(errorMessage = "메시지 로딩 중 오류 발생") }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || chatRoomId == -1L) return
        val currentMyId = _myUserId.value ?: return

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

        viewModelScope.launch {
            chatRepository.sendMessage(chatRoomId, text)
        }
    }

    fun detectFraud() {
        viewModelScope.launch {
            val currentMyId = myUserId.value ?: return@launch
            val currentMessages = _uiState.value.messages
            if (currentMessages.isEmpty()) {
                _uiState.update { it.copy(fraudWarningMessage = "분석할 대화 내용이 없습니다.") }
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
                        _uiState.update { it.copy(fraudWarningMessage = result.data.data?.message) }
                    }

                    is ApiResult.Error -> {
                        _uiState.update { it.copy(fraudWarningMessage = "분석 오류 (API: ${result.code})") }
                    }

                    is ApiResult.Exception -> {
                        _uiState.update { it.copy(fraudWarningMessage = "분석 오류 (네트워크)") }
                    }
                }
            } finally {
                _uiState.update { it.copy(isDetectingFraud = false) }
            }
        }
    }

//    fun startImageAnalysis() {
//        if (_uiState.value.isAnalyzingImage || _uiState.value.imageAnalysisResult != null) return
//
//        viewModelScope.launch {
//            _uiState.update { it.copy(isAnalyzingImage = true) }
//            delay(2500)
//
//            val dummyResults = listOf(
//                ImageAnalysisResult(
//                    imageUrl = _uiState.value.productInfo?.viewableThumbnailUrl ?: "",
//                    result = "WARNING",
//                    similarImages = listOf(
//                        "https://via.placeholder.com/150/FF0000/FFFFFF?Text=Similar+1",
//                        "https://via.placeholder.com/150/0000FF/FFFFFF?Text=Similar+2",
//                        "https://via.placeholder.com/150/00FF00/FFFFFF?Text=Similar+3"
//                    )
//                )
//            )
//            _uiState.update {
//                it.copy(
//                    isAnalyzingImage = false,
//                    imageAnalysisResult = dummyResults
//                )
//            }
//        }
//    }

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

//                            result.data.toDomainModel()

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
        _uiState.update { it.copy(fraudWarningMessage = null) }
    }

    private fun markMessagesAsRead() {
        if (chatRoomId == -1L) return
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(chatRoomId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            Log.d("ChatViewModel", "onCleared: Disconnecting STOMP...")
            chatRepository.disconnectStomp()
        }
    }
}

