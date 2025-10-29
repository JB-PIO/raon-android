package com.example.raon.features.auth.data.remote.api

import com.example.raon.core.network.dto.ApiResponse
import com.example.raon.features.auth.data.remote.dto.LoginRequest
import com.example.raon.features.auth.data.remote.dto.LoginResponse
import com.example.raon.features.auth.data.remote.dto.RefreshTokenResponse
import com.example.raon.features.auth.data.remote.dto.SignUpRequest
import com.example.raon.features.auth.data.remote.dto.SignUpResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 인증(Auth) 관련 API 명세를 정의하는 인터페이스 (주문서)
 */
interface AuthApiService {


    // 회원가입을 요청
    // @param request 닉네임, 이메일, 비밀번호가 담긴 요청 본문
    // @return 서버의 응답 (성공/실패 여부, 메시지 등)
    @POST("api/v1/auth/sign-up")
    suspend fun signUp(@Body request: SignUpRequest): Response<SignUpResponse>


    // 로그인을 요청
    // @param request 이메일, 비밀번호가 담긴 요청 본문
    // @return 서버의 응답 (성공/실패 여부, 토큰 정보 등)
    @POST("api/v1/auth/sign-in")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>


    // AccessToken 재발급 요청
    @POST("api/v1/auth/refresh")
//    suspend fun refreshToken(): ApiResponse<RefreshTokenResponse>
    suspend fun refreshToken(): Response<ApiResponse<RefreshTokenResponse>> // response로 감싸기


}