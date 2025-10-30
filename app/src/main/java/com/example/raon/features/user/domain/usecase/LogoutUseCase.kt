package com.example.raon.features.user.domain.usecase

import com.example.raon.features.auth.data.repository.AuthRepository
import com.example.raon.features.user.domain.repository.UserRepository
// import com.example.raon.features.chat.domain.repository.ChatRepository // (만약 채팅 캐시도 지워야 한다면)
import javax.inject.Inject

/**
 * 로그아웃 처리를 조율하는 UseCase
 * 1. 인증 토큰(Auth) 삭제
 * 2. 로컬 유저 정보(User) 삭제
 * 3. (필요시) 기타 캐시 삭제
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
    // private val chatRepository: ChatRepository // (예시)
) {
    suspend operator fun invoke() {
        // 1. 인증 정보 삭제 (토큰, 쿠키)
        // (AuthRepository의 logout()은 이제 토큰만 지우도록 수정되어야 함)
        authRepository.logout()

        // 2. 사용자 프로필 캐시(DataStore) 삭제
        userRepository.clearUserProfileData()

        // 3. (필요시) 다른 모든 로컬 데이터 삭제
        // chatRepository.clearChatCache()
    }
}