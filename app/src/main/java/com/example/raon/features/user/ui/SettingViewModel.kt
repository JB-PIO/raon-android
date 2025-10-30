package com.example.raon.features.user.ui


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raon.features.user.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
//    private val authRepository: AuthRepository,
    private val logoutUseCase: LogoutUseCase // <-- Repository 대신 UseCase 주입
) : ViewModel() {

    /**
     * 로그아웃을 수행하는 함수
     */
    fun logout() {
        viewModelScope.launch {
            // AuthRepository를 통해 토큰 삭제
//            authRepository.logout()

            logoutUseCase()
        }

    }
}