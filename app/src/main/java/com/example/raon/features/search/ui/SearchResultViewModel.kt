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
    val sortOption: String = "createdAt,desc", // 기본값: 최신순
    val status: String? = "AVAILABLE",      // 기본값: 판매중
    val categoryId: Int? = null,
    val categoryName: String? = null,
    val locationId: Int? = null,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val condition: String? = null,
    val tradeType: String? = null,

    // 3. 페이징 관련 상태 추가
    val currentPage: Int = 0,
    val canLoadMore: Boolean = true,
    val isRefreshing: Boolean = false // 상단 Pull-to-Refresh 상태
)

@HiltViewModel
class SearchResultViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // UI가 관찰할 단 하나의 상태 객체
    private val _uiState = MutableStateFlow(SearchResultUiState())
    val uiState: StateFlow<SearchResultUiState> = _uiState.asStateFlow()

    // 한 번에 불러올 아이템 개수
    private val pageSize = 20

    // 4. ViewModel 초기화 시 DataStore의 값을 로드
    init {
        viewModelScope.launch {
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

    /** 검색 시작 (검색어 입력 후 엔터) */
    fun onSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        refresh()
    }

    /** 검색어 변경 (검색창에서 실시간 변경) */
    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        refresh()
    }


    /** 정렬 변경 */
    fun onSortChanged(sortValue: String) {
        _uiState.update { it.copy(sortOption = sortValue) }
        refresh()
    }

    /** 판매 상태 변경 (판매중만 보기 스위치) */
    fun onSaleStatusChanged(isSaleOnly: Boolean) {
        _uiState.update {
            it.copy(status = if (isSaleOnly) "AVAILABLE" else null)
        }
        refresh()
    }

    /** 카테고리 변경 */
    fun onCategoryChanged(newCategoryId: Int?, newCategoryName: String?) {
        _uiState.update {
            it.copy(
                categoryId = newCategoryId,
                categoryName = newCategoryName
            )
        }
        refresh()
    }

    /** 가격 범위 변경 */
    fun onPriceChanged(min: Int?, max: Int?) {
        _uiState.update { it.copy(minPrice = min, maxPrice = max) }
        refresh()
    }

    /** (수정) Pull-to-Refresh 또는 필터 변경 시 호출되는 "새로고침" 함수 */
    fun refresh() {
        // 이미 로딩/새로고침 중이면 중복 실행 방지
        if (_uiState.value.isLoading || _uiState.value.isRefreshing) return

        val currentState = _uiState.value
        if (currentState.searchQuery.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshing = true, // 상단 새로고침 애니메이션 시작
                    isLoading = false,   // 중앙 로딩은 숨김
                    error = null,
                    products = emptyList(), // 목록 비우기
                    currentPage = 0,
                    canLoadMore = true
                )
            }

            searchRepository.searchProducts(
                keyword = currentState.searchQuery,
                page = 0, // "새로고침"이므로 0페이지 요청
                size = pageSize,
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
                    it.copy(
                        isRefreshing = false, // 새로고침 완료
                        isLoading = false,
                        products = productList, // 목록을 새로 교체
                        currentPage = 1, // 다음 페이지는 1
                        canLoadMore = productList.size == pageSize
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isRefreshing = false, // 새로고침 실패
                        isLoading = false,
                        error = "데이터 로딩 실패: ${exception.message}"
                    )
                }
            }
        }
    }

    /** (유지) 스크롤이 끝에 도달했을 때 호출되는 "더 불러오기" 함수 */
    fun loadMoreItems() {
        // 로딩/새로고침 중이거나, 더 이상 불러올 데이터가 없으면 중단
        if (_uiState.value.isLoading || _uiState.value.isRefreshing || !_uiState.value.canLoadMore) {
            return
        }

        val currentState = _uiState.value
        if (currentState.searchQuery.isBlank()) return

        viewModelScope.launch {
            // "더 불러오기"는 isLoading(하단 스피너)을 사용
            _uiState.update { it.copy(isLoading = true) }

            searchRepository.searchProducts(
                keyword = currentState.searchQuery,
                page = currentState.currentPage, // 현재 페이지 번호로 요청
                size = pageSize,
                sort = currentState.sortOption,
                status = currentState.status,
                categoryId = currentState.categoryId,
                locationId = currentState.locationId,
                minPrice = currentState.minPrice,
                maxPrice = currentState.maxPrice,
                condition = currentState.condition,
                tradeType = currentState.tradeType
            ).onSuccess { newProductList ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        products = it.products + newProductList, // 기존 목록에 새 목록 "추가"
                        currentPage = it.currentPage + 1, // 페이지 번호 증가
                        canLoadMore = newProductList.size == pageSize
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "데이터 로딩 실패: ${exception.message}"
                    )
                }
            }
        }
    }
}