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
import kotlinx.coroutines.flow.first // 👈 'first' import 추가
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

    val locationId: Int? = null,

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
        // ViewModel이 시작될 때 DataStore에서 위치 정보를 먼저 가져옵니다.
        viewModelScope.launch {
            // getUserProfile() Flow에서 첫 번째 값(User 객체)을 가져옵니다.
            val userProfile = userRepository.getUserProfile().first()


            // userProfile로 필요한 데이터를 가져온 후에 첫 아이템 로드를 시작
            if (userProfile != null) {
                val userLocationId = userProfile?.locationId // User 객체에서 locationId를 추출합니다.

                // 가져온 locationId로 UiState를 업데이트합니다.
                _uiState.update { it.copy(locationId = userLocationId) }



                Log.d(
                    "ItemListViewModel",
                    "Initial locationId set to: $userLocationId. Loading items."
                )
                loadItems() // 첫 화면 데이터 가져오기
            } else {
                // 위치 정보가 없는 경우 (ex: 신규 유저)
                Log.w("ItemListViewModel", "locationId is null. User may need to set location.")
                _uiState.update { it.copy(errorMessage = "위치 정보가 없습니다. 프로필에서 위치를 설정해주세요.") }
            }
        }
    }


    // 새로고침 함수
    fun refresh() {
        viewModelScope.launch {
            // 현재 state의 locationId를 가져옵니다.
            val currentLocationId = _uiState.value.locationId

            // 📍 위치 정보가 없으면 새로고침을 중단합니다.
            if (currentLocationId == null) {
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
                    locationId = currentLocationId // locationId 전달
                )
                _uiState.update {
                    it.copy(
                        isRefreshing = false, // 새로고침 완료
                        items = refreshedItems,
                        currentPage = 1, // 다음 페이지는 1
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false, // 새로고침 실패
                        errorMessage = "데이터를 새로고침하는데 실패했습니다."
                    )
                }
            }
        }
    }


    // loadItems (더 로드하기 - Pagination)
    private fun loadItems() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            val currentPage = _uiState.value.currentPage
            val currentLocationId = _uiState.value.locationId // 현재 state의 locationId 사용

            // 📍 위치 정보가 없으면 로드를 중단합니다.
            if (currentLocationId == null) {
                Log.w("ItemListViewModel", "Skipping loadItems, locationId is null.")
                _uiState.update { it.copy(isLoading = false) } // 로딩 상태 해제
                return@launch
            }

            Log.d(
                "ItemListViewModel",
                "Start loading items for page: $currentPage with locationId: $currentLocationId"
            )

            _uiState.update { it.copy(isLoading = true) }
            try {
                // Repository 함수에 'locationId' 파라미터 전달
                val newItemsUiModel =
                    itemRepository.getItemsWithViewableUrls(
                        page = currentPage,
                        locationId = currentLocationId // locationId 전달
                    )

                Log.d(
                    "ItemListViewModel",
                    "Successfully loaded ${newItemsUiModel.size} items: $newItemsUiModel"
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = it.items + newItemsUiModel,
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