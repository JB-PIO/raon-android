package com.example.raon.features.auth.ui.state

// 회원가입 화면의 모든 상태를 담는 단일 데이터 클래스
data class SignUpUiState(
    // (주석 처리된 테스트 값은 실제 값으로 대체합니다)
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

// [수정] LoginResult와 구조 통일 (Success는 object, ServerError 추가)
sealed class SignUpResult {
    object Idle : SignUpResult()
    object Loading : SignUpResult()
    object Success : SignUpResult() // 👈 object, 메시지 없음
    data class Failure(val message: String) : SignUpResult()
    data class ServerError(val message: String) : SignUpResult() // 👈 일관성을 위해 추가
}