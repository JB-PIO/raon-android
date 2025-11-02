package com.example.raon.features.auth.ui.state

// SignUpResult와 구조를 통일 (Success가 object)
sealed class LoginResult {
    object Idle : LoginResult()
    object Loading : LoginResult()
    object Success : LoginResult() // 👈 [수정] data class -> object
    data class Failure(val message: String) : LoginResult()
    data class ServerError(val message: String) : LoginResult()

    // 👈 [수정] LoginViewModel에서 사용하지 않는 Error 클래스 제거 (필요시 추가)
}