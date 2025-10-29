package com.example.raon.features.auth.ui.state

// 회원가입 화면의 모든 상태를 담는 단일 데이터 클래스
data class SignUpUiState(
    // 1. 사용자가 입력하는 값들
//    val nickname: String = "test",
//    val email: String = "test@email.com",
//    val password: String = "test1234",
//    val passwordCheck: String = "test1234",


    val nickname: String = "",
    val email: String = "",
    val password: String = "",
    val passwordCheck: String = "",


    val userLocation: String = "",
    val userLocationId: Int = -1,


    // 2. UI의 상태를 나타내는 값들
    val isPasswordVisible: Boolean = false,
    val isPasswordCheckVisible: Boolean = false,

    // 3. 비동기 작업의 결과를 나타내는 값 (Sealed Class 활용)
    val signUpResult: SignUpResult = SignUpResult.Idle
)

// 회원가입 '결과'만을 위한 Sealed Class
sealed class SignUpResult {
    object Idle : SignUpResult()
    object Loading : SignUpResult()
    object Success : SignUpResult()
    data class Failure(val message: String) : SignUpResult()
}