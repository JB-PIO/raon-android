package com.example.raon.core.network

import android.util.Log
// import com.example.raon.features.auth.data.repository.AuthRepository // 👈 제거
import com.example.raon.features.auth.data.local.TokenManager // 👈 추가
// import kotlinx.coroutines.flow.first // 👈 제거
// import kotlinx.coroutines.runBlocking // 👈 제거
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    // AuthRepository 대신 TokenManager를 직접 주입받습니다.
    private val tokenManager: TokenManager // 👈 수정
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {

        // 1. "No-Authentication" 헤더가 있는지 확인
        val request = chain.request()
        val noAuthHeader = request.header("No-Authentication")

        // 2. 헤더가 있다면, 해당 헤더를 제거하고 그대로 요청을 진행 (토큰 추가 X)
        if (noAuthHeader != null) {
            val newRequest = request.newBuilder()
                .removeHeader("No-Authentication")
                .build()
            return chain.proceed(newRequest)
        }

        // 3. runBlocking 없이 동기적으로 토큰을 가져옵니다.
        val accessToken = tokenManager.getAccessToken() // 👈 수정

        Log.d("AuthInterceptor", "토큰: $accessToken")

        val newRequest = if (!accessToken.isNullOrBlank()) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
        } else {
            request
        }
        return chain.proceed(newRequest)
    }
}