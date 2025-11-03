package com.example.raon.features.main.ui

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val stompService: StompService
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

        // [유지] 서버 데이터를 가져와 'Room DB'에 저장 (초기 로드)
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
                        delay(2000L)

                        // 읽음 처리 직후에도 목록 갱신 (unreadCount 반영)
                        refreshChatList()
//                        loadInitialData()
                    }

//                    refreshChatList()

                }
        }

        // [유지] 새 채팅방 생성 후 복귀 시 강제 갱신 로직
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

        // ▼▼▼ [수정됨] STOMP 메시지 구독 및 DB 캐싱 로직 (Unit 반환에 맞춤) ▼▼▼
        viewModelScope.launch {
            Log.d("MainViewModel_STOMP", "STOMP 메시지 수신 및 DB 캐싱 처리 시작")
            try {
                // 1. Repository의 cacheStompMessages()는 Unit을 반환합니다.
                //    새 채팅방 감지 및 임시 엔티티 삽입은 Repository 내부에서 처리됩니다.
                //    따라서 ViewModel에서 별도의 반환값 확인 및 refreshChatList() 호출이 필요 없습니다.
                chatRepository.cacheStompMessages()


                // 2. Repository가 Unit을 반환하므로, 기존의 `val isNewRoom = ...` 코드를 삭제합니다.
                // 3. Repository가 DB를 갱신하면 `chatRoomsFlow`가 자동으로 새 데이터를 받아 UI가 갱신됩니다.

                /* * ❌ 주석 처리된 기존 코드 (타입 미스매치 발생 지점)
                 * val isNewRoom = chatRepository.cacheStompMessages()
                 * if (isNewRoom) { ... refreshChatList() ... }
                 */

            } catch (e: Exception) {
                // 이 예외는 STOMP 연결 취소/종료 시 주로 발생합니다.
                Log.e("MainViewModel_STOMP", "STOMP 메시지 처리 중 오류 발생/취소", e)
            }
        }
        // ▲▲▲ [수정 완료] ▲▲▲
    }

    // [유지] 서비스 시작 함수
    private fun startChatService() {
        val serviceIntent = Intent(context, ChatService::class.java).apply {
            action = ChatService.ACTION_START
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    // [유지] 서비스 중지 함수
    fun handleLogout() {
        viewModelScope.launch {
            // TODO: DataStore의 유저 정보/토큰 삭제 로직
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
     * [유지] 신규 함수: 채팅 목록만 서버에서 새로고침하고 Room DB를 덮어씁니다.
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
                            // 🔽 Presigned URL을 반영한 ChatRoomInfo 생성
                            chatRoomInfo.copy(viewableThumbnailUrl = thumbnailUrl)
                        }
                    }.awaitAll()

                try {
                    // 🔽 Room DB에 캐싱 (DB 덮어쓰기)
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
        }
    }


    // [유지] 초기 데이터 로드 함수 (채팅 목록 로직은 refreshChatList() 사용)
    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val chatRoomsJob = async { refreshChatList() } // 채팅 목록 새로고침
            val userProfileJob = async { userRepository.fetchAndSaveUserProfile() }

            chatRoomsJob.await() // 채팅방 로드 완료 대기
            val profileResult = userProfileJob.await() // 프로필 로드 완료 대기


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


    // ---------------- [유지] 위치 관련 로직 ----------------

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

    // [유지] onCleared()
    override fun onCleared() {
        Log.d("MainViewModel", "onCleared: ViewModel 파괴")
        super.onCleared()
    }
}