package com.example.raon.features.item.ui.add

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raon.core.network.ApiResult
import com.example.raon.features.item.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddItemViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    savedStateHandle: SavedStateHandle // itemId를 받기 위해 주입
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddItemUiState())
    val uiState = _uiState.asStateFlow()

    // 수정할 아이템의 ID를 저장 (없으면 null -> 등록 모드)
    val itemId: Int? = savedStateHandle["itemId"]


    init {
        // 수정 모드인지 확인하고, 맞으면 기존 데이터를 불러옵니다.
        if (itemId != null && itemId != -1) { // -1은 NavArgument의 기본값이므로 제외
            //  [로그 추가 1] ViewModel이 생성될 때 어떤 모드인지 확인합니다.
            Log.d("AddItemViewModel", " ViewModel 초기화: 수정 모드입니다. Item ID: $itemId")
            loadItemForEditing(itemId)
        } else {
            //  [로그 추가 1] 등록 모드일 때도 로그를 남깁니다.
            Log.d("AddItemViewModel", " ViewModel 초기화: 새 상품 등록 모드입니다.")
        }
    }

    /**
     * [수정 모드]일 때 기존 상품 정보를 불러오는 함수
     */
    private fun loadItemForEditing(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Repository에서 상세 정보를 가져옵니다.
                val item = itemRepository.getItemDetail(id)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        title = item.title,
                        price = item.price.toString(),
                        description = item.description,
                        selectedCategoryName = item.category,
                        // TODO: 상세 모델에 categoryId가 포함되어야 정확한 ID를 설정할 수 있습니다.
                        // 주석을 풀고 item 모델에서 categoryId를 가져오도록 수정합니다.
                        selectedCategoryId = item.categoryId,
                        productCondition = when (item.condition) {
                            "새 상품" -> ProductCondition.NEW
                            else -> ProductCondition.USED
                        },
                        // 기존 이미지 URL을 UI State에 저장
                        existingImageUrls = item.imageUrls,
                        // 수정 모드에서는 불러온 데이터가 유효하다고 간주
                        isPriceValid = true,
                        isCategoryValid = true
                    )
                }
            } catch (e: Exception) {
                // TODO: 에러 처리 (예: Toast 메시지 이벤트 발생)
                _uiState.update { it.copy(isLoading = false, errorMessage = "상품 정보를 불러오지 못했습니다.") }
            }
        }
    }

    fun onEvent(event: AddItemEvent) {
        when (event) {
            is AddItemEvent.TitleChanged -> {
                _uiState.update { it.copy(title = event.title) }
            }

            is AddItemEvent.CategorySelected -> {
                _uiState.update {
                    it.copy(
                        isCategoryValid = true,
                        selectedCategoryId = event.id,
                        selectedCategoryName = event.name
                    )
                }
            }

            is AddItemEvent.ProductConditionChanged -> {
                _uiState.update { it.copy(productCondition = event.condition) }
            }

            is AddItemEvent.DescriptionChanged -> {
                _uiState.update { it.copy(description = event.description) }
            }

            is AddItemEvent.PriceChanged -> {
                val newPrice = event.price.filter { it.isDigit() }
                val isValid = newPrice.toLongOrNull()?.let { it >= 0 } ?: false
                _uiState.update {
                    it.copy(
                        price = newPrice,
                        isPriceValid = isValid
                    )
                }
            }

            // 기존 이미지 삭제 이벤트 처리 로직 추가
            is AddItemEvent.RemoveExistingImage -> {
                _uiState.update {
                    it.copy(
                        // 기존 이미지 목록에서 해당 URL 제거
                        existingImageUrls = it.existingImageUrls.filterNot { url -> url == event.url },
                        // 삭제된 이미지 목록에 추가
                        deletedImageUrls = it.deletedImageUrls + event.url
                    )
                }
            }

            is AddItemEvent.AddImages -> {
                _uiState.update {
                    val currentImages = it.seletedImages.toMutableList()
                    currentImages.addAll(event.uris)
                    val totalCount = it.existingImageUrls.size + currentImages.size
                    it.copy(seletedImages = if (totalCount > 5) currentImages.take(5 - it.existingImageUrls.size) else currentImages)
                }
            }

            is AddItemEvent.RemoveImage -> {
                _uiState.update {
                    val currentImages = it.seletedImages.toMutableList()
                    currentImages.remove(event.uri)
                    it.copy(seletedImages = currentImages)
                }
            }
            // TODO: 기존 이미지 삭제 이벤트 처리
            // is AddItemEvent.RemoveExistingImage -> { ... }
            is AddItemEvent.Submit -> submitItem()
        }
    }

    /**
     * '등록 완료' 또는 '수정 완료' 버튼 클릭 시 호출되는 함수
     */
    private fun submitItem() {
        // 수정된 부분: 중복 실행 방지 가드 강화
        // 1. 로딩 중이거나 2. 이미 성공했다면 함수를 즉시 종료
        if (_uiState.value.isLoading || _uiState.value.isSuccess) {
            Log.d(
                "AddItemViewModel",
                " 중복 제출 시도 감지 (isLoading=${_uiState.value.isLoading}, isSuccess=${_uiState.value.isSuccess}). 요청을 무시합니다."
            )
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentState = _uiState.value
            try {
                // itemId 유무에 따라 등록/수정 API를 분기하여 호출
                if (itemId != null && itemId != -1) {
                    // --- 수정 로직 ---
                    val result = itemRepository.updateItem(
                        itemId = itemId,
                        title = currentState.title,
//                        locationId = currentState.locationId,
                        description = currentState.description,
                        price = currentState.price.toInt(),
                        categoryId = currentState.selectedCategoryId,
                        condition = currentState.productCondition.name, // "NEW" or "USED"

                        // Repository에 이미지 관련 정보를 모두 넘겨줍니다.
                        newImageUris = currentState.seletedImages,
                        existingImageUrls = currentState.existingImageUrls

                    )
                    if (result is ApiResult.Success) {
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    } else {
                        // TODO: 에러 처리
                        Log.e("AddItemViewModel", "상품 수정 실패: $result")
                        _uiState.update { it.copy(isLoading = false) }
                    }
                } else {
                    // --- 등록 로직 ---
                    val response = itemRepository.postNewItem(
                        title = currentState.title,
                        description = currentState.description,
                        price = currentState.price.toInt(),
                        imageUris = currentState.seletedImages,
                        categoryId = currentState.selectedCategoryId,
                        condition = currentState.productCondition.name
                    )
                    if (response.code == "OK") {
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    } else {
                        // TODO: 에러 처리
                        Log.e("AddItemViewModel", "상품 등록 실패: ${response.message}")
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                // TODO: 네트워크 에러 등 예외 처리
                Log.e("AddItemViewModel", "작업 실패 (Exception)", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}