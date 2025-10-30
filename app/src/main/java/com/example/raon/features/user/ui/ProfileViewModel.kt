package com.example.raon.features.user.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raon.core.common.AppConstants
import com.example.raon.core.network.repository.ImageStorageRepository
import com.example.raon.features.user.domain.model.User
import com.example.raon.features.user.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

//  UI State 데이터 클래스 정의
data class ProfileUiState(
    val viewableProfileImageUrl: String? = null // Presigned URL 저장용
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val imageStorageRepository: ImageStorageRepository //  [7. ImageStorageRepository 주입 추가]
) : ViewModel() {

    //  [8. UI State Flow 정의]
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    // DataStore의 userProfileFlow를 관찰하여 UI에 제공할 StateFlow로 변환
    val userProfile: StateFlow<User?> = userRepository.getUserProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    //  [9. init 블록 추가 (Flow 감시 시작)]
    init {
        observeUserProfileAndLoadPresignedUrl()
    }


    //  [10. userProfile 변경 감지 및 Presigned URL 요청 로직 함수]
    private fun observeUserProfileAndLoadPresignedUrl() {
        userProfile
            .filterNotNull() // null이 아닌 User 객체가 올 때만 처리
            .onEach { user ->
                // 1. 저장된 사용자 정보에서 이미지 키 파싱
                val s3ImageKey = user.profileImage?.removePrefix(AppConstants.S3_BASE_URL)

                if (s3ImageKey != null && user.profileImage != AppConstants.DEFAULT_PROFILE_URL) {
                    // 2. S3 키가 있고 기본 URL이 아니면 Presigned URL 요청
                    try {
                        val presignedUrl =
                            imageStorageRepository.getPresignedImageUrl(s3ImageKey).getOrNull()
                        Log.d("ProfileViewModel", "✅ Presigned URL 획득: $presignedUrl")
                        // 3. UI State 업데이트
                        _uiState.update { it.copy(viewableProfileImageUrl = presignedUrl) }
                    } catch (e: Exception) {
                        Log.e("ProfileViewModel", "❌ Presigned URL 획득 실패", e)
                        _uiState.update { it.copy(viewableProfileImageUrl = null) } // 실패 시 null
                    }
                } else {
                    // 4. 기본 프로필이거나 이미지 URL 없으면 null
                    _uiState.update { it.copy(viewableProfileImageUrl = null) }
                }
            }
            .launchIn(viewModelScope) // viewModelScope 내에서 Flow 감시 시작
    }
}