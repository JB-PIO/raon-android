package com.example.raon.features.auth.ui.viewmodel


import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raon.features.auth.data.repository.AuthRepository
import com.example.raon.features.auth.ui.state.LoginResult // 👈 [수정] state 패키지에서 임포트
import com.example.raon.features.user.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 👈 [수정] ViewModel 내부에 있던 sealed class LoginResult {} 정의 삭제

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository

) : ViewModel() {

    // UI 상태 관리를 위한 변수들
    var email by mutableStateOf("test100@email.com")
        private set
    var password by mutableStateOf("test1234")
        private set
    var passwordVisible by mutableStateOf(false)
        private set


    private val _loginResult = MutableStateFlow<LoginResult>(LoginResult.Idle)
    val loginResult = _loginResult.asStateFlow()

    // 텍스트 필드 관련 함수들
    fun onEmailChange(newEmail: String) {
        email = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
    }

    // 로그인 관련 함수들
    fun login(email: String, password: String) {
        viewModelScope.launch {

            // 로그인 상태 변수
            _loginResult.value = LoginResult.Loading
            delay(1000)

            Log.d("LoginTest", "로그인 ViewModel 시작")

            // 1. [수정] 로그인 API 결과를 변수에 저장
            val result = authRepository.login(email, password)  // 로그인하기

            // 2. [수정] 로그인이 성공했는지 확인
            if (result is LoginResult.Success) {
                try {
                    // 3. [수정] UserRepository를 통해 프로필 정보 가져와서 DataStore에 저장
                    // 이 함수가 완료될 때까지 기다림 (suspend)
                    userRepository.fetchAndSaveUserProfile()
                    Log.d("LoginTest", "User Data 확인 : ${userRepository.getUserProfile()}")

                    // 4. [수정] 프로필 저장까지 모두 성공했을 때 최종 Success 상태로 변경
                    _loginResult.value = result
                    Log.d("LoginState", "로그인 상태 $result")

                } catch (e: Exception) {
                    // 3-1. [수정] 프로필 가져오기 실패 시
                    Log.e("LoginViewModel", "프로필 가져오기 실패: ${e.message}")
                    _loginResult.value = LoginResult.Failure("로그인은 성공했으나 프로필을 불러오지 못했습니다.")
                }
            } else {
                // 2-1. [수정] 로그인 자체가 실패한 경우
                _loginResult.value = result
                Log.d("LoginState", "로그인 상태 $result")
            }

            Log.d("LoginTest", "로그인 ViewModel 시작2")
        }
    }
}