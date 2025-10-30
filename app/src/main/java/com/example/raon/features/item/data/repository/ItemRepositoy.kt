package com.example.raon.features.item.data.repository

import android.net.Uri
import com.example.raon.core.network.ApiResult
import com.example.raon.features.chat.data.remote.dto.CreateChatRoomResponseDto
import com.example.raon.features.chat.data.remote.dto.GetChatRoomResponseDto
import com.example.raon.features.item.data.remote.dto.add.ItemResponse
import com.example.raon.features.item.data.remote.dto.list.ItemDto
import com.example.raon.features.item.data.remote.dto.update_status.BuyerListResponseDto
import com.example.raon.features.item.ui.detail.model.ItemDetailModel
import com.example.raon.features.item.ui.list.model.ItemUiModel

// 아이템 데이터 처리를 위한 인터페이스
interface ItemRepository {

    // ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ 게시글 리스트, 게시글 CURD ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ


    // --- Presigned URL을 포함한 UI 모델을 반환하는 새 함수 추가 ---
    suspend fun getItemsWithViewableUrls(
        page: Int,
        locationId: Int,
    ): List<ItemUiModel>

    // 아이템 목록 조회. 실패 시 예외를 던짐
    suspend fun getItems(
        page: Int,
        locationId: Int,
    ): List<ItemDto> // 이름 및 반환 타입 변경

    // [ New Item 등록 }
    suspend fun postNewItem(
        title: String,
        description: String,
        price: Int,
        imageUris: List<Uri>,
        categoryId: Int?,
        condition: String,
    ): ItemResponse

    // [ Item 수정 ]
    suspend fun updateItem(
        itemId: Int,
        title: String,
//        locationId: Int,
        description: String,
        price: Int,
        // TODO: 이미지 수정 로직 구현 시 파라미터 변경 필요
        // imageUris: List<Uri>,
        categoryId: Int?,
        condition: String,
        newImageUris: List<Uri>,
        existingImageUrls: List<String>
    ): ApiResult<Unit>


    // 상세 페이지 모델을 가져오는 함수 정의
    suspend fun getItemDetail(itemId: Int): ItemDetailModel

    // [ Item 삭제 ]
    suspend fun deleteProduct(productId: Int): ApiResult<Unit>


    // ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ 채팅관련 ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ

    // 채팅방 id를 가져오는 함수 -> ApiResult를 사용해서 HTTP 200 code 일때만 Repository에서 DTO를 받아서 처리
    // 채팅방이 있는지 확인하는 함수
    suspend fun getChatRoomForItem(itemId: Long): ApiResult<GetChatRoomResponseDto>

    // 2. 채팅방이 없을 때 생성하는 함수
    suspend fun createChatForItem(itemId: Long): ApiResult<CreateChatRoomResponseDto>

    // ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ


    // Item 조회수 증가 함수
    suspend fun increaseViewCount(itemId: Int)

    // Item 찜(관심상품) 상태 변경 함수
    suspend fun updateFavoriteStatus(itemId: Int, isFavorite: Boolean)

    // 찜 상태 조회 함수 추가 (Boolean을 직접 반환하도록 단순화)
    suspend fun getFavoriteStatus(productId: Int): Boolean


    // ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ


    // [추가] 상품 상태 변경 함수
    suspend fun updateProductStatus(
        itemId: Int,
        newStatus: String,
        buyerId: Int? = null
    ): ApiResult<Unit>

    // [추가] 구매자 목록 조회 함수
    suspend fun getBuyersForProduct(itemId: Int): ApiResult<BuyerListResponseDto> // TODO: 실제 응답 타입으로 변경


}