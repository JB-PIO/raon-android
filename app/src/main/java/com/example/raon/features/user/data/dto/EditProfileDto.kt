package com.example.raon.features.user.data.dto


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 닉네임 변경 요청 DTO
 * (Postman 1번 이미지의 Body 참고)
 */
@JsonClass(generateAdapter = true)
data class UpdateNicknameRequest(
    @Json(name = "nickname") val nickname: String
)

/**
 * 프로필 이미지 변경 요청 DTO
 * (Postman 2번 이미지의 Body 참고)
 */
@JsonClass(generateAdapter = true)
data class UpdateProfileImageRequest(
    @Json(name = "profileImage") val profileImage: String
)

// 주소 변경 요청 dto
@JsonClass(generateAdapter = true)
data class UpdateUserLocation(
    @Json(name = "locationId") val locationId: Int
)

/**
 * 공통 응답 DTO (code, message 필드만 포함)
 * (Postman 응답 참고)
 */
@JsonClass(generateAdapter = true)
data class SimpleApiResponse(
    @Json(name = "code") val code: String,
    @Json(name = "message") val message: String
)