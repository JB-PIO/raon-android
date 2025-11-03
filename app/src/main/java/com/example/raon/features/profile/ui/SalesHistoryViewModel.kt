package com.example.raon.features.profile.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// ▼▼▼ [1] TimeExtensions 및 Log 임포트 추가 ▼▼▼
import com.example.raon.core.common.toInstant
import com.example.raon.core.common.toKSTLocalDateTime
import com.example.raon.core.common.toRelativeTimeString
// ▲▲▲ [1] 임포트 완료 ▲▲▲
import com.example.raon.core.network.ApiResult
import com.example.raon.core.ui.model.ItemListUiModel
import com.example.raon.features.item.data.repository.ItemRepository
import com.example.raon.features.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SalesHistoryUiState(
    val sellingItems: List<ItemListUiModel> = emptyList(),
    val reservedItems: List<ItemListUiModel> = emptyList(),
    val soldItems: List<ItemListUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedItemIdForStatusChange: Int? = null
)

sealed class SalesHistoryEvent {
    data class NavigateToBuyerSelection(val itemId: Int) : SalesHistoryEvent()
    data class ShowError(val message: String) : SalesHistoryEvent()
}


@HiltViewModel
class SalesHistoryViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesHistoryUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SalesHistoryEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        fetchMyProducts()
    }

    fun fetchMyProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            profileRepository.getMyProducts()
                .onSuccess { myItems ->

                    // ▼▼▼ [2] 시간 포맷팅 로직 (핵심) ▼▼▼
                    // 원본 리스트(myItems)를 '방금 전'으로 가공합니다.
                    val formattedItems = myItems.map { item ->
                        try {
                            // (가정: ItemListUiModel이 'createdAt' 필드를 가진 data class)
                            item.copy(
                                timeAgo = formatTimeAgo(item.timeAgo) // 👈 시간 필드명 확인 필요
                            )
                        } catch (e: Exception) {
                            Log.w("SalesHistoryVM", "Item 시간 변환 실패 (원본 반환): ${e.message}")
                            item // 예외 발생 시 원본 반환
                        }
                    }
                    // ▲▲▲ [2] 로직 완료 ▲▲▲

                    // ▼▼▼ [3] 'formattedItems'를 사용해 그룹화 ▼▼▼
                    val groupedByStatus = formattedItems.groupBy { it.status }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            sellingItems = groupedByStatus["AVAILABLE"] ?: emptyList(),
                            reservedItems = groupedByStatus["RESERVED"] ?: emptyList(),
                            soldItems = groupedByStatus["SOLD"] ?: emptyList()
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message)
                    }
                }
        }
    }

    /**
     * [함수 1 - 분기용]
     * UI(BottomSheet)에서 '거래완료' 클릭 시 호출됨.
     * 'SOLD' + 'buyerId=null' 이면 네비게이션 이벤트만 발생시킴.
     */
    fun updateProductStatus(itemId: Int, newStatus: String, buyerId: Int? = null) {

        if (newStatus == "SOLD" && buyerId == null) {
            _uiState.update { it.copy(selectedItemIdForStatusChange = itemId) }
            viewModelScope.launch {
                _eventFlow.emit(SalesHistoryEvent.NavigateToBuyerSelection(itemId))
            }
            return // API 호출 안 함
        }

        // 'AVAILABLE', 'RESERVED'는 즉시 API 호출
        executeUpdateStatus(itemId, newStatus, buyerId)
    }

    /**
     * [함수 2 - 실행용]
     * ▼▼▼ [핵심 수정] private fun -> fun (public으로 변경) ▼▼▼
     * SalesHistoryScreen이 구매자 선택 결과를 받고 '직접' 호출할 함수.
     * 네비게이션 분기 없이 '무조건' API를 호출함.
     */
    fun executeUpdateStatus(itemId: Int, newStatus: String, buyerId: Int?) {
        viewModelScope.launch {
            Log.d(
                "SalesHistoryVM_Update",
                "[API CALL] Item ID: $itemId, New Status: $newStatus, Buyer ID: $buyerId"
            )

            _uiState.update { it.copy(isLoading = true) }
            val result = itemRepository.updateProductStatus(itemId, newStatus, buyerId)

            Log.d("SalesHistoryVM_Update", "API Result: $result")

            if (result is ApiResult.Success) {
                Log.d(
                    "SalesHistoryVM_Update",
                    "Status update successful. Fetching updated product list."
                )
                _uiState.update { it.copy(selectedItemIdForStatusChange = null) }
                fetchMyProducts() // 목록 새로고침
            } else {
                val errorMessage = when (result) {
                    is ApiResult.Error -> "API Error(Code: ${result.code}) - ${result.errorBody?.message}"
                    is ApiResult.Exception -> "Network Exception - ${result.e.message}"
                    else -> "Unknown Error"
                }
                Log.e("SalesHistoryVM_Update", "Status update failed: $errorMessage")
                _eventFlow.emit(SalesHistoryEvent.ShowError(errorMessage))
                _uiState.update { it.copy(isLoading = false, selectedItemIdForStatusChange = null) }
            }
        }
    }

    // ▼▼▼ [4] 헬퍼 함수를 ViewModel 클래스 내부에 private fun으로 추가 ▼▼▼
    /**
     * 시간 문자열을 "방금 전", "N분 전" 등으로 변환합니다.
     */
    private fun formatTimeAgo(dateTimeString: String): String {
        // 이미 "방금 전" 등으로 변환된 문자열이 다시 들어오는 경우를 방지
        if (dateTimeString.endsWith(" 전") || dateTimeString == "어제") {
            return dateTimeString
        }

        if (dateTimeString.isEmpty()) return ""
        return try {
            // 프로젝트의 검증된 시간 파서 (TimeExtensions.kt)를 사용
            dateTimeString.toInstant()?.toKSTLocalDateTime()?.toRelativeTimeString() ?: ""
        } catch (e: Exception) {
            Log.e("TimeParser", "formatTimeAgo 파싱 실패: $dateTimeString", e)
            "시간 정보 없음" // 파싱 실패 시 대체 텍스트
        }
    }
    // ▲▲▲ [4] 헬퍼 함수 추가 완료 ▲▲▲
}