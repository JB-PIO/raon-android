package com.example.raon.features.auth.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raon.features.auth.data.repository.AuthRepository
import com.example.raon.features.auth.ui.state.SignUpResult // 👈 [수정] state 패키지에서 임포트
import com.example.raon.features.auth.ui.state.SignUpUiState
import com.example.raon.features.user.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 👈 ViewModel 내부에 있던 sealed class SignUpResult {} 정의 삭제됨

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository, // 👈 UserRepository 주입
    savedStateHandle: SavedStateHandle

) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState = _uiState.asStateFlow()


    init {
        val address: String? = savedStateHandle["location"]
        val locationId: Int = savedStateHandle.get<Int>("locationId") ?: -1

        if (address != null && locationId != -1) {
            _uiState.update {
                it.copy(
                    userLocation = address,
                    userLocationId = locationId
                )
            }
        }
    }

    // --- UI 이벤트를 처리하는 함수들 ---
    fun onNicknameChange(nickname: String) {
        _uiState.update { it.copy(nickname = nickname) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun onPasswordCheckChange(passwordCheck: String) {
        _uiState.update { it.copy(passwordCheck = passwordCheck) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun togglePasswordCheckVisibility() {
        _uiState.update { it.copy(isPasswordCheckVisible = !it.isPasswordCheckVisible) }
    }


    fun signUp() {
        if (_uiState.value.signUpResult == SignUpResult.Loading) return

        // --- ✨ [수정] 유효성 검사 순서 및 내용 강화 ---
        val currentState = _uiState.value

        // 1. 빈칸 검사
        if (currentState.nickname.isBlank() ||
            currentState.email.isBlank() ||
            currentState.password.isBlank() ||
            currentState.passwordCheck.isBlank() ||
            currentState.userLocation.isBlank() ||
            currentState.userLocationId == -1
        ) {
            _uiState.update {
                it.copy(signUpResult = SignUpResult.Failure("모든 항목을 입력해주세요."))
            }
            return
        }

        // ✨ [추가] 닉네임 유효성 검사 (예: 2자 이상)
        if (currentState.nickname.length < 2) {
            _uiState.update {
                it.copy(signUpResult = SignUpResult.Failure("닉네임은 2자 이상 입력해주세요."))
            }
            return
        }

        // ✨ [추가] 이메일 형식 유효성 검사
        // (안드로이드 SDK에 내장된 이메일 패턴 사용)
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(currentState.email).matches()) {
            _uiState.update {
                it.copy(signUpResult = SignUpResult.Failure("이메일 형식이 올바르지 않습니다."))
            }
            return
        }

        // ✨ [추가] 비밀번호 유효성 검사 (예: 8자 이상)
        if (currentState.password.length < 8) {
            _uiState.update {
                it.copy(signUpResult = SignUpResult.Failure("비밀번호는 8자 이상 입력해주세요."))
            }
            return
        }

        // 2. 비밀번호 일치 여부 검사 (기존과 동일)
        if (currentState.password != currentState.passwordCheck) {
            _uiState.update {
                it.copy(signUpResult = SignUpResult.Failure("비밀번호가 일치하지 않습니다."))
            }
            return
        }

        // 3. 회원가입 절차 진행
        viewModelScope.launch {
            _uiState.update { it.copy(signUpResult = SignUpResult.Loading) }

            // 1. [수정] 회원가입 repository 실행
            val signupApiResult = authRepository.signup(
                currentState.nickname,
                currentState.email,
                currentState.password,
                locationId = currentState.userLocationId
            )

            // 2. [수정] 회원가입 API가 성공했는지 확인
            if (signupApiResult is SignUpResult.Success) {
                try {
                    // 3. [수정] 프로필 정보를 가져와 DataStore에 저장 (완료될 때까지 기다림)
                    userRepository.fetchAndSaveUserProfile()

                    // 4. [수정] 프로필 저장까지 성공했을 때 최종 Success 상태로 변경
                    delay(2000) // (기존 코드에 있던 가상 네트워크 딜레이)
                    _uiState.update { it.copy(signUpResult = signupApiResult) }

                } catch (e: Exception) {
                    // 3-1. [수정] 프로필 가져오기 실패 시
                    _uiState.update {
                        it.copy(signUpResult = SignUpResult.Failure("회원가입은 성공했으나 프로필을 불러오지 못했습니다: ${e.message}"))
                    }
                }
            } else {
                // 2-1. [수정] 회원가입 자체가 실패한 경우 (signupApiResult가 Failure 등일 때)
                _uiState.update { it.copy(signUpResult = signupApiResult) }
            }
        }
    }


    // SignUpResult를 다시 초기화 시키는 코드
    fun resultConsumed() {
        _uiState.update { it.copy(signUpResult = SignUpResult.Idle) }
    }
}