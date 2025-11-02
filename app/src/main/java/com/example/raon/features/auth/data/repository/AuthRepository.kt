package com.example.raon.features.auth.data.repository

import android.util.Log
import com.example.raon.core.common.AppConstants
import com.example.raon.features.auth.data.local.TokenManager
import com.example.raon.features.auth.data.remote.api.AuthApiService
import com.example.raon.features.auth.data.remote.dto.LoginRequest
import com.example.raon.features.auth.data.remote.dto.SignUpRequest
import com.example.raon.features.auth.ui.state.LoginResult // 👈 [수정] state 패키지에서 임포트
import com.example.raon.features.auth.ui.state.SignUpResult // 👈 [수정] state 패키지에서 임포트
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.net.CookieManager
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val apiService: AuthApiService,
    private val tokenManager: TokenManager,
    private val cookieManager: CookieManager,
) {

    private var currentAccessToken: String? = null

    private val serverUri = java.net.URI.create(AppConstants.RAON_SERVER_URL) // 실제 앱 주소 사용

    init {
        currentAccessToken = tokenManager.getAccessToken()
        Log.d("AuthRepository", "초기 AccessToken 로드: $currentAccessToken")

        // ▼▼▼▼▼▼▼▼▼▼ [쿠키 복원 로직 - Secure 수정] ▼▼▼▼▼▼▼▼▼▼
        val persistentRefreshToken = tokenManager.getRefreshToken()
        if (persistentRefreshToken != null) {
            try {
                val cookie = java.net.HttpCookie("refreshToken", persistentRefreshToken).apply {
                    domain = serverUri.host
                    path = "/"
                    secure = true // [수정] https 이므로 true
                    isHttpOnly = true
                }
                cookieManager.cookieStore.add(serverUri, cookie)
                Log.i("AuthRepository", "[INIT] 🟢 영구 저장소의 RefreshToken을 CookieJar로 복원 성공")
            } catch (e: Exception) {
                Log.e("AuthRepository", "[INIT] 🔴 CookieJar 복원 중 예외 발생", e)
            }
        } else {
            Log.w("AuthRepository", "[INIT] 🟡 저장된 RefreshToken이 없어 복원할 수 없음.")
        }
    }

    fun getAccessTokenSync(): String? {
        return currentAccessToken
    }

    fun getAccessToken(): Flow<String?> = flow {
        emit(tokenManager.getAccessToken())
    }

    fun getRefreshToken(): Flow<String?> = flow {
        emit(tokenManager.getRefreshToken())
    }

    suspend fun login(email: String, password: String): LoginResult {
        return try {
            val request = LoginRequest(email, password)
            val response = apiService.login(request)

            when (response.code()) {
                200 -> {
                    if (response.body() != null) {
                        val loginResponse = response.body()!!
                        val accessToken = loginResponse.data.accessToken

                        if (loginResponse.code == "OK" && !accessToken.isNullOrBlank()) {
                            tokenManager.saveAccessToken(accessToken)
                            currentAccessToken = accessToken

                            val cookieHeaders = response.headers().values("Set-Cookie")
                            val cookieHeader = cookieHeaders.firstOrNull {
                                it.trim().startsWith("refreshToken=", ignoreCase = true)
                            }

                            if (cookieHeader != null) {
                                try {
                                    Log.d(
                                        "TokenDebug",
                                        "[Login] Raw Set-Cookie header: $cookieHeader"
                                    )

                                    val uri = serverUri // 상수 사용

                                    val existingCookies = cookieManager.cookieStore.get(uri)
                                    val badCookie =
                                        existingCookies.firstOrNull {
                                            it.name.equals(
                                                "refreshToken",
                                                ignoreCase = true
                                            )
                                        }
                                    if (badCookie != null) {
                                        cookieManager.cookieStore.remove(uri, badCookie)
                                    }

                                    val parsedCookies = java.net.HttpCookie.parse(cookieHeader)
                                    val refreshTokenCookie =
                                        parsedCookies.firstOrNull {
                                            it.name.equals(
                                                "refreshToken",
                                                ignoreCase = true
                                            )
                                        }

                                    if (refreshTokenCookie != null) {
                                        val fixedCookie =
                                            java.net.HttpCookie(
                                                refreshTokenCookie.name,
                                                refreshTokenCookie.value
                                            )
                                                .apply {
                                                    path = "/"
                                                    domain = uri.host
                                                    isHttpOnly = true
                                                    secure = true // [수정] https 이므로 true
                                                }

                                        // 1. CookieManager(메모리)에 저장
                                        cookieManager.cookieStore.add(uri, fixedCookie)

                                        // 2. TokenManager(영구저장소)에 값 저장
                                        tokenManager.saveRefreshToken(refreshTokenCookie.value)
                                        Log.i("TokenDebug", "[Login] 🟢 RefreshToken 영구 저장 성공.")

                                        Log.d(
                                            "TokenDebug",
                                            "[Login] Manually fixed and added cookie: ${fixedCookie} (Path=${fixedCookie.path}, Secure=${fixedCookie.secure})"
                                        )
                                    } else {
                                        Log.e(
                                            "TokenDebug",
                                            "[Login] 'refreshToken' cookie not found in header: $cookieHeader"
                                        )
                                    }

                                } catch (e: Exception) {
                                    Log.e("TokenDebug", "[Login] Failed to parse and fix cookie", e)
                                }
                            }

                            Log.d(
                                "TokenDebug",
                                "[Login] CookieJar 상태 (수동 저장 후): ${cookieManager.cookieStore.cookies}"
                            )

                            // 👈 [수정] object Success 반환 (메시지 삭제)
                            LoginResult.Success
                        } else {
                            LoginResult.Failure(loginResponse.message)
                        }
                    } else {
                        LoginResult.Failure("응답 바디가 비어있습니다.")
                    }
                }

                401 -> {
                    Log.d("LoginTest", "로그인 실패")
                    LoginResult.Failure("로그인 실패 (401 Unauthorized)")
                }

                else -> {
                    // 👈 [수정] ServerError 반환 (새 LoginResult 정의에 따름)
                    LoginResult.ServerError("알 수 없는 오류: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            Log.d("LoginTest", "서버 통신 실패 : ${e.message}")
            LoginResult.ServerError(e.message ?: "로그인 실패 (서버 응답 오류)")
        }
    }

    suspend fun signup(
        nickname: String,
        email: String,
        password: String,
        locationId: Int
    ): SignUpResult { // 👈 [수정] 반환 타입을 SignUpResult로 변경
        return try {
            val request = SignUpRequest(nickname, email, password, locationId)
            val response = apiService.signUp(request)

            when (response.code()) {
                200, 201 -> {
                    if (response.body() != null) {
                        val signUpResponse = response.body()!!
                        val accessToken = signUpResponse.data.accessToken

                        if (signUpResponse.code == "OK" && !accessToken.isNullOrBlank()) {
                            tokenManager.saveAccessToken(accessToken)
                            currentAccessToken = accessToken

                            val cookieHeaders = response.headers().values("Set-Cookie")
                            val cookieHeader = cookieHeaders.firstOrNull {
                                it.trim().startsWith("refreshToken=", ignoreCase = true)
                            }

                            if (cookieHeader != null) {
                                try {
                                    val uri = serverUri // 상수 사용

                                    val existingCookies = cookieManager.cookieStore.get(uri)
                                    val badCookie =
                                        existingCookies.firstOrNull {
                                            it.name.equals(
                                                "refreshToken",
                                                ignoreCase = true
                                            )
                                        }
                                    if (badCookie != null) {
                                        cookieManager.cookieStore.remove(uri, badCookie)
                                    }

                                    val parsedCookies = java.net.HttpCookie.parse(cookieHeader)
                                    val refreshTokenCookie =
                                        parsedCookies.firstOrNull {
                                            it.name.equals(
                                                "refreshToken",
                                                ignoreCase = true
                                            )
                                        }

                                    if (refreshTokenCookie != null) {
                                        val fixedCookie =
                                            java.net.HttpCookie(
                                                refreshTokenCookie.name,
                                                refreshTokenCookie.value
                                            )
                                                .apply {
                                                    path = "/"
                                                    domain = uri.host
                                                    isHttpOnly = true
                                                    secure = true // [수정] https 이므로 true
                                                }

                                        // 1. CookieManager(메모리)에 저장
                                        cookieManager.cookieStore.add(uri, fixedCookie)

                                        // 2. TokenManager(영구저장소)에 값 저장
                                        tokenManager.saveRefreshToken(refreshTokenCookie.value)
                                        Log.i("TokenDebug", "[Signup] 🟢 RefreshToken 영구 저장 성공.")

                                        Log.d(
                                            "TokenDebug",
                                            "[Signup] Manually fixed and added cookie: ${fixedCookie} (Path=${fixedCookie.path}, Secure=${fixedCookie.secure})"
                                        )
                                    } else {
                                        Log.e(
                                            "TokenDebug",
                                            "[Signup] 'refreshToken' cookie not found in header: $cookieHeader"
                                        )
                                    }

                                } catch (e: Exception) {
                                    Log.e(
                                        "TokenDebug",
                                        "[Signup] Failed to parse and fix cookie",
                                        e
                                    )
                                }
                            }
                            Log.d(
                                "TokenDebug",
                                "[Signup] CookieJar 상태 (수동 저장 후): ${cookieManager.cookieStore.cookies}"
                            )

                            // 👈 [수정] object Success 반환 (메시지 삭제)
                            SignUpResult.Success
                        } else {
                            SignUpResult.Failure(signUpResponse.message)
                        }
                    } else {
                        SignUpResult.Failure("회원가입 실패: 응답 바디가 비어있습니다.")
                    }
                }

                409 -> {
                    SignUpResult.Failure("이미 가입된_ 이메일입니다.")
                }

                else -> {
                    // 👈 [수정] ServerError 반환 (새 SignUpResult 정의에 따름)
                    SignUpResult.ServerError("회원가입 실패 (코드: ${response.code()})")
                }
            }
        } catch (e: Exception) {
            Log.e("SignupTest", "서버 통신 실패 : ${e.message}")
            SignUpResult.ServerError(e.message ?: "회원가입 중 오류가 발생했습니다.")
        }
    }

    fun logout() {
        Log.d("Logout_Test", "--- Access Token 삭제 전 ---")
        val accessTokenBefore = tokenManager.getAccessToken()
        Log.d("Logout_Test", "저장된 Access Token: $accessTokenBefore")

        tokenManager.clearTokens()
        currentAccessToken = null

        Log.d("Logout_Test", "--- Access Token 삭제 후 ---")
        val accessTokenAfter = tokenManager.getAccessToken()
        Log.d("Logout_Test", "남아있는 Access Token: $accessTokenAfter")

        Log.d("Logout_Test", "--- 쿠키(Refresh Token) 삭제 전 ---")
        val cookiesBefore = cookieManager.cookieStore.cookies
        Log.d("Logout_Test", "저장된 쿠키: $cookiesBefore")

        cookieManager.cookieStore.removeAll()

        Log.d("Logout_Test", "--- 쿠키(Refresh Token) 삭제 후 ---")
        val cookiesAfter = cookieManager.cookieStore.cookies
        Log.d("Logout_Test", "남아있는 쿠키: $cookiesAfter")

        Log.d("Logout_Test", "로그아웃 절차 완료.")


    }

    suspend fun refreshToken(): String? {
        Log.d("AuthRepository", "토큰 재발급 API 호출 시도...")

        // [수정됨] IO 스레드에서 동기 .execute()를 호출하도록 변경
        return withContext(Dispatchers.IO) {
            try {
                // [수정됨] apiService.refreshToken()은 Call 객체를 반환하므로 .execute() 호출
                val response = apiService.refreshToken().execute()

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.code == "OK" && apiResponse.data?.accessToken != null) {

                        val newAccessToken = apiResponse.data.accessToken
                        currentAccessToken = newAccessToken
                        tokenManager.saveAccessToken(newAccessToken)
                        Log.d("AuthRepository", "토큰 재발급 성공! 새 AccessToken 저장 완료.")

                        // ▼▼▼▼▼▼ [새 RefreshToken 덮어쓰기 로직 - Secure 수정] ▼▼▼▼▼▼
                        val cookieHeaders = response.headers().values("Set-Cookie")
                        val cookieHeader = cookieHeaders.firstOrNull {
                            it.trim().startsWith("refreshToken=", ignoreCase = true)
                        }

                        if (cookieHeader != null) {
                            try {
                                val uri = serverUri // 상수 사용
                                val parsedCookies = java.net.HttpCookie.parse(cookieHeader)
                                val refreshTokenCookie = parsedCookies.firstOrNull {
                                    it.name.equals("refreshToken", ignoreCase = true)
                                }

                                if (refreshTokenCookie != null) {
                                    val newRefreshTokenValue = refreshTokenCookie.value
                                    // 1. (중요!) 영구 저장소(SharedPreferences) 업데이트
                                    tokenManager.saveRefreshToken(newRefreshTokenValue)

                                    // 2. (안정성) 현재 CookieJar(메모리)도 새 값으로 덮어쓰기
                                    val fixedCookie =
                                        java.net.HttpCookie("refreshToken", newRefreshTokenValue)
                                            .apply {
                                                path = "/"
                                                domain = uri.host
                                                isHttpOnly = true
                                                secure = true // [수정] https 이므로 true
                                            }
                                    cookieManager.cookieStore.add(uri, fixedCookie)
                                    Log.i(
                                        "TokenDebug",
                                        "[Refresh] 🟢 새 RefreshToken 영구 저장/메모리 갱신 성공."
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e("TokenDebug", "[Refresh] 🔴 새 RefreshToken 파싱/저장 실패", e)
                            }
                        }
                        // ▲▲▲▲▲▲ [덮어쓰기 로직 끝] ▲▲▲▲▲▲

                        newAccessToken // 새 AccessToken 반환
                    } else {
                        Log.e("AuthRepository", "API 응답 실패: ${apiResponse.message}")
                        logout()
                        null
                    }
                } else {
                    // 401 (EXP_TOKEN 등) 또는 500 등 서버 오류
                    Log.e("AuthRepository", "HTTP 오류: ${response.code()} ${response.message()}")
                    logout() // 토큰이 유효하지 않으므로 로그아웃
                    null
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "토큰 재발급 중 예외 발생", e)
                logout()
                null
            }
        }
    }
}