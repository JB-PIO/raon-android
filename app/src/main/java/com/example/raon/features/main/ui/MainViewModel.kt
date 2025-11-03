package com.example.raon.features.main.ui

// [수정됨] 1. 불필요한 주석 제거
// import com.example.raon.core.service.NotificationService
// [수정] 'ChatRoomInfo' DTO 대신 Domain Model 'ChatRoom'을 import 합니다.
// [신규] combine import 추가
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raon.core.common.AppConstants
import com.example.raon.core.common.CurrentScreenManager
import com.example.raon.core.network.ApiResult
import com.example.raon.core.network.repository.ImageStorageRepository
import com.example.raon.core.notification.ChatNotificationHelper
import com.example.raon.features.chat.data.remote.StompService
import com.example.raon.features.chat.data.remote.dto.ChatMessageDto
import com.example.raon.features.chat.data.remote.dto.ChatRoomInfo
import com.example.raon.features.chat.domain.model.ChatRoom
import com.example.raon.features.chat.domain.repository.ChatRepository
import com.example.raon.features.item.ui.list.LocationUiModel
import com.example.raon.features.user.domain.model.User
import com.example.raon.features.user.domain.repository.UserRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject


data class MainUiState(
    // [수정] DTO(ChatRoomInfo)가 아닌 Domain Model(ChatRoom)을 사용합니다.
    val chatRooms: List<ChatRoom> = emptyList(),
    val unreadChatCount: Int = 0,
    val isLoading: Boolean = true,
    // [1. Presigned URL을 저장할 변수 추가]
    val viewableProfileImageUrl: String? = null,


    val locationName: String = "위치 정보 없음", // 현재 위치 이름 (UI 표시용)
    val availableLocations: List<LocationUiModel> = emptyList() // 드롭다운 목록
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle,
    private val imageStorageRepository: ImageStorageRepository, // S3에 업로드 하는 Repository
    private val stompService: StompService,
    private val currentScreenManager: CurrentScreenManager,
    private val chatNotificationHelper: ChatNotificationHelper // 주입 확인
    // [수정됨] 2. 불필요한 주석 제거
    // private val notificationService: NotificationService
) : ViewModel() {

    // [수정] _uiState는 이제 '채팅 목록 외의' 상태(로딩, 프로필)만 관리합니다.
    private val _uiState = MutableStateFlow(
        MainUiState(
            isLoading = true,
            chatRooms = emptyList()
        )
    )

    // [신규] Room DB의 채팅방 목록을 Domain Model(ChatRoom)로 구독합니다.
    private val chatRoomsFlow: StateFlow<List<ChatRoom>> = chatRepository.getChatRoomsFromDb()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // [신규] UI가 최종적으로 구독할 uiState 입니다.
    // _uiState(로딩 상태 등)와 chatRoomsFlow(Room 데이터)를 조합하여 완성된 UI 상태를 만듭니다.
    val uiState: StateFlow<MainUiState> = combine(
        _uiState, // isLoading, viewableProfileImageUrl 등
        chatRoomsFlow // chatRooms (List<ChatRoom>)
    ) { state, rooms ->
        state.copy(
            chatRooms = rooms,
            unreadChatCount = rooms.sumOf { it.unreadCount } // Room 데이터를 기반으로 안 읽은 수 자동 계산
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )


    // (선택 사항) StompService가 String을 반환하므로 파싱을 위해 Gson 인스턴스 추가
    private val gson = Gson()

    val userProfile: StateFlow<User?> = userRepository.getUserProfile()
        .onEach { user -> // <-- 이 부분을 추가하세요!
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


    // [추가] 즐겨찾기 위치 목록
    val favoriteLocations: StateFlow<List<LocationUiModel>> =
        userRepository.getFavoriteLocations()
            .map { pairs ->
                // Pair<Int, String>을 LocationUiModel로 변환
                pairs.map { LocationUiModel(id = it.first, name = it.second) }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // ▼▼▼ [수정된 부분] ▼▼▼
    companion object {
        // MainViewModel 인스턴스가 여러 개 생성되더라도,
        // DB 저장 Collector는 앱에서 단 한 번만 실행되도록 보장하는 플래그
        private val isCacheCollectorRunning = AtomicBoolean(false)
    }
    // ▲▲▲ [수정된 부분] ▲▲▲

    init {
        // 1. (STOMP 연결 코드는 connectToStomp() 함수로 분리됨 - 기존 코드 유지)

        // ▼▼▼ [수정된 부분] ▼▼▼
        // 2. [수정] STOMP 메시지 수신 및 DB 저장 (SSoT 전담)
        // compareAndSet: "현재 값이 false이면 true로 바꾸고, true를 반환"
        // 이 로직을 통해 앱 전체에서 이 블록이 단 한 번만 실행되도록 보장합니다.
        if (isCacheCollectorRunning.compareAndSet(false, true)) {
            viewModelScope.launch {
                try {
                    Log.d("MainViewModel", "🚀🚀🚀 STOMP DB Caching Collector 시작 (최초 1회) 🚀🚀🚀")
                    // (ChatRepositoryImpl에 구현된 cacheStompMessages 호출)
                    chatRepository.cacheStompMessages()
                } catch (e: Exception) {
                    Log.e("MainViewModel", "❌ STOMP Caching (DB Save) Coroutine 실패", e)
                }
            }
        } else {
            // 이미 다른 MainViewModel 인스턴스가 Collector를 실행 중
            Log.d("MainViewModel", "🔵 STOMP DB Caching Collector는 이미 실행 중입니다.")
        }
        // ▲▲▲ [수정된 부분] ▲▲▲

        // 3. [수정] 알림을 위해 '단순' 메시지 구독 (DB 저장 X)
        // (이 함수는 이제 DB 저장을 하지 않는 'observeMessages'를 호출하므로 안전함)
        observeGlobalMessages()

        // 4. [수정] 서버 데이터를 가져와 'Room DB'에 저장 (기존 코드와 동일)
        loadInitialData()


        // 5. [수정] SavedStateHandle (기존 코드와 동일)
        viewModelScope.launch {
            savedStateHandle.getStateFlow<Long?>("read_chat_room_id", null)
                .collect { readChatId ->

                    //  [로그 추가] SavedStateHandle로부터 값을 받았는지 확인합니다.
                    Log.d("ChatReadDebug", "4. MainViewModel collected chatId: $readChatId")

                    if (readChatId != null && readChatId != -1L) {
                        Log.d("MainViewModel", "✅ Chat room read result received: $readChatId")
                        // [수정] Room DB를 업데이트합니다.
                        markChatRoomAsRead(readChatId)
                        // 처리가 끝난 결과는 반드시 제거합니다.
                        savedStateHandle.remove<Long>("read_chat_room_id")

                        Log.d(
                            "ChatReadDebug",
                            "5. Processed and removed chatId from SavedStateHandle."
                        )
                    }
                }
        }
    }

    // (connectToStomp 함수는 기존과 동일)
    fun connectToStomp() {
        Log.d("MainViewModel", "STOMP 연결 시도 (ON_RESUME)")
        viewModelScope.launch {
            try {
                // 1. STOMP 연결
                chatRepository.connectStomp(0L) // (0L은 현재 코드 기준)

                // 2. [권장] 재연결 시, 혹시 놓친 데이터가 있는지 채팅방 목록을 다시 동기화
                chatRepository.getChatRoomList(page = 0)
                Log.d("MainViewModel", "✅ STOMP 연결 및 채팅 목록 동기화 시도 완료")

            } catch (e: Exception) {
                Log.e("MainViewModel", "❌ STOMP 연결 시도 실패", e)
            }
        }
    }

    // (disconnectFromStomp 함수는 기존과 동일)
    fun disconnectFromStomp() {
        Log.d("MainViewModel", "STOMP 연결 해제 (ON_PAUSE)")
        viewModelScope.launch {
            try {
                chatRepository.disconnectStomp()
                Log.d("MainViewModel", "✅ STOMP 연결 해제 완료")
            } catch (e: Exception) {
                Log.e("MainViewModel", "❌ STOMP 연결 해제 실패", e)
            }
        }
    }


    // [수정] 이 함수는 이제 DB 저장을 하지 않는 observeMessages를 호출합니다.
    private fun observeGlobalMessages() {
        viewModelScope.launch {
            chatRepository.observeMessages(0L) // (Repo에서 DB 저장을 안 함)
                .catch { e -> Log.e("MainViewModel", "❌ STOMP global observe error", e) }
                .collect { messageJson ->
                    // DB 저장은 cacheStompMessages가 알아서 하므로,
                    // 여기서는 알림 로직에만 집중합니다.
                    try {
                        val messageDto = gson.fromJson(messageJson, ChatMessageDto::class.java)

                        // --- [핵심] 알림 분기 로직 ---
                        val currentChatId = currentScreenManager.currentChatRoomId.value
                        val isAppInForeground = currentScreenManager.isAppInForeground.value

                        // ... (디버그 로그는 동일) ...
                        Log.d("NotificationDebug", "--- 새 메시지 수신 ---")
                        Log.d("NotificationDebug", "앱 포그라운드 상태: $isAppInForeground")
                        Log.d("NotificationDebug", "현재 보고있는 채팅방 ID: $currentChatId")
                        Log.d("NotificationDebug", "수신된 메시지 채팅방 ID: ${messageDto.chatId}")

                        if (isAppInForeground) {
                            // --- 앱이 켜져있을 때 ---
                            if (currentChatId == messageDto.chatId) {
                                // [CASE 1: 채팅방 일치]
                                Log.d("NotificationDebug", "판단: CASE 1 (현재 채팅방). 알림 없음.")
                                // Repo의 observeMessages가 Room을 업데이트했고,
                                // ChatRoomViewModel이 Room을 구독하므로 아무것도 안 함.
                            } else {
                                // [CASE 2: 채팅방 불일치]
                                Log.d("NotificationDebug", "판단: CASE 2 (다른 화면). 헤드업 알림 시도.")
                                chatNotificationHelper.showChatNotification(messageDto)
                            }
                        } else {
                            // --- 앱이 백그라운드일 때 ---
                            // [CASE 3: 백그라운드]
                            Log.d("NotificationDebug", "판단: CASE 3 (백그라운드). 기본 알림 시도.")
                            chatNotificationHelper.showChatNotification(messageDto)
                        }
                        // --- 분기 로직 끝 ---

                    } catch (e: Exception) {
                        Log.e("MainViewModel", "❌ Failed to parse global message", e)
                    }
                }
        }
    }

    // [삭제] updateChatListFromMessage 함수 전체 (더 이상 필요 없음)
    // private fun updateChatListFromMessage(messageDto: ChatMessageDto) { ... }


    // [수정] 서버 데이터를 가져와 Presigned URL 처리 후 'Room DB'에 저장
    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 프로필 가져오기 & 채팅 목록 가져오기 동시 실행
            // [수정] chatRepository.getChatRoomList (Repo에서 Room 저장 X)
            val chatRoomsJob = async { chatRepository.getChatRoomList(page = 0) }
            val userProfileJob = async { userRepository.fetchAndSaveUserProfile() }

            // 두 작업이 끝날 때까지 대기
            val chatResult = chatRoomsJob.await()
            val profileResult = userProfileJob.await()


            // ▼▼▼ 프로필 로직 (기존과 동일) ▼▼▼
            if (profileResult is ApiResult.Success) {
                // ... (프로필 로직) ...
                val savedUser = userRepository.getUserProfile().first()
                Log.d("MainViewModel", "✅ DataStore 저장 데이터 확인: $savedUser")

                val s3ImageUrl = savedUser?.profileImage?.removePrefix(AppConstants.S3_BASE_URL)
                Log.d("MainViewModel", "✅ s3ImageUrl URL 획득: $s3ImageUrl")

                if (s3ImageUrl != null) {
                    try {
                        val presignedUrl =
                            imageStorageRepository.getPresignedImageUrl(s3ImageUrl).getOrNull()
                        Log.d("MainViewModel", "✅ *Presigned URL 획득: $presignedUrl")

                        //  [수정] _uiState의 viewableProfileImageUrl만 업데이트
                        _uiState.update { it.copy(viewableProfileImageUrl = presignedUrl) }

                    } catch (e: Exception) {
                        Log.e("MainViewModel", "❌ Presigned URL 획득 실패", e)
                    }
                }

            } else {
                Log.e("MainViewModel", "❌ 사용자 프로필 가져오기/저장 실패")
            }
            // ▲▲▲ 프로필 로직 (기존과 동일) ▲▲▲


            // ▼▼▼ [수정] 채팅 목록 로직 ▼▼▼
            if (chatResult is ApiResult.Success) {
                var thumbnailUrl: String? = null    // items 썸네일 url

                // 서버에서 받은 채팅 목록 DTO (List<ChatRoomInfo>)
                val chatList = chatResult.data.data?.chats ?: emptyList()
                Log.d("MainViewModel", "✅ Chat list DTO loaded: ${chatList.size} rooms")


                // Presigned URL 가져오는 로직 (기존과 동일)
                val chatListWithUrls: List<ChatRoomInfo> =
                    chatList.map { chatRoomInfo -> // 반환 타입 명시
                        async {
                            // 썸네일 S3 키 파싱 (예: "items/image.jpg")
                            val thumbnailKey =
                                chatRoomInfo.product.thumbnail?.removePrefix(AppConstants.S3_BASE_URL)

                            Log.d("MainViewMode2l", "✅ thumbnailKey : ${thumbnailKey}")

                            // S3 키가 있으면 Presigned URL 요청
                            if (thumbnailKey != null) {
                                try {
                                    thumbnailUrl =
                                        imageStorageRepository.getPresignedImageUrl(thumbnailKey)
                                            .getOrNull()
                                    Log.d("MainViewMode2l", "✅ thumbnailUrl : ${thumbnailUrl}")

                                } catch (e: Exception) {
                                    Log.d("MainViewMode2l", "✅ 실패 : 실패")
                                    Log.e(
                                        "MainViewModel",
                                        "❌ Failed to load Thumbnail Presigned URL (chatId: ${chatRoomInfo.chatId})",
                                        e
                                    )
                                }
                            }
                            chatRoomInfo.copy(viewableThumbnailUrl = thumbnailUrl)
                        }
                    }.awaitAll() // 모든 썸네일 URL 요청이 완료될 때까지 기다림


                // [신규] Presigned URL이 포함된 목록을 Room DB에 저장합니다.
                try {
                    // (ChatRepository에 cacheChatRoomList(list)가 구현되어 있다고 가정)
                    chatRepository.cacheChatRoomList(chatListWithUrls)
                    Log.d("MainViewModel", "✅ Fetched list (with URLs) saved to Room.")
                } catch (e: Exception) {
                    Log.e("MainViewModel", "❌ Failed to save chat list to Room", e)
                }

                // [삭제] _uiState.update { ... } 로직 삭제
                // -> 데이터는 chatRoomsFlow가 Room을 구독하므로 자동으로 UI에 반영됩니다.

                Log.d("MainViewModel", "✅ 서버에서 받아온 User 데이터 확인: $userProfileJob")
                Log.d("MainViewModel", "✅ 서버에서 받아온 Chat 데이터 확인: $chatList")
                Log.d("MainViewMode2l", "✅ s3 서버에서 받아온 url 데이터 확인: $thumbnailUrl")

            } else {
                Log.w(
                    "MainViewModel",
                    "❌ Chat list fetch failed. Will use cached data if available."
                )
            }

            // [수정] 로딩 상태는 항상 마지막에 false로 변경
            _uiState.update { it.copy(isLoading = false) }
        }
    }


    // [수정] 이 함수는 이제 Repository를 통해 Room DB를 업데이트합니다.
    fun markChatRoomAsRead(chatId: Long) {

        //  [로그 추가] 이 함수가 실제로 호출되는지 확인합니다.
        Log.d("ChatReadDebug", "6. markChatRoomAsRead called with chatId: $chatId")

        viewModelScope.launch {
            // (ChatRepository에 markRoomAsReadInDb(id)가 구현되어 있다고 가정)
            chatRepository.markRoomAsReadInDb(chatId)
            Log.d("MainViewModel", "Room in Repository marked as read.")
        }

        // [삭제] _uiState.update { ... } 로직 전체 삭제
        // -> chatRoomsFlow가 Room의 변경을 감지하고 UI를 자동 갱신합니다.
    }


    // ---------------- [이 부분 수정] ----------------
    // (이하 동일)
    // 드롭다운에서 새 위치(즐겨찾기 또는 현재위치)를 선택했을 때 호출
    fun selectNewMainLocation(location: LocationUiModel) {
        // ❗️[로그 추가] 이 함수가 호출되는지 확인합니다.
        Log.d("LocationUpdate", "🚀 selectNewMainLocation 호출됨")
        Log.d("LocationUpdate", "  > 선택된 주소: ${location.name} (ID: ${location.id})")

        viewModelScope.launch {
            Log.d("LocationUpdate", "  > userRepository.editMyLocation 실행...")
            // 레파지토리를 통해 API 호출 및 로컬 DataStore 업데이트
            userRepository.editMyLocation(location.id, location.name) // 👈 주석 해제
            Log.d("LocationUpdate", "  > API 요청 완료.")
            // userProfile Flow가 자동으로 갱신되므로
            // ItemListViewModel 등 userProfile을 구독하는 모든 곳이 자동 갱신됩니다.
        }
    }

    // [추가] '내 동네 설정'에서 위치를 선택했을 때 호출 (MainGraph에서 사용)
    fun addFavoriteLocation(id: Int, name: String) {
        viewModelScope.launch {
            userRepository.addFavoriteLocation(id, name)
            // favoriteLocations Flow가 자동으로 갱신됩니다.
        }
    }

    // [추가] (MainGraph용) MainViewModel에서 위치를 직접 업데이트하는 함수
    // '내 동네 설정'이 아닌, 다른 경로로 위치를 변경할 때 사용 (예: 프로필 수정)
    fun updateUserLocation(locationId: Int, address: String) {
        viewModelScope.launch {
            userRepository.editMyLocation(locationId, address) //  주석 해제
        }
    }
    // ---------------------------------------------------

    // 4. MainViewModel 종료 시 STOMP 연결 해제 (로그아웃 시에도 호출 필요)
    override fun onCleared() {
        Log.d("MainViewModel", "onCleared: Disconnecting STOMP...")
        // 🔽 [수정] 새로 만든 disconnect 함수를 여기서도 호출
        disconnectFromStomp()
        super.onCleared()
    }
}