package com.example.raon.features.search.ui


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raon.features.search.data.local.RecentSearchEntity
import com.example.raon.features.search.domain.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// 1. UI 상태 정의
data class SearchInputUiState(
    val recentSearches: List<RecentSearchEntity> = emptyList()
)

@HiltViewModel
class SearchInputViewModel @Inject constructor(
    private val searchRepository: SearchRepository // 2. Repository 주입
) : ViewModel() {

    // 3. (읽기) Repository의 Flow를 UI StateFlow로 변환
    val uiState: StateFlow<SearchInputUiState> =
        searchRepository.getRecentSearches()
            .map { list -> SearchInputUiState(recentSearches = list) } // DB Entity 리스트를 UiState로 매핑
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L), // 화면이 활성화된 동안 구독
                initialValue = SearchInputUiState() // 초기값
            )

    // --- 4. (쓰기) 검색 실행 시 호출되어 검색어를 저장 ---
    fun onSearch(query: String) {
        // 공백만 있는 검색어는 저장하지 않음
        val trimmedQuery = query.trim()
        if (trimmedQuery.isNotBlank()) {
            viewModelScope.launch {
                searchRepository.addRecentSearch(trimmedQuery)
            }
        }
    }
    // --- 추가 끝 ---

    // 5. (삭제) 특정 검색어 삭제
    fun onDeleteSearch(query: String) {
        viewModelScope.launch {
            searchRepository.deleteRecentSearch(query)
        }
    }

    // 6. (전체 삭제) 전체 삭제
    fun onDeleteAllSearches() {
        viewModelScope.launch {
            searchRepository.deleteAllRecentSearches()
        }
    }
}