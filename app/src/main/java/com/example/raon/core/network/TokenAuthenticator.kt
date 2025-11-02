package com.example.raon.core.network

import android.util.Log
import com.example.raon.core.network.dto.ApiResponse
import com.example.raon.features.auth.data.local.TokenManager
import com.example.raon.features.auth.data.remote.api.AuthApiService
import com.example.raon.features.auth.data.remote.dto.RefreshTokenResponse
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Call
import javax.inject.Inject

class TokenAuthenticator @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        Log.d("TokenAuthenticator", "Authenticator 호출됨! 401 에러 감지.")

        // 1. 재시도 횟수 확인 (기존 로직 유지)
        if (responseCount(response) >= 2) {
            Log.d("TokenAuthenticator", "재시도 횟수 초과. 갱신을 중단합니다.")
            return null
        }

        // 2. 동기화 블럭 (기존 로직 유지)
        synchronized(this) {
            val currentAccessToken = tokenManager.getAccessToken()
            val failedRequestToken =
                response.request.header("Authorization")?.substringAfter("Bearer ")

            // 3. 다른 스레드가 이미 갱신했는지 확인 (기존 로직 유지)
            if (failedRequestToken != null && currentAccessToken != failedRequestToken) {
                Log.d("TokenAuthenticator", "다른 스레드가 이미 토큰을 갱신함. 새 토큰으로 바로 재시도.")
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            Log.d("TokenAuthenticator", "토큰 갱신을 시도합니다...")

            val tokenCall: Call<ApiResponse<RefreshTokenResponse>> = authApiService.refreshToken()
            val tokenResponse: retrofit2.Response<ApiResponse<RefreshTokenResponse>>

            try {
                // 동기적으로 네트워크 호출 실행
                tokenResponse = tokenCall.execute()
            } catch (e: Exception) {
                Log.e("TokenAuthenticator", "토큰 갱신 중 네트워크 예외 발생", e)
                tokenManager.clearTokens() // 로그아웃 처리
                return null // 갱신 실패
            }

            // 4. 응답 결과 처리
            val responseBody = tokenResponse.body()

            // [수정] "SUCCESS"가 아닌 "OK"로 변경
            if (tokenResponse.isSuccessful && responseBody != null && responseBody.code == "OK") {

                val newData = responseBody.data

                if (newData != null && newData.accessToken != null) {
                    val newAccessToken = newData.accessToken

                    // accessToken만 TokenManager에 저장
                    tokenManager.saveAccessToken(newAccessToken)

                    Log.d("TokenAuthenticator", "토큰 갱신 성공! 새 AccessToken 저장. 재시도합니다.")
                    // 새 토큰으로 헤더를 교체하여 요청 재시도
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .build()
                } else {
                    // 성공(200)은 했으나 data 필드나 accessToken이 null로 온 경우
                    Log.e("TokenAuthenticator", "토큰 갱신 실패: 응답 body의 data 또는 accessToken이 null입니다.")
                    tokenManager.clearTokens()
                    return null
                }
            } else {
                Log.e(
                    "TokenAuthenticator",
                    "토큰 갱신 실패: API가 실패를 반환했습니다. (HTTP Code: ${tokenResponse.code()}, Server Code: ${responseBody?.code})"
                )
                tokenManager.clearTokens() // 로그아웃 처리
                return null // 갱신 실패
            }
        }
    }

    /**
     * 재시도 횟수를 확인하는 헬퍼 함수 (기존 로직 유지)
     */
    private fun responseCount(response: Response): Int {
        var result = 1
        var currentResponse = response.priorResponse
        while (currentResponse != null) {
            result++
            currentResponse = currentResponse.priorResponse
        }
        return result
    }
}