package com.example.raon.core.di

import android.util.Log
import com.example.raon.core.common.AppConstants
import com.example.raon.core.network.AuthInterceptor
import com.example.raon.core.network.TokenAuthenticator
import com.example.raon.core.network.api.ImageStorageService
import com.example.raon.features.auth.data.local.TokenManager
import com.example.raon.features.auth.data.remote.api.AuthApiService
import com.example.raon.features.category.data.remote.api.CategoryApiService
import com.example.raon.features.chat.data.remote.api.ChatApiService
import com.example.raon.features.item.data.remote.api.ItemApiService
import com.example.raon.features.location.data.remote.api.LocationApiService
import com.example.raon.features.profile.data.remote.ProfileApiService
import com.example.raon.features.search.data.remote.api.SearchApiService
import com.example.raon.features.user.data.remote.UserApiService
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CookieJar
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {


    // 잊지 말고 API Gateway ID를 꼭 수정 -> 바뀌면
    private const val AWS_API_GATEWAY_URL =
        "https://35w5qu5t6g.execute-api.ap-northeast-2.amazonaws.com"


    // =================================================================
    // MARK: - 공통 컴포넌트 제공
    // =================================================================

    // 1. [변경] CookieManager를 싱글톤으로 분리하여 제공합니다.
    // 이렇게 하면 다른 곳(AuthRepository)에서 주입받아 사용할 수 있습니다.
    @Provides
    @Singleton
    fun provideCookieManager(): CookieManager {
        return CookieManager().apply {
            setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        }
    }

    @Provides
    @Singleton
    fun provideCookieJar(cookieManager: CookieManager): CookieJar {
        return JavaNetCookieJar(cookieManager)
    }

    // AuthInterceptor는 Hilt로부터 TokenManager를 주입받습니다.
    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenManager: TokenManager // 👈 [이전 작업에서 수정 완료]
    ): AuthInterceptor {
        return AuthInterceptor(tokenManager) // 👈 [이전 작업에서 수정 완료]
    }


    // =================================================================
    // MARK: - 토큰 자동 갱신 (순환 참조 해결 구조)
    // =================================================================

    // 401 에러 시 토큰 자동 갱신 로직
    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        // authRepository: AuthRepository // 👈 [제거]
        authApiService: AuthApiService, // 👈 [추가]
        tokenManager: TokenManager    // 👈 [추가]
    ): TokenAuthenticator {
        // return TokenAuthenticator(authRepository) // 👈 [제거]
        return TokenAuthenticator(authApiService, tokenManager) // 👈 [수정]
    }

    // '토큰 갱신 전용' OkHttpClient (Authenticator가 없어 순환 참조를 방지)
    @Provides
    @Singleton
    @Named("RefreshClient")
    fun provideRefreshOkHttpClient(cookieJar: CookieJar): OkHttpClient {
        return OkHttpClient.Builder()
            // ▼▼▼▼▼▼▼▼▼▼ 이 코드로 수정하세요 ▼▼▼▼▼▼▼▼▼▼
            .addInterceptor(HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    Log.d("TokenDebug_Refresh", message)
                }
            }).apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            // ▲▲▲▲▲▲▲▲▲▲ 이 코드로 수정하세요 ▲▲▲▲▲▲▲▲▲▲
            .cookieJar(cookieJar)
            .build()
    }

    // '토큰 갱신 전용' Retrofit
    @Provides
    @Singleton
    @Named("RefreshRetrofit")
    fun provideRefreshRetrofit(@Named("RefreshClient") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AppConstants.RAON_SERVER_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    // =================================================================
    // MARK: - 메인 OkHttpClient 및 Retrofit 제공
    // =================================================================

    // '일반 API 호출용' 메인 OkHttpClient
    @Provides
    @Singleton
    @Named("MainClient")
    fun provideOkHttpClient(
        cookieJar: CookieJar,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()

            //  타임아웃 3줄이 추가
            .connectTimeout(360, TimeUnit.SECONDS)
            .readTimeout(360, TimeUnit.SECONDS)
            .writeTimeout(360, TimeUnit.SECONDS)

            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor(authInterceptor)
            .cookieJar(cookieJar)
            .authenticator(tokenAuthenticator) // 401 에러 시 작동
            .build()
    }

    // 라온마켓 서버용 Retrofit
    @Provides
    @Singleton
    @Named("RaonRetrofit")
    fun provideRaonRetrofit(@Named("MainClient") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AppConstants.RAON_SERVER_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 이미지 서버(AWS)용 Retrofit
    @Provides
    @Singleton
    @Named("ImageStorageRetrofit")
    fun provideImageStorageRetrofit(@Named("MainClient") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AWS_API_GATEWAY_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    // =================================================================
    // MARK: - API 서비스 제공
    // =================================================================

    // 인증 API (토큰 갱신 전용 Retrofit 사용)
    @Provides
    @Singleton
    fun provideAuthApiService(@Named("RefreshRetrofit") retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    // 아이템 API (라온마켓 서버용 Retrofit 사용)
    @Provides
    @Singleton
    fun provideItemApiService(@Named("RaonRetrofit") retrofit: Retrofit): ItemApiService {
        return retrofit.create(ItemApiService::class.java)
    }

    // 이미지 저장 API (이미지 서버용 Retrofit 사용)
    @Provides
    @Singleton
    fun provideImageStorageService(@Named("ImageStorageRetrofit") retrofit: Retrofit): ImageStorageService {
        return retrofit.create(ImageStorageService::class.java)
    }


    // 사용자 프로필 API (라온마켓 서버용 Retrofit 사용)
    @Provides
    @Singleton
    fun provideUserApiService(@Named("RaonRetrofit") retrofit: Retrofit): UserApiService {
        return retrofit.create(UserApiService::class.java)
    }


    // =================================================================
    // MARK: - 채팅 API 서비스
    // =================================================================


    @Provides
    @Singleton
    fun provideChatApiService(@Named("RaonRetrofit") retrofit: Retrofit): ChatApiService {
        return retrofit.create(ChatApiService::class.java)
    }


    // 수정된 코드 ✨
    @Provides
    @Singleton
    fun provideCategoryApiService(@Named("RaonRetrofit") retrofit: Retrofit): CategoryApiService {
        // "RaonRetrofit"이라는 이름표가 붙은 Retrofit을 달라고 명확히 요청
        return retrofit.create(CategoryApiService::class.java)
    }

    // 위치(주소) 검색 API (라온마켓 서버용 Retrofit 사용)
    @Provides
    @Singleton
    fun provideLocationApiService(@Named("RaonRetrofit") retrofit: Retrofit): LocationApiService {
        return retrofit.create(LocationApiService::class.java)
    }


    // 검색 API (라온마켓 서버용 Retrofit 사용)
    @Provides
    @Singleton
    fun provideSearchApiService(@Named("RaonRetrofit") retrofit: Retrofit): SearchApiService {
        return retrofit.create(SearchApiService::class.java)
    }

    // 2. 이 부분을 다른 ApiService 제공 함수들 사이에 추가하세요.
    @Provides
    @Singleton
    fun provideProfileApiService(@Named("RaonRetrofit") retrofit: Retrofit): ProfileApiService {
        return retrofit.create(ProfileApiService::class.java)
    }

    // ▼▼▼ [신규] 이 함수를 추가해 주세요 ▼▼▼
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

}