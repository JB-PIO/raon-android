package com.example.raon.features.user.data.remote

import com.example.raon.features.user.data.dto.ProfileResponse
import com.example.raon.features.user.data.dto.SimpleApiResponse
import com.example.raon.features.user.data.dto.UpdateNicknameRequest
import com.example.raon.features.user.data.dto.UpdateProfileImageRequest
import com.example.raon.features.user.data.dto.UpdateUserLocation
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface UserApiService {

    // 내 프로필 정보 조회
    @GET("api/v1/me")
    suspend fun getProfile(): Response<ProfileResponse>


    // 닉네임 변경
    @PATCH("api/v1/me/nickname")
    suspend fun updateNickname(
        @Body request: UpdateNicknameRequest
    ): Response<SimpleApiResponse>

    // 프로필 이미지 변경
    @PATCH("api/v1/me/profile-image")
    suspend fun updateProfileImage(
        @Body request: UpdateProfileImageRequest
    ): Response<SimpleApiResponse>


// ------------------ [이 부분 수정] ------------------
    /**
     * 사용자의 '위치'만 수정합니다.
     * Postman (image_f4ffe2.png)과 일치하도록 URL을 수정합니다.
     */
    @PATCH("api/v1/me/location") // 404 에러의 원인. "profile" -> "location"으로 수정
    suspend fun updateUserLocation(@Body request: UpdateUserLocation): Response<Unit>
    // -------------------------------------------------------------
}