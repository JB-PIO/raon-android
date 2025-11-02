package com.example.raon.features.user.domain.repository

import com.example.raon.core.network.ApiResult
import com.example.raon.features.user.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    // DataStore에서 사용자 프로필을 Flow 형태로 가져옴
    fun getUserProfile(): Flow<User?>

    // 서버에서 프로필을 가져와 DataStore에 업데이트
    suspend fun fetchAndSaveUserProfile(): ApiResult<Unit>


    // [추가] 닉네임 업데이트
    suspend fun updateNickname(nickname: String): ApiResult<Unit>

    // [추가] 프로필 이미지 URL 업데이트 (파일이 아닌 URL을 받음)
    suspend fun updateProfileImage(imageUrl: String): ApiResult<Unit>


    suspend fun clearUserProfileData() // DataStore 초기화 함수 추가

    // ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ

    // ---------------- [추가된 코드] ----------------
    /**
     * API를 호출하여 사용자의 '메인 위치'를 수정합니다.
     */
    suspend fun editMyLocation(locationId: Int, locationName: String): ApiResult<Unit>

    /**
     * DataStore에서 '즐겨찾기 위치' 목록을 Flow로 가져옵니다.
     */
    fun getFavoriteLocations(): Flow<List<Pair<Int, String>>>

    /**
     * DataStore에 '즐겨찾기 위치'를 추가합니다.
     */
    suspend fun addFavoriteLocation(id: Int, name: String)
    // ---------------------------------------------


    /**
     * 서버에 로그아웃을 요청합니다.
     * (기존 withdrawAccount에서 이름 변경)
     */
    suspend fun signOut(): ApiResult<Unit> // <--- 함수명 변경
    // -------------------------------------------------


    /**
     * 서버에 회원탈퇴(계정 삭제)를 요청하고,
     * 성공 시 로컬 DataStore의 사용자 정보도 삭제합니다.
     */
    suspend fun deleteAccount(): ApiResult<Unit>
    // -------------------------------------------------

}