package com.example.raon

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.example.raon.features.auth.ui.z_etc.KakaoAuthViewModel
import com.example.raon.navigation.AppNavigation
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.util.Utility
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val kakaoAuthViewModel: KakaoAuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 키 해시값 가지기
        val keyHash = Utility.getKeyHash(this)
        Log.e("키 해시", keyHash.toString())

        // KakaoSdk 초기화
        KakaoSdk.init(this, BuildConfig.Kakao_native_App_Key)


        // ▼▼▼ 이 코드를 추가하세요 ▼▼▼
        // 앱이 시스템 창(상태바, 키보드 등)을 직접 처리하도록 설정합니다.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲


        // ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ
        enableEdgeToEdge()
        setContent {
            AppNavigation(modifier = Modifier)
        }
    }
}


