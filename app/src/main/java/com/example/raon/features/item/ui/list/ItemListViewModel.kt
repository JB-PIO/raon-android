package com.example.raon.features.item.ui.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raon.features.item.data.repository.ItemRepository
import com.example.raon.features.item.ui.list.model.ItemUiModel
import com.example.raon.features.user.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest // 👈 'first' 대신 'collectLatest' import
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


// (파일 상단 또는 별도 파일에 추가)
data class LocationUiModel(
    val id: Int,
    val name: String
)

data class ItemListUiState(
    val items: List<ItemUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 0,
    val isRefreshing: Boolean = false, // 새로고침 상
    val itemsImageUrl: String = "",

    val locationId: Int? = null, // Int? 타입 유지

    val locationName: String = "위치 정보 없음", // 현재 위치 이름 (UI 표시용)
    val availableLocations: List<LocationUiModel> = emptyList() // 드롭다운 목록
)

@HiltViewModel
class ItemListViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val userRepository: UserRepository  // Datastore에서 Profile 주소 데이터 가져오기 위해서
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // ViewModel이 시작될 때 DataStore 구독을 시작합니다.
        observeUserProfileAndLoadItems()
    }

    /**
     * DataStore의 유저 프로필(위치)을 "구독(Observe)"합니다.
     * 유저 정보가 (MainViewModel에 의해) 갱신되면, 이 함수가 자동으로 다시 실행되어
     * 위치를 업데이트하고 아이템 목록을 새로고침합니다.
     */
    private fun observeUserProfileAndLoadItems() {
        viewModelScope.launch {
            // .first() 대신 .collectLatest를 사용해 DataStore의 변경사항을 계속 "구독"합니다.
            userRepository.getUserProfile().collectLatest { userProfile ->

                if (userProfile != null) {
                    // [흐름 1] DataStore에 유저 정보가 로드됨 (또는 갱신됨)
                    val newLocationId = userProfile.locationId
                    val newLocationName = userProfile.address
                    val currentLocationIdInUi = _uiState.value.locationId

                    // UI State를 새 위치 정보로 업데이트합니다.
                    _uiState.update {
                        it.copy(
                            locationId = newLocationId,
                            locationName = newLocationName
                        )
                    }

                    //  중요: UI가 알던 위치(null 또는 옛날 위치)와
                    //    DataStore의 새 위치가 다른 경우에만 'refresh'를 호출합니다.
                    if (currentLocationIdInUi != newLocationId) {
                        Log.d("ItemListViewModel", "✅ 새 위치($newLocationId) 수신. 아이템 목록을 새로고칩니다.")
                        refresh() // 새 위치로 목록 새로고침
                    } else {
                        Log.d("ItemListViewModel", "✅ 위치($newLocationId) 정보 재확인 (변경 없음).")
                    }

                } else {
                    // [흐름 2] DataStore가 아직 비어있음 (MainViewModel이 로딩 중)
                    Log.w("ItemListViewModel", "⏳ 위치 정보 대기 중... (user is null)")
                    _uiState.update {
                        it.copy(
                            locationId = null,
                            locationName = "위치 정보 없음",
                            isLoading = true, // [수정] 로딩 시작을 알림
                            errorMessage = "위치 정보를 불러오는 중입니다..."
                        )
                    }
                }
            }
        }
    }


    // 새로고침 함수
    fun refresh() {
        viewModelScope.launch {
            // 현재 state의 locationId를 가져옵니다. (observe~ 함수에 의해 갱신된 값)
            val currentLocationId = _uiState.value.locationId
            Log.d("ItemListViewModel", "refresh() 호출됨. LocationId: $currentLocationId")

            // 📍 위치 정보가 없으면 새로고침을 중단합니다.
            if (currentLocationId == null) {
                Log.w("ItemListViewModel", "위치 정보가 없어 refresh 중단.")
                _uiState.update { it.copy(isRefreshing = false, errorMessage = "위치 정보가 없습니다.") }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isRefreshing = true, // 새로고침 시작
                    items = emptyList(), // 기존 목록 초기화
                    currentPage = 0      // 페이지 번호 초기화
                )
            }
            try {
                // 'getItemsWithViewableUrls'에 'locationId' 파라미터 전달
                val refreshedItems = itemRepository.getItemsWithViewableUrls(
                    page = 0,
                    locationId = currentLocationId // locationId 전달 (Int로 자동 형변환)
                )
                _uiState.update {
                    it.copy(
                        isRefreshing = false, // 새로고침 완료
                        isLoading = false,    // [버그 수정] 로딩 상태 false로 변경
                        items = refreshedItems,
                        currentPage = 1, // 다음 페이지는 1
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false, // 새로고침 실패
                        isLoading = false,    // [버그 수정] 로딩 상태 false로 변경
                        errorMessage = "데이터를 새로고침하는데 실패했습니다."
                    )
                }
            }
        }
    }


    // loadItems (더 로드하기 - Pagination)
    fun loadMoreItems() {
        if (_uiState.value.isLoading || _uiState.value.isRefreshing) return

        viewModelScope.launch {
            val currentPage = _uiState.value.currentPage
            val currentLocationId = _uiState.value.locationId // 현재 state의 locationId 사용

            // 위치 정보가 없으면 로드를 중단합니다.
            if (currentLocationId == null) {
                Log.w("ItemListViewModel", "Skipping loadMoreItems, locationId is null.")
                _uiState.update { it.copy(isLoading = false) } // 로딩 상태 해제
                return@launch
            }

            Log.d(
                "ItemListViewModel",
                "Start loading more items for page: $currentPage with locationId: $currentLocationId"
            )

            _uiState.update { it.copy(isLoading = true) }
            try {
                // Repository 함수에 'locationId' 파라미터 전달
                val newItemsUiModel =
                    itemRepository.getItemsWithViewableUrls(
                        page = currentPage,
                        locationId = currentLocationId // locationId 전달 (Int로 자동 형변환)
                    )

                Log.d(
                    "ItemListViewModel",
                    "Successfully loaded ${newItemsUiModel.size} more items: $newItemsUiModel"
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = it.items + newItemsUiModel, // 기존 목록에 새 목록 추가
                        currentPage = it.currentPage + 1,
                        itemsImageUrl = it.itemsImageUrl
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "데이터를 불러오는데 실패했습니다.")
                }
            }
        }
    }


    // --- [함수 추가] ---
    // UI(드롭다운)에서 새 주소를 선택했을 때 호출될 함수
    fun onAddressSelected(selectedLocation: LocationUiModel) {
        viewModelScope.launch {
            // 1. (선택 사항) UserRepository를 통해 DataStore 등에 "기본 위치"를 업데이트
            //    userRepository.updateCurrentLocation(selectedLocation.id) // (이런 함수가 있다고 가정)

            // 2. UiState를 새 위치로 업데이트
            _uiState.update {
                it.copy(
                    locationId = selectedLocation.id,
                    locationName = selectedLocation.name
                )
            }

            // 3. 새 위치로 데이터 새로고침
            refresh()
        }
    }
}