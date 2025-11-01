package com.example.raon.features.item.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.raon.core.network.ApiResult
import com.example.raon.core.network.handleApi
import com.example.raon.core.network.repository.ImageStorageRepository
import com.example.raon.features.chat.data.remote.dto.CreateChatRoomResponseDto
import com.example.raon.features.chat.data.remote.dto.GetChatRoomResponseDto
import com.example.raon.features.item.data.remote.api.ItemApiService
import com.example.raon.features.item.data.remote.dto.add.ItemAddRequest
import com.example.raon.features.item.data.remote.dto.add.ItemResponse
import com.example.raon.features.item.data.remote.dto.add.ItemUpdateRequest
import com.example.raon.features.item.data.remote.dto.detail.ChangeFavoriteStatusRequest
import com.example.raon.features.item.data.remote.dto.detail.ItemDetailData
import com.example.raon.features.item.data.remote.dto.list.ItemDto
import com.example.raon.features.item.data.remote.dto.update_status.BuyerListResponseDto
import com.example.raon.features.item.data.remote.dto.update_status.UpdateStatusRequest
import com.example.raon.features.item.ui.detail.model.ItemDetailModel
import com.example.raon.features.item.ui.list.model.ItemUiModel
import com.example.raon.features.user.domain.repository.UserRepository
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepositoryImpl @Inject constructor(
    private val itemApiService: ItemApiService,
    private val imageStorageRepository: ImageStorageRepository, // S3에 업로드 하는 Repository
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ItemRepository {
    private val S3_BASE_URL0 = "https://raon-market-images-prod.s3.ap-northeast-2.amazonaws.com/"

    // itemList 가져오기
    override suspend fun getItems(
        page: Int,
        locationId: Int,
    ): List<ItemDto> {
        val response = itemApiService.getItems(
            page = page,
            locationId = locationId
        )
        if (response.isSuccessful) {
            return response.body()?.data?.items ?: emptyList()
        } else {
            throw Exception("API 호출 실패: ${response.code()}")
        }
    }

    // 가져온 itemList에서 이미지 키값으로 presignedURL 가져와서 이미지 받아오기
    override suspend fun getItemsWithViewableUrls(
        page: Int,
        locationId: Int,
    ): List<ItemUiModel> {
        // 1. 메인 서버에서 아이템 목록(DTO)을 가져오기
        val itemsDto = getItems(
            page = page,
            locationId = locationId
        )

        // 2. 각 아이템의 thumbnail(객체 키)로 Presigned URL을 병렬로 요청
        val itemsWithPresignedUrl = coroutineScope {
            val urlJobs = itemsDto.map { item ->
                async {

                    Log.d("itemRepository_url", "이미지 URL 확인 : ${item.thumbnail}")


                    item.thumbnail?.let { key ->

                        // S3_BASE_URL을 지우고 순수 이미지 이름만 파싱
                        val objectKey = key.removePrefix(S3_BASE_URL0)

                        Log.d("itemRepository_url", "이미지 URL 파싱 확인 : ${objectKey}")


                        // 파싱한 이미지 이름으로 presigned URL 요청
                        imageStorageRepository.getPresignedImageUrl(objectKey).getOrNull()
                    }
                }
            }
            val presignedUrls = urlJobs.awaitAll()

            Log.d("itemRepository_url", "이미지 presignedUrls 확인 : ${presignedUrls}")

            itemsDto.zip(presignedUrls) // (ItemDto, PresignedUrl) 쌍으로 묶기
        }
        // 3. 최종 UI 모델로 변환하여 반환
        return itemsWithPresignedUrl.map { (itemDto, presignedUrl) ->
            itemDto.toUiModel(presignedUrl)
        }
    }

    // [ New item 등록 ]
    override suspend fun postNewItem(
        title: String,
        description: String,
        price: Int,
        imageUris: List<Uri>,
        categoryId: Int?,
        condition: String
    ): ItemResponse {
        try {
            // 1. userRepository에서 Flow를 가져온 뒤 .first()를 호출해 최신 User 데이터를 꺼냅니다.
            val currentUser = userRepository.getUserProfile()
                .first()               //    이 작업은 비동기이므로 suspend 함수 내에서만 가능합니다.


            // 2. 데이터가 null일 수 있으므로 안전하게 처리합니다.
            //    사용자 정보가 있으면 그 사용자의 locationId를, 없으면 기본값(예: 435)을 사용합니다.
            val userLocationId = currentUser?.locationId ?: 1 // 예시: User 모델에 locationId가 있다고 가정

            Log.d("imageUpload", "imageUris : ${imageUris.size}")

            // presigned url 경로
            val imageUrls = uploadImagesAndGetS3Urls(imageUris)

            Log.d("imageUpload", "imageUrls : ${imageUrls}")

            // 앱에서 선택한 이미지 존재 && presigned url 경로 없을 때
            // -> 이미지를 업로드 하려다가 서버에서 실패한 상황
            if (imageUris.isNotEmpty() && imageUrls.isEmpty()) {
                return ItemResponse(
                    code = "IMAGE_UPLOAD_FAILED",
                    message = "이미지 업로드 실패",
                    data = null
                )
            }

            val itemRequest = ItemAddRequest(
                categoryId = categoryId,
                locationId = userLocationId,
                title = title,
                description = description,
                price = price,
                condition = condition,
                tradeType = "DIRECT",
                imageList = imageUrls
            )
            Log.d("imageUpload", "서버 저장 이미지 url : ${imageUrls}")


            // 아이템 올리기
            return itemApiService.postItem(itemRequest)

        } catch (e: HttpException) {
            Log.d("imageUpload", "HttpException : ${e.code()}")

            val errorBody = e.response()?.errorBody()?.string()
            return try {
                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                ItemResponse(
                    code = errorResponse.code,
                    message = errorResponse.message,
                    data = null
                )
            } catch (jsonE: Exception) {
                Log.d("imageUpload", "jsonE : ${jsonE.message}")

                ItemResponse(code = "PARSING_ERROR", message = "에러 응답 파싱 실패", data = null)
            }
        } catch (e: Exception) {
            Log.d("imageUpload", "Exception : ${e.message}")

            return ItemResponse(
                code = "UNKNOWN_ERROR",
                message = e.message ?: "알 수 없는 오류 발생",
                data = null
            )
        }
    }


    // [ Item 수정 ]
    override suspend fun updateItem(
        itemId: Int,
        title: String,
        description: String,
        price: Int,
        categoryId: Int?,
        condition: String,
        newImageUris: List<Uri>,
        existingImageUrls: List<String>
    ): ApiResult<Unit> {
        val currentUser = userRepository.getUserProfile().first()
        val userLocationId = currentUser?.locationId ?: 1

        val newImageUrls = if (newImageUris.isNotEmpty()) {
            uploadImagesAndGetS3Urls(newImageUris)
        } else {
            emptyList()
        }

        val cleanedExistingImageUrls = existingImageUrls.map { presignedUrl ->
            presignedUrl.substringBefore("?")
        }

        val finalImageUrls = cleanedExistingImageUrls + newImageUrls

        val request = ItemUpdateRequest(
            categoryId = categoryId,
            locationId = userLocationId,
            title = title,
            description = description,
            price = price,
            condition = condition,
            tradeType = "DIRECT",
            imageUrls = finalImageUrls
        )

        Log.e("AddItemViewModel_Repository", "request : ${request}")

        return handleApi { itemApiService.updateItem(itemId, request) }
    }


    // Aws Lambda 함수에 Presigned Url 요청 -> 이미지 업로드 -> 이미지 경로 가져오기
    private suspend fun uploadImagesAndGetS3Urls(imageUris: List<Uri>): List<String> =
        coroutineScope {
            if (imageUris.isEmpty()) return@coroutineScope emptyList()

            val uploadJobs = imageUris.map { uri ->
                async {
                    try {
                        val fileName =
                            "item-image-${System.currentTimeMillis()}-${uri.lastPathSegment}"

                        Log.d("imageUpload", "fileName : ${fileName}")

                        val presignedUrl =
                            imageStorageRepository.getPresignedUrl("item", fileName).getOrNull()

                        Log.d("imageUpload", "presignedUrl : ${presignedUrl}")


                        presignedUrl?.let {
                            val requestBody = context.contentResolver.openInputStream(uri)?.use {
                                it.readBytes().toRequestBody(
                                    context.contentResolver.getType(uri)?.toMediaTypeOrNull()
                                )
                            } ?: return@async null

                            Log.d("imageUpload", "presignedUrl : ${presignedUrl}")

                            val uploadResult = imageStorageRepository.uploadFile(it, requestBody)

                            Log.d("imageUpload", "이미지 업로드 결과 : ${uploadResult}")


                            if (uploadResult.isSuccess) {
                                it.substringBefore("?")
                            } else null
                        }
                    } catch (e: Exception) {
                        Log.d("imageUpload", "에러 : ${e.message}")
                        null
                    }
                }
            }
            uploadJobs.awaitAll().filterNotNull()
        }

    private fun ItemDto.toUiModel(presignedUrl: String?): ItemUiModel {
        return ItemUiModel(
            id = this.itemId,
            title = this.title,
            location = this.location.address,
            timeAgo = formatTimeAgo(this.createdAt),
            price = this.price,
            imageUrl = presignedUrl ?: "",
            likes = this.favoriteCount,
            comments = 0,
            viewCount = this.viewCount,
            status = this.status
        )
    }

    private fun formatTimeAgo(createdAt: String): String {
        return try {
            val createdTime = OffsetDateTime.parse(createdAt)
            val now = OffsetDateTime.now()

            val years = ChronoUnit.YEARS.between(createdTime, now)
            val months = ChronoUnit.MONTHS.between(createdTime, now)
            val days = ChronoUnit.DAYS.between(createdTime, now)
            val hours = ChronoUnit.HOURS.between(createdTime, now)
            val minutes = ChronoUnit.MINUTES.between(createdTime, now)

            when {
                years > 0 -> "${years}년 전"
                months > 0 -> "${months}달 전"
                days > 0 -> "${days}일 전"
                hours > 0 -> "${hours}시간 전"
                minutes > 0 -> "${minutes}분 전"
                else -> "방금 전"
            }
        } catch (e: Exception) {
            "시간 정보 없음"
        }
    }

    override suspend fun getItemDetail(itemId: Int): ItemDetailModel {
        val responseDto = itemApiService.getItemDetail(itemId)
        val itemData = responseDto.data

        val loggedInUserId = userRepository.getUserProfile().first()?.userId

        val isMine = (loggedInUserId != null) && (itemData.seller.userId == loggedInUserId)

        val viewableImageUrls = coroutineScope {
            val urlJobs = itemData.imageUrls.map { key ->
                async {
                    val objectKey =
                        key.removePrefix("https://raon-market-images-prod.s3.ap-northeast-2.amazonaws.com/")
                    imageStorageRepository.getPresignedImageUrl(objectKey).getOrNull()
                }
            }
            urlJobs.awaitAll().filterNotNull()
        }

        return itemData.toItemDetailModel(presignedUrls = viewableImageUrls, isMine)
    }

    private fun ItemDetailData.toItemDetailModel(
        presignedUrls: List<String>,
        isMine: Boolean
    ): ItemDetailModel {
        val productStatus = when (this.condition) {
            "NEW" -> "새 상품"
            "USED" -> "중고 상품"
            else -> "제품 상태"
        }

        return ItemDetailModel(
            id = this.productId,
            imageUrls = presignedUrls,
            sellerNickname = this.seller.nickname,
            sellerProfileUrl = this.seller.profileImage,
            sellerAddress = this.location.address,
            title = this.title,
            category = this.categories.joinToString(" > ") { it.name },
            categoryId = this.categories.lastOrNull()?.categoryId,
            createdAt = formatTimeAgo(this.createdAt),
            description = this.description,
            favoriteCount = this.favoriteCount,
            viewCount = this.viewCount,
            price = this.price,
            isFavorite = this.isFavorite,
            condition = productStatus,
            sellerId = seller.userId,
            isMine = isMine
        )
    }

    override suspend fun getChatRoomForItem(itemId: Long): ApiResult<GetChatRoomResponseDto> {
        return handleApi { itemApiService.getChatRoomForItem(itemId) }
    }

    override suspend fun createChatForItem(itemId: Long): ApiResult<CreateChatRoomResponseDto> {
        return handleApi { itemApiService.createChatForItem(itemId) }
    }

    override suspend fun increaseViewCount(itemId: Int) {
        try {
            val response = itemApiService.increaseViewCount(itemId)
            if (response.isSuccessful) {
                Log.d("ItemRepositoryImpl", "View count for item $itemId increased successfully.")
            } else {
                Log.e(
                    "ItemRepositoryImpl",
                    "Failed to increase view count for item $itemId. Code: ${response.code()}"
                )
            }
        } catch (e: Exception) {
            Log.e("ItemRepositoryImpl", "Error increasing view count for item $itemId", e)
        }
    }

    override suspend fun updateFavoriteStatus(itemId: Int, isFavorite: Boolean) {
        try {
            val request = ChangeFavoriteStatusRequest(isFavorite = isFavorite)
            val response = itemApiService.updateFavoriteStatus(itemId, request)
            if (!response.isSuccessful) {
                Log.e(
                    "ItemRepositoryImpl",
                    "Failed to update favorite status for item $itemId. Code: ${response.code()}"
                )
            }
        } catch (e: Exception) {
            Log.e("ItemRepositoryImpl", "Error updating favorite status for item $itemId", e)
            throw e
        }
    }

    override suspend fun deleteProduct(productId: Int): ApiResult<Unit> {
        return handleApi { itemApiService.deleteProduct(productId) }
    }

    override suspend fun getFavoriteStatus(productId: Int): Boolean {
        return itemApiService.getFavoriteStatus(productId).data.isFavorite
    }

    // ▼▼▼ [추가] 상품 상태 변경 함수 구현 ▼▼▼
    override suspend fun updateProductStatus(
        itemId: Int,
        newStatus: String,
        buyerId: Int?
    ): ApiResult<Unit> {
        val request = UpdateStatusRequest(status = newStatus, buyerId = buyerId)

        // ▼▼▼ [로그 추가 1] 서버로 '어떤 데이터'를 보내는지 정확히 확인 ▼▼▼
        // UpdateStatusRequest가 data class라면, $request가 (status=..., buyerId=...) 형태로 예쁘게 출력됩니다.
        Log.d(
            "ItemRepository",
            "Attempting updateProductStatus. itemId: $itemId, requestBody: $request"
        )

        // API 호출
        val result = handleApi { itemApiService.updateProductStatus(itemId, request) }

        // ▼▼▼ [로그 추가 2] API 호출 '결과'가 성공인지 실패인지 확인 ▼▼▼
        when (result) {
            is ApiResult.Success -> {
                Log.i("ItemRepository", "updateProductStatus SUCCESS for itemId: $itemId")
            }

            is ApiResult.Error -> {
                Log.e(
                    "ItemRepository",
                    "updateProductStatus ERROR for itemId: $itemId. Code: ${result.code}, Msg: ${result.errorBody?.message}"
                )
            }

            is ApiResult.Exception -> {
                Log.e(
                    "ItemRepository",
                    "updateProductStatus EXCEPTION for itemId: $itemId. Ex: ${result.e.message}"
                )
            }
        }

        return result
    }

    // ▼▼▼ [추가] 구매자 목록 조회 함수 구현 ▼▼▼
    override suspend fun getBuyersForProduct(itemId: Int): ApiResult<BuyerListResponseDto> {
        return handleApi { itemApiService.getBuyersForProduct(itemId) }
    }

}

// API 에러 응답 파싱용 DTO
data class ErrorResponse(
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("errors")
    val errors: Map<String, String>?
)