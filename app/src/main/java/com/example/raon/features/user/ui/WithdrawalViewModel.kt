package com.example.raon.features.user.ui // SettingsScreen과 같은 경로

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raon.core.network.ApiResult
import com.example.raon.features.user.domain.repository.UserRepository
import com.example.raon.features.user.domain.usecase.LogoutUseCase // AuthRepository 대신 UseCase 사용
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 회원탈퇴 화면의 상태를 정의하는 데이터 클래스
data class WithdrawalUiState(
    val agreedToTerms: Boolean = false, // 탈퇴 동의 체크박스 상태
    val isLoading: Boolean = false,     // API 요청 중 로딩 상태
)

// 회원탈퇴 과정에서 발생하는 단발성 이벤트를 정의
sealed class WithdrawalEvent {
    data class ShowError(val message: String) : WithdrawalEvent()
    object WithdrawalSuccess : WithdrawalEvent()
}

@HiltViewModel
class WithdrawalViewModel @Inject constructor(
    private val userRepository: UserRepository, // 서버 탈퇴 API 호출용
    private val logoutUseCase: LogoutUseCase      // 로컬 토큰 삭제용
) : ViewModel() {

    private val _uiState = MutableStateFlow(WithdrawalUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<WithdrawalEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    /**
     * UI에서 탈퇴 동의 체크박스 상태가 변경될 때 호출됩니다.
     */
    fun onAgreementChanged(isChecked: Boolean) {
        _uiState.update { it.copy(agreedToTerms = isChecked) }
    }

    /**
     * UI에서 회원탈TAE 버튼을 눌렀을 때 호출됩니다.
     * "진짜" 회원탈퇴 로직으로 변경
     */
    fun withdrawAccount() {
        // 동의 체크 확인
        if (!_uiState.value.agreedToTerms) {
            viewModelScope.launch {
                _eventFlow.emit(WithdrawalEvent.ShowError("탈퇴 안내를 확인하고 동의해주세요."))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) } // 로딩 시작
            try {

                // 1. 서버에 회원탈퇴 API 호출 ("deleteAccount" 호출)
                val result = userRepository.deleteAccount()

                if (result is ApiResult.Success) {
                    // 2. 서버 탈퇴 성공 시, 로컬 토큰/데이터도 완전 삭제 (로그아웃)
                    logoutUseCase()

                    // 3. 화면에 성공 이벤트 전달
                    _eventFlow.emit(WithdrawalEvent.WithdrawalSuccess)

                } else {
                    // 4. 서버 탈퇴 실패 시 에러 메시지 전달
                    val errorMessage = when (result) {
                        is ApiResult.Error -> result.errorBody?.message ?: "회원탈퇴에 실패했습니다."
                        is ApiResult.Exception -> "네트워크 오류가 발생했습니다."
                        else -> "알 수 없는 오류가 발생했습니다." // Success 외 모든 경우
                    }
                    _eventFlow.emit(WithdrawalEvent.ShowError(errorMessage))
                }

            } catch (e: Exception) {
                // 5. 알 수 없는 예외 처리
                _eventFlow.emit(WithdrawalEvent.ShowError(e.message ?: "알 수 없는 오류가 발생했습니다."))
            } finally {
                // 6. 성공/실패 여부와 관계없이 로딩 종료
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}