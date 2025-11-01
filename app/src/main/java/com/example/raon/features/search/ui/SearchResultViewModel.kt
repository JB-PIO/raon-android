package com.example.raon.features.search.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raon.features.search.domain.repository.SearchRepository
import com.example.raon.features.search.ui.model.SearchItemUiModel
import com.example.raon.features.user.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 검색 결과 화면의 UI 상태를 모두 담는 데이터 클래스입니다.
 * 이 객체 하나만 있으면 화면을 온전히 그릴 수 있습니다.
 */
data class SearchResultUiState(
    // 1. 데이터 관련 상태
    val products: List<SearchItemUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",

    // 2. 필터 및 정렬 관련 전체 상태
//    val query: String = "",
    val sortOption: String = "createdAt,desc", // 기본값: 최신순
    val status: String? = "AVAILABLE",      // 기본값: 판매중
    val categoryId: Int? = null,
    val locationId: Int? = null,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val condition: String? = null,          // 예: "USED", "NEW"
    val tradeType: String? = null           // 예: "DIRECT", "DELIVERY"
)

@HiltViewModel
class SearchResultViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // UI가 관찰할 단 하나의 상태 객체
    private val _uiState = MutableStateFlow(SearchResultUiState())
    val uiState: StateFlow<SearchResultUiState> = _uiState.asStateFlow()


    // 4. ViewModel 초기화 시 DataStore의 값을 로드
    init {
        viewModelScope.launch {
            // DataStore에서 저장된 사용자의 locationId를 가져옴 (첫 번째 값만)
            // UserRepository.kt에 정의된 함수를 사용합니다.
            val userLocationId = userRepository.getUserProfile().firstOrNull()?.locationId


            Log.d("위치 데이터", "위치 데이터 : $userLocationId")




            if (userLocationId != null) {
                _uiState.update { currentState ->
                    currentState.copy(locationId = userLocationId)
                }
            }
        }
    }


    // --- UI 이벤트를 처리하는 공개 함수 ---

    /** 검색 시작 */
    fun onSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        fetchProducts()
    }

    /** 검색어 변경 */
    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        fetchProducts()
    }


    /** 정렬 변경 */
    fun onSortChanged(sortValue: String) {
        _uiState.update { it.copy(sortOption = sortValue) }
        fetchProducts()
    }

    /** 판매 상태 변경 (판매중만 보기 스위치) */
    fun onSaleStatusChanged(isSaleOnly: Boolean) {
        _uiState.update {
            it.copy(status = if (isSaleOnly) "AVAILABLE" else null)
        }
        fetchProducts()
    }

    /** 카테고리 변경 */
    fun onCategoryChanged(newCategoryId: Int?) {
        _uiState.update { it.copy(categoryId = newCategoryId) }
        fetchProducts()
    }

    /** 가격 범위 변경 */
    fun onPriceChanged(min: Int?, max: Int?) {
        _uiState.update { it.copy(minPrice = min, maxPrice = max) }
        fetchProducts()
    }

    // ... 필요한 다른 필터 변경 함수들도 동일한 패턴으로 추가 ...

    // --- 핵심 로직: 데이터를 가져오는 비공개 함수 ---

    private fun fetchProducts() {
        val currentState = _uiState.value
        if (currentState.searchQuery.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 현재 UI 상태 객체에 저장된 모든 필터 값을 Repository에 전달
            searchRepository.searchProducts(
                keyword = currentState.searchQuery,
//                keyword = currentState.searchQuery,
                page = 0, // 페이지네이션은 추후 구현
                size = 20,
                sort = currentState.sortOption,
                status = currentState.status,
                categoryId = currentState.categoryId,
                locationId = currentState.locationId,
                minPrice = currentState.minPrice,
                maxPrice = currentState.maxPrice,
                condition = currentState.condition,
                tradeType = currentState.tradeType
            ).onSuccess { productList ->
                _uiState.update {
                    it.copy(isLoading = false, products = productList)
                }

                Log.d("데이터_로딩_성공", "데이터_로딩_성공 minPrice: ${uiState.value.minPrice}")
                Log.d("데이터_로딩_성공", "데이터_로딩_성공 maxPrice: ${uiState.value.maxPrice}")


            }.onFailure { exception ->
                _uiState.update {
                    it.copy(isLoading = false, error = "데이터 로딩 실패: ${exception.message}")
                }
            }
        }
    }
}

