package com.example.raon.features.user.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raon.core.network.ApiResult // [추가]
import com.example.raon.features.user.domain.repository.UserRepository // [추가]
import com.example.raon.features.user.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow // [추가]
import kotlinx.coroutines.flow.MutableStateFlow // [추가]
import kotlinx.coroutines.flow.asSharedFlow // [추가]
import kotlinx.coroutines.flow.asStateFlow // [추가]
import kotlinx.coroutines.launch
import javax.inject.Inject

// 1. UI 피드백을 위한 이벤트를 정의합니다.
sealed class SettingsEvent {
    object LogoutSuccess : SettingsEvent()
    data class ShowError(val message: String) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase, // 로컬 토큰 삭제(UseCase)
    private val userRepository: UserRepository // 2. 서버 로그아웃 API 호출용 (추가)
) : ViewModel() {

    // 3. UI가 관찰할 로딩 상태와 이벤트를 만듭니다.
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SettingsEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    /**
     * [수정] 로그아웃을 수행하는 함수
     * 1. 서버에 로그아웃 요청
     * 2. 성공 시 로컬 로그아웃(토큰 삭제) 수행
     */
    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true // 로딩 시작
            try {
                // 1. 서버에 로그아웃 요청 (우리가 UserRepo에 만든 함수)
                val result = userRepository.signOut()

                if (result is ApiResult.Success) {
                    // 2. 서버 로그아웃 성공 시, 로컬 토큰 삭제
                    logoutUseCase()

                    // 3. UI에 성공 이벤트 전달
                    _eventFlow.emit(SettingsEvent.LogoutSuccess)

                } else {
                    // 4. 서버 로그아웃 실패 시, 에러 이벤트 전달
                    val errorMessage =
                        (result as? ApiResult.Error)?.errorBody?.message ?: "로그아웃에 실패했습니다."
                    _eventFlow.emit(SettingsEvent.ShowError(errorMessage))
                }

            } catch (e: Exception) {
                // 5. 네트워크 예외 등 처리
                _eventFlow.emit(SettingsEvent.ShowError(e.message ?: "알 수 없는 오류가 발생했습니다."))
            } finally {
                _isLoading.value = false // 로딩 종료
            }
        }
    }
}