package com.example.raon.features.item.ui.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raon.core.common.AppConstants
import com.example.raon.core.network.ApiResult
import com.example.raon.core.network.repository.ImageStorageRepository
import com.example.raon.features.item.data.repository.ItemRepository
import com.example.raon.features.item.ui.detail.model.ItemDetailModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

// ------------------- UI State -------------------
data class ItemDetailUiState(
    val item: ItemDetailModel? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val viewableSellerProfileImageUrl: String? = null
)

// ------------------- UI Event -------------------
sealed class ItemDetailEvent {
    data class NavigateToChatRoom(val chatId: Long) : ItemDetailEvent()
    data class ShowError(val message: String) : ItemDetailEvent()
    object ProductDeleted : ItemDetailEvent()
    object ShowProductNotFoundError : ItemDetailEvent()
    object Refresh : ItemDetailEvent()

    // 채팅방 생성 완료 이벤트
    data class ChatRoomCreated(val newChatId: Long) : ItemDetailEvent() //
}

// ------------------- ViewModel -------------------
@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val imageStorageRepository: ImageStorageRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ItemDetailEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val itemId: Int? = savedStateHandle["itemId"]

    // ▼▼▼ [핵심 추가] 채팅방 화면에서 전달받은 채팅방 ID ▼▼▼
    // 이 ID가 -1L이 아니면, 이미 존재하는 채팅방에서 왔다는 의미입니다.
    private val sourceChatRoomId: Long = savedStateHandle.get<Long>("chatRoomId") ?: -1L

    init {
        if (itemId != null) {
            reloadData()
            increaseViewCount(itemId)
        } else {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "상품 ID를 불러올 수 없습니다.")
            }
        }
    }

    fun onEvent(event: ItemDetailEvent) {
        when (event) {
            ItemDetailEvent.Refresh -> reloadData()
            is ItemDetailEvent.NavigateToChatRoom -> { /* Do nothing */
            }

            is ItemDetailEvent.ProductDeleted -> { /* Do nothing */
            }

            is ItemDetailEvent.ShowError -> { /* Do nothing */
            }

            is ItemDetailEvent.ShowProductNotFoundError -> { /* Do nothing */
            }

            is ItemDetailEvent.ChatRoomCreated -> {
                /* Do nothing */
            }
        }
    }

    private fun reloadData() {
        val currentItemId = itemId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val detailDeferred = async { itemRepository.getItemDetail(currentItemId) }
                val favoriteStatusDeferred =
                    async { itemRepository.getFavoriteStatus(currentItemId) }

                val itemDetails = detailDeferred.await()
                val isFavorite = favoriteStatusDeferred.await()

                var sellerPresignedUrl: String? = null
                val sellerS3Key =
                    itemDetails.sellerProfileUrl?.removePrefix(AppConstants.S3_BASE_URL)

                if (sellerS3Key != null && itemDetails.sellerProfileUrl != AppConstants.DEFAULT_PROFILE_URL) {
                    try {
                        sellerPresignedUrl =
                            imageStorageRepository.getPresignedImageUrl(sellerS3Key).getOrNull()
                        Log.d(
                            "ItemDetailViewModel",
                            "✅ Seller Presigned URL loaded: $sellerPresignedUrl"
                        )
                    } catch (e: Exception) {
                        Log.e("ItemDetailViewModel", "❌ Failed to load seller Presigned URL", e)
                    }
                }

                val finalItemDetails = itemDetails.copy(
                    isFavorite = isFavorite,
                    viewCount = itemDetails.viewCount + 1
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        item = finalItemDetails,
                        viewableSellerProfileImageUrl = sellerPresignedUrl
                    )
                }

            } catch (e: Exception) {
                val errorMessage = if (e is HttpException && e.code() == 404) {
                    _eventFlow.emit(ItemDetailEvent.ShowProductNotFoundError)
                    _uiState.update { it.copy(isLoading = false) }
                    "404 에러 존재하지 않는 상품이거나 삭제되었습니다."
                } else {
                    "데이터를 불러오는 중 오류가 발생했습니다."
                }
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = errorMessage)
                }
            }
        }
    }

    fun deleteProduct() {
        if (itemId == null) {
            viewModelScope.launch { _eventFlow.emit(ItemDetailEvent.ShowError("상품 ID가 없어 삭제할 수 없습니다.")) }
            return
        }

        viewModelScope.launch {
            when (val result = itemRepository.deleteProduct(itemId)) {
                is ApiResult.Success -> {
                    _eventFlow.emit(ItemDetailEvent.ProductDeleted)
                }

                is ApiResult.Error -> {
                    val errorMessage = result.errorBody?.message ?: "삭제 중 오류가 발생했습니다."
                    _eventFlow.emit(ItemDetailEvent.ShowError(errorMessage))
                }

                is ApiResult.Exception -> {
                    _eventFlow.emit(
                        ItemDetailEvent.ShowError(result.e.message ?: "알 수 없는 오류가 발생했습니다.")
                    )
                }
            }
        }
    }

    // ▼▼▼ [핵심 수정] 채팅하기 버튼 클릭 시의 로직 변경 ▼▼▼
    fun onChatButtonClicked() {
        // 1. 채팅방에서 넘어왔는지 먼저 확인합니다.
        if (sourceChatRoomId != -1L) {
            // 채팅방에서 온 경우, 새 채팅방을 만들 필요 없이
            // "이 채팅방으로 돌아가줘" 라는 신호만 보냅니다.
            // AppNavigation에서 이 신호를 받고 popBackStack()을 실행할 것입니다.
            viewModelScope.launch {
                _eventFlow.emit(ItemDetailEvent.NavigateToChatRoom(sourceChatRoomId))
            }
            return // 여기서 함수 실행을 종료합니다.
        }

        // 2. 채팅방에서 온 것이 아니라면, 기존 로직을 그대로 실행합니다.
        if (itemId == null) {
            viewModelScope.launch { _eventFlow.emit(ItemDetailEvent.ShowError("상품 ID가 없어 채팅을 시작할 수 없습니다.")) }
            return
        }

        viewModelScope.launch {
            when (val result = itemRepository.getChatRoomForItem(itemId.toLong())) {
                is ApiResult.Success -> {
                    _eventFlow.emit(ItemDetailEvent.NavigateToChatRoom(result.data.data.chatId))
                }

                is ApiResult.Error -> {
                    if (result.code == 404 && result.errorBody?.code == "CHAT_404") {
                        createChatRoom(itemId.toLong())
                    } else {
                        val errorMessage = result.errorBody?.message ?: "에러 코드: ${result.code}"
                        _eventFlow.emit(ItemDetailEvent.ShowError(errorMessage))
                    }
                }

                is ApiResult.Exception -> {
                    _eventFlow.emit(ItemDetailEvent.ShowError(result.e.message ?: "알 수 없는 오류"))
                }
            }
        }
    }

    private suspend fun createChatRoom(itemId: Long) {
        when (val result = itemRepository.createChatForItem(itemId)) {
            is ApiResult.Success -> {

                val newChatId = result.data.data.chatId
                // [수정] 네비게이션 전에 채팅방 생성 완료 이벤트를 먼저 보냅니다.
                _eventFlow.emit(ItemDetailEvent.ChatRoomCreated(newChatId)) //
                _eventFlow.emit(ItemDetailEvent.NavigateToChatRoom(result.data.data.chatId))
            }

            is ApiResult.Error -> {
                _eventFlow.emit(ItemDetailEvent.ShowError("채팅방 생성 실패: ${result.code}"))
            }

            is ApiResult.Exception -> {
                _eventFlow.emit(ItemDetailEvent.ShowError(result.e.message ?: "알 수 없는 오류"))
            }
        }
    }

    private fun increaseViewCount(itemId: Int) {
        viewModelScope.launch {
            itemRepository.increaseViewCount(itemId)
        }
    }

    fun onFavoriteButtonClicked() {
        val currentItem = _uiState.value.item ?: return

        val newFavoriteState = !currentItem.isFavorite
        val newFavoriteCount =
            if (newFavoriteState) currentItem.favoriteCount + 1 else currentItem.favoriteCount - 1

        _uiState.update {
            it.copy(
                item = currentItem.copy(
                    isFavorite = newFavoriteState,
                    favoriteCount = newFavoriteCount
                )
            )
        }

        viewModelScope.launch {
            try {
                itemRepository.updateFavoriteStatus(currentItem.id, newFavoriteState)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(item = currentItem)
                }
                _eventFlow.emit(ItemDetailEvent.ShowError("찜 상태 변경에 실패했습니다."))
            }
        }
    }
}