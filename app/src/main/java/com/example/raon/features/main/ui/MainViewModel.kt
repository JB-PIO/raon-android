package com.example.raon.features.main.ui

//import com.example.raon.core.service.ChatService // [추가] ChatService import
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raon.core.common.AppConstants
import com.example.raon.core.network.ApiResult
import com.example.raon.core.network.repository.ImageStorageRepository
import com.example.raon.features.chat.data.remote.StompService
import com.example.raon.features.chat.data.remote.dto.ChatRoomInfo
import com.example.raon.features.chat.data.remote.service.ChatService
import com.example.raon.features.chat.domain.model.ChatRoom
import com.example.raon.features.chat.domain.repository.ChatRepository
import com.example.raon.features.item.ui.list.LocationUiModel
import com.example.raon.features.user.domain.model.User
import com.example.raon.features.user.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class MainUiState(
    val chatRooms: List<ChatRoom> = emptyList(),
    val unreadChatCount: Int = 0,
    val isLoading: Boolean = true,
    val viewableProfileImageUrl: String? = null,
    val locationName: String = "위치 정보 없음",
    val availableLocations: List<LocationUiModel> = emptyList()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle,
    private val imageStorageRepository: ImageStorageRepository,
    private val stompService: StompService // 👈 [수정 2] StompService 주입
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MainUiState(
            isLoading = true,
            chatRooms = emptyList()
        )
    )

    // [유지] Room DB의 채팅방 목록을 구독
    private val chatRoomsFlow: StateFlow<List<ChatRoom>> = chatRepository.getChatRoomsFromDb()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // [유지] UI가 최종적으로 구독할 uiState
    val uiState: StateFlow<MainUiState> = combine(
        _uiState,
        chatRoomsFlow
    ) { state, rooms ->
        state.copy(
            chatRooms = rooms,
            unreadChatCount = rooms.sumOf { it.unreadCount }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    // [유지] 사용자 프로필 구독
    val userProfile: StateFlow<User?> = userRepository.getUserProfile()
        .onEach { user ->
            Log.d("YourViewModel", "DataStore에서 ?1")
            if (user != null) {
                Log.d("YourViewModel", "DataStore에서 사용자 데이터 로드 성공: $user")
            } else {
                Log.d("YourViewModel", "DataStore에 사용자 데이터가 없거나 초기값입니다.")
            }
            Log.d("YourViewModel", "DataStore에서 ?2")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )


    // [유지] 즐겨찾기 위치 목록 구독
    val favoriteLocations: StateFlow<List<LocationUiModel>> =
        userRepository.getFavoriteLocations()
            .map { pairs ->
                pairs.map { LocationUiModel(id = it.first, name = it.second) }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    init {
        // [유지] ChatService가 실행 중이 아니라면 시작시킴
        if (!ChatService.isRunning()) {
            Log.d("MainViewModel", "ChatService가 실행 중이 아니므로 시작합니다.")
            startChatService()
        } else {
            Log.d("MainViewModel", "ChatService는 이미 실행 중입니다.")
        }

        // [유지] 서버 데이터를 가져와 'Room DB'에 저장
        loadInitialData()

        // [유지] SavedStateHandle (읽음 처리)
        viewModelScope.launch {
            savedStateHandle.getStateFlow<Long?>("read_chat_room_id", null)
                .collect { readChatId ->

                    Log.d("ChatReadDebug", "4. MainViewModel collected chatId: $readChatId")

                    if (readChatId != null && readChatId != -1L) {
                        Log.d("MainViewModel", "✅ Chat room read result received: $readChatId")
                        markChatRoomAsRead(readChatId)
                        savedStateHandle.remove<Long>("read_chat_room_id")

                        Log.d(
                            "ChatReadDebug",
                            "5. Processed and removed chatId from SavedStateHandle."
                        )

                        // 👈 [수정 4] 읽음 처리 직후에도 목록 갱신 (unreadCount 반영)
                        refreshChatList()
                    }
                }
        }

        // ▼▼▼ [수정된 부분] 새 채팅방 생성 후 복귀 시 강제 갱신 로직 추가 ▼▼▼
        viewModelScope.launch {
            savedStateHandle.getStateFlow<Boolean?>("new_chat_created", null)
                .collect { isNew ->
                    if (isNew == true) {
                        Log.d("MainViewModel", "✅ 'new_chat_created' 플래그 수신. 채팅 목록을 강제로 갱신합니다.")
                        refreshChatList()
                        savedStateHandle.remove<Boolean>("new_chat_created")
                    }
                }
        }
        // ▲▲▲ [수정 완료] ▲▲▲

        // 👈 [수정 4] STOMP 메시지 구독 (실시간 갱신)
        viewModelScope.launch {
            Log.d("MainViewModel_STOMP", "STOMP 'messages' Flow 구독 시작")
            stompService.messages
                .debounce(1500L) // 1.5초간 메시지 폭주 방지
                .collect { messageJson ->
                    // 어떤 메시지든 받으면 (새 채팅방이든, 기존 채팅방이든)
                    // 채팅방 목록 전체를 서버로부터 다시 받아옵니다.
                    Log.d("MainViewModel_STOMP", "🔥 STOMP 메시지 수신 (Debounced)! 채팅 목록을 갱신합니다.")
                    Log.d("MainViewModel_STOMP", " > 수신 메시지(참고용): $messageJson")

                    refreshChatList() // 2단계에서 만든 함수 호출
                }
        }
    }

    // [유지] 서비스 시작 함수
    private fun startChatService() {
        val serviceIntent = Intent(context, ChatService::class.java).apply {
            action = ChatService.ACTION_START
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    // [유지] (로그아웃 함수가 여기 있다면) 서비스 중지 함수
    /**
     * 사용자가 로그아웃을 요청할 때 호출됩니다.
     */
    fun handleLogout() {
        viewModelScope.launch {
            // TODO: DataStore의 유저 정보/토큰 삭제 로직 (예: userRepository.clearAllData())

            // ChatService를 중지시킴
            stopChatService()
        }
    }

    // [유지] 서비스 중지 함수
    private fun stopChatService() {
        val serviceIntent = Intent(context, ChatService::class.java).apply {
            action = ChatService.ACTION_STOP
        }
        context.stopService(serviceIntent)
    }


    /**
     * 👈 [수정 3] 신규 함수: 채팅 목록만 서버에서 새로고침하고 Room DB를 덮어씁니다.
     * (백그라운드 갱신용 - isLoading 상태를 건드리지 않음)
     */
    private fun refreshChatList() {
        viewModelScope.launch {
            val chatResult = chatRepository.getChatRoomList(page = 0)

            if (chatResult is ApiResult.Success) {
                var thumbnailUrl: String? = null

                val chatList = chatResult.data.data?.chats ?: emptyList()
                Log.d("MainViewModel_Refresh", "✅ Chat list DTO loaded: ${chatList.size} rooms")

                val chatListWithUrls: List<ChatRoomInfo> =
                    chatList.map { chatRoomInfo ->
                        async {
                            val thumbnailKey =
                                chatRoomInfo.product.thumbnail?.removePrefix(AppConstants.S3_BASE_URL)

                            Log.d("MainViewModel_Refresh", "✅ thumbnailKey : ${thumbnailKey}")

                            if (thumbnailKey != null) {
                                try {
                                    thumbnailUrl =
                                        imageStorageRepository.getPresignedImageUrl(thumbnailKey)
                                            .getOrNull()
                                    Log.d(
                                        "MainViewModel_Refresh",
                                        "✅ thumbnailUrl : ${thumbnailUrl}"
                                    )

                                } catch (e: Exception) {
                                    Log.d("MainViewModel_Refresh", "✅ 실패 : 실패")
                                    Log.e(
                                        "MainViewModel_Refresh",
                                        "❌ Failed to load Thumbnail Presigned URL (chatId: ${chatRoomInfo.chatId})",
                                        e
                                    )
                                }
                            }
                            chatRoomInfo.copy(viewableThumbnailUrl = thumbnailUrl)
                        }
                    }.awaitAll()

                try {
                    chatRepository.cacheChatRoomList(chatListWithUrls)
                    Log.d("MainViewModel_Refresh", "✅ Fetched list (with URLs) saved to Room.")
                } catch (e: Exception) {
                    Log.e("MainViewModel_Refresh", "❌ Failed to save chat list to Room", e)
                }

            } else {
                Log.w(
                    "MainViewModel_Refresh",
                    "❌ Chat list fetch failed. Will use cached data if available."
                )
            }
            // (isLoading = false) 로직은 여기서 제거!
        }
    }


    // 👈 [수정 4] 기존 loadInitialData 함수 수정
    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // ▼▼▼ [수정] 채팅방 로직을 refreshChatList() 호출로 변경 ▼▼▼
            val chatRoomsJob = async { refreshChatList() }
            val userProfileJob = async { userRepository.fetchAndSaveUserProfile() }

            // ▼▼▼ [수정] chatRoomsJob.await() 추가, profileResult는 뒤에서 await ▼▼▼
            chatRoomsJob.await()
            val profileResult = userProfileJob.await()
            // ▲▲▲ [수정] ▲▲▲


            // ▼▼▼ 프로필 로직 (유지) ▼▼▼
            if (profileResult is ApiResult.Success) {
                val savedUser = userRepository.getUserProfile().first()
                Log.d("MainViewModel", "✅ DataStore 저장 데이터 확인: $savedUser")

                val s3ImageUrl = savedUser?.profileImage?.removePrefix(AppConstants.S3_BASE_URL)
                Log.d("MainViewModel", "✅ s3ImageUrl URL 획득: $s3ImageUrl")

                if (s3ImageUrl != null) {
                    try {
                        val presignedUrl =
                            imageStorageRepository.getPresignedImageUrl(s3ImageUrl).getOrNull()
                        Log.d("MainViewModel", "✅ *Presigned URL 획득: $presignedUrl")

                        _uiState.update { it.copy(viewableProfileImageUrl = presignedUrl) }

                    } catch (e: Exception) {
                        Log.e("MainViewModel", "❌ Presigned URL 획득 실패", e)
                    }
                }

            } else {
                Log.e("MainViewModel", "❌ 사용자 프로필 가져오기/저장 실패")
            }
            // ▲▲▲ 프로필 로직 (유지) ▲▲▲


            // ▼▼▼ [삭제] 기존 채팅 목록 로직은 refreshChatList()로 이동했으므로 여기선 삭제 ▼▼▼
            // if (chatResult is ApiResult.Success) { ... } 블록 전체 삭제
            // ▲▲▲ [삭제] ▲▲▲

            _uiState.update { it.copy(isLoading = false) }
        }
    }


    // [유지] Room DB 업데이트
    fun markChatRoomAsRead(chatId: Long) {
        Log.d("ChatReadDebug", "6. markChatRoomAsRead called with chatId: $chatId")
        viewModelScope.launch {
            chatRepository.markRoomAsReadInDb(chatId)
            Log.d("MainViewModel", "Room in Repository marked as read.")
        }
    }


    // ---------------- [유지] ----------------
    // (위치 관련 로직은 STOMP와 무관하므로 모두 유지합니다)

    fun selectNewMainLocation(location: LocationUiModel) {
        Log.d("LocationUpdate", "🚀 selectNewMainLocation 호출됨")
        Log.d("LocationUpdate", "  > 선택된 주소: ${location.name} (ID: ${location.id})")

        viewModelScope.launch {
            Log.d("LocationUpdate", "  > userRepository.editMyLocation 실행...")
            userRepository.editMyLocation(location.id, location.name)
            Log.d("LocationUpdate", "  > API 요청 완료.")
        }
    }

    fun addFavoriteLocation(id: Int, name: String) {
        viewModelScope.launch {
            userRepository.addFavoriteLocation(id, name)
        }
    }

    fun updateUserLocation(locationId: Int, address: String) {
        viewModelScope.launch {
            userRepository.editMyLocation(locationId, address)
        }
    }
    // ---------------------------------------------------

    // [유지] onCleared()에서 STOMP 연결 해제 로직 삭제
    override fun onCleared() {
        Log.d("MainViewModel", "onCleared: ViewModel 파괴")
        super.onCleared()
    }
}