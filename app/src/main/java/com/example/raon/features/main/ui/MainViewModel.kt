package com.example.raon.features.main.ui

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raon.core.common.AppConstants
import com.example.raon.core.network.ApiResult
import com.example.raon.core.network.repository.ImageStorageRepository
import com.example.raon.features.chat.data.remote.dto.ChatRoomInfo
import com.example.raon.features.chat.domain.repository.ChatRepository
import com.example.raon.features.item.ui.list.LocationUiModel
import com.example.raon.features.user.domain.model.User
import com.example.raon.features.user.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class MainUiState(
    val chatRooms: List<ChatRoomInfo> = emptyList(),
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
    private val imageStorageRepository: ImageStorageRepository// S3에 업로드 하는 Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

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


    init {
        loadInitialData()


        //  [핵심 수정] ViewModel이 직접 결과를 감시하도록 로직을 옮깁니다.
        viewModelScope.launch {
            savedStateHandle.getStateFlow<Long?>("read_chat_room_id", null)
                .collect { readChatId ->

                    //  [로그 추가] SavedStateHandle로부터 값을 받았는지 확인합니다.
                    Log.d("ChatReadDebug", "4. MainViewModel collected chatId: $readChatId")

                    if (readChatId != null && readChatId != -1L) {
                        Log.d("MainViewModel", "✅ Chat room read result received: $readChatId")
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

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 프로필 가져오기 & 채팅 목록 가져오기 동시 실행
            val chatRoomsJob = async { chatRepository.getChatRoomList(page = 0) }
            val userProfileJob = async { userRepository.fetchAndSaveUserProfile() }

            // 두 작업이 끝날 때까지 대기
            val chatResult = chatRoomsJob.await()
            val profileResult = userProfileJob.await()


            // ▼▼▼ 이 부분이 핵심입니다 ▼▼▼
            // 1. 프로필 저장 작업이 성공했는지 확인합니다.
            if (profileResult is ApiResult.Success) {
                // 2. UserRepository의 getUserProfile() Flow에서 첫 번째 값을 꺼내옵니다.
                //    이것이 바로 DataStore에 저장된 최신 데이터입니다.
                val savedUser = userRepository.getUserProfile().first()

                // 3. 꺼내온 실제 데이터를 로그로 출력합니다.
                Log.d("MainViewModel", "✅ DataStore 저장 데이터 확인: $savedUser")


                // ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ [수정 2] 완료 ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ
                // [수정 2] 'init' 블록에서 이동된 Presigned URL 요청 로직
                // 1. 저장된 사용자 정보에서 이미지 URL을 파싱합니다.
                val s3ImageUrl = savedUser?.profileImage?.removePrefix(AppConstants.S3_BASE_URL)

                Log.d("MainViewModel", "✅ s3ImageUrl URL 획득: $s3ImageUrl")


                // 2. null이 아닌지 확인하고 'suspend' 함수를 호출합니다.
                if (s3ImageUrl != null) {
                    try {
                        val presignedUrl =
                            imageStorageRepository.getPresignedImageUrl(s3ImageUrl).getOrNull()
                        Log.d("MainViewModel", "✅ *Presigned URL 획득: $presignedUrl")

                        //  [2. 획득한 URL을 UI 상태에 업데이트]
                        _uiState.update { it.copy(viewableProfileImageUrl = presignedUrl) }

                        // TODO: 획득한 URL을 _uiState에 저장하여 UI에 반영해야 합니다.
                        // 예: _uiState.update { it.copy(viewableProfileUrl = presignedUrl) }

//                        userProfile.value.profileImage

                    } catch (e: Exception) {
                        Log.e("MainViewModel", "❌ Presigned URL 획득 실패", e)
                    }
                }
                // ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ [수정 2] 완료 ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ

            } else {
                Log.e("MainViewModel", "❌ 사용자 프로필 가져오기/저장 실패")
            }

            // 채팅 목록 상태 업데이트 (기존과 동일)
            if (chatResult is ApiResult.Success) {
                var thumbnailUrl: String? = null    // items 썸네일 url


                // 서버에서 받은 채팅 목록 DTO (List<ChatRoomInfo>)
                val chatList = chatResult.data.data?.chats ?: emptyList()
                Log.d("MainViewModel", "✅ Chat list DTO loaded: ${chatList.size} rooms")


                //  [주석 5] 채팅 DTO 리스트를 UI 모델 리스트로 변환 (각 썸네일 Presigned URL 포함)
                val chatListWithUrls: List<ChatRoomInfo> =
                    chatList.map { chatRoomInfo -> // 반환 타입 명시                    // 각 채팅방 썸네일에 대해 Presigned URL 요청을 비동기로 시작
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
                //  [주석 5] 완료


                // 안 읽은 채팅수 계산
                val unreadCount = chatList.sumOf { it.unreadCount }

                _uiState.update {
                    it.copy(
                        isLoading = false,
//                        chatRooms = chatList,
                        chatRooms = chatListWithUrls,

                        unreadChatCount = unreadCount,
//                        viewableProfileImageUrl = thumbnailUrl
                    )
                }
                Log.d("MainViewModel", "✅ 서버에서 받아온 User 데이터 확인: $userProfileJob")
                Log.d("MainViewModel", "✅ 서버에서 받아온 Chat 데이터 확인: $chatList")
                Log.d("MainViewMode2l", "✅ s3 서버에서 받아온 url 데이터 확인: $thumbnailUrl")

            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }


    // [ 채팅방 읽음 처리 함수 ]
    fun markChatRoomAsRead(chatId: Long) {

        //  [로그 추가] 이 함수가 실제로 호출되는지 확인합니다.
        Log.d("ChatReadDebug", "6. markChatRoomAsRead called with chatId: $chatId")

        _uiState.update { currentState ->
            // 현재 채팅방 목록에서 ID가 일치하는 채팅방을 찾습니다.
            val updatedChatRooms = currentState.chatRooms.map { chatRoom ->
                if (chatRoom.chatId == chatId) {
                    // ID가 일치하면 unreadCount를 0으로 바꾼 새 객체를 만듭니다.
                    chatRoom.copy(unreadCount = 0)
                } else {
                    // ID가 다르면 기존 객체를 그대로 사용합니다.
                    chatRoom
                }
            }
            // 업데이트된 새 리스트로 UI 상태를 교체합니다.
            currentState.copy(chatRooms = updatedChatRooms)
        }
    }


    // ---------------- [이 부분 수정] ----------------
    // [추가] 드롭다운에서 새 위치(즐겨찾기 또는 현재위치)를 선택했을 때 호출
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
}