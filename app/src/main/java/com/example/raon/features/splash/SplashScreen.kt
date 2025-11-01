package com.example.raon.features.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.raon.core.model.LoginState
import com.example.raon.ui.theme.BrandYellow

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),   // SplashViewModel
    onNavigateToAuth: () -> Unit,   // 로그인 화면으로 이동하는 콜백 -> 토큰 없음
    onNavigateToMain: () -> Unit    // 메인 화면으로 이동하는 콜백 -> 토큰 있음
) {
    val loginState by viewModel.loginState.collectAsState()

    // loginState가 변경될 때마다 적절한 화면으로 이동
    LaunchedEffect(loginState) {
        when (loginState) {
            LoginState.LoggedIn -> onNavigateToMain()
            LoginState.LoggedOut -> onNavigateToAuth()
            LoginState.Loading -> { /* 로딩 중이므로 대기 */
            }
        }
    }

    // 로딩 중에 보여줄 UI (앱 로고 등을 넣으면 좋습니다)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = BrandYellow
        )
    }
}