package com.example.raon.features.user.ui // SettingsViewModel과 같은 경로에 생성

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raon.features.auth.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
    private val authRepository: AuthRepository // 실제 사용하는 Repository로 변경
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
     * UI에서 회원탈퇴 버튼을 눌렀을 때 호출됩니다.
     */
    fun withdrawAccount() {
        // 동의하지 않았다면 에러 이벤트를 발생시키고 함수를 종료합니다.
        if (!_uiState.value.agreedToTerms) {
            viewModelScope.launch {
                _eventFlow.emit(WithdrawalEvent.ShowError("탈퇴 안내를 확인하고 동의해주세요."))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) } // 로딩 상태 시작
            try {

                // 토큰 삭제
                authRepository.logout()


                // TODO: 실제 회원탈퇴 API 호출 로직을 여기에 구현합니다.
                // val result = authRepository.withdraw()
                // if (result.isSuccess) {
                //     _eventFlow.emit(WithdrawalEvent.WithdrawalSuccess)
                // } else {
                //     _eventFlow.emit(WithdrawalEvent.ShowError("회원탈퇴에 실패했습니다."))
                // }

                // --- API 호출 테스트용 임시 코드 (실제 구현 시 삭제) ---
                delay(1500) // 1.5초간 로딩하는 척
                _eventFlow.emit(WithdrawalEvent.WithdrawalSuccess)
                // ---------------------------------------------------

            } catch (e: Exception) {
                _eventFlow.emit(WithdrawalEvent.ShowError(e.message ?: "알 수 없는 오류가 발생했습니다."))
            } finally {
                _uiState.update { it.copy(isLoading = false) } // 성공/실패 여부와 관계없이 로딩 종료
            }
        }
    }
}