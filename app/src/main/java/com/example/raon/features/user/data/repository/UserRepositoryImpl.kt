package com.example.raon.features.user.data.repository


import android.util.Log
import com.example.raon.core.network.ApiResult
import com.example.raon.core.network.handleApi
import com.example.raon.features.user.data.dto.UpdateNicknameRequest
import com.example.raon.features.user.data.dto.UpdateProfileImageRequest
import com.example.raon.features.user.data.dto.UpdateUserLocation
import com.example.raon.features.user.data.local.UserDataStore
import com.example.raon.features.user.data.remote.UserApiService
import com.example.raon.features.user.domain.model.User
import com.example.raon.features.user.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userApiService: UserApiService,
    private val userDataStore: UserDataStore
) : UserRepository {

    // DataStore에서 Profile 데이터 가져오기
    override fun getUserProfile(): Flow<User?> {
        return userDataStore.userProfileFlow
    }

    override suspend fun fetchAndSaveUserProfile(): ApiResult<Unit> {
        return handleApi { userApiService.getProfile() }.onSuccess { response ->
            // API 호출 성공 시 DTO를 Domain Model로 변환
            val user = User(
                userId = response.data.userId,
                nickname = response.data.nickname,
                email = response.data.email,
                profileImage = response.data.profileImage,
                address = response.data.location.address,
                locationId = response.data.location.locationId
            )

            Log.d("UserData", "UserData 확인1 : ${response.data.userId},")
            Log.d("UserData", "UserData 확인2 : ${response.data.nickname},")
            Log.d("UserData", "UserData 확인3 : ${response.data.email},")
            Log.d("UserData", "UserData 확인4 : ${response.data.profileImage},")
            Log.d("UserData", "UserData 확인5 : ${response.data.location.address},")
            Log.d("UserData", "UserData 확인5 : ${response.data.location.locationId},")


            // DataStore에 저장
            userDataStore.saveUserProfile(user)


            // [확인용 코드] 저장 직후 바로 꺼내서 로그 찍기
            val savedUser = userDataStore.userProfileFlow.first()
            if (savedUser != null) {
                Log.d("DataStoreVerify", "✅ [확인 완료] 저장 직후 꺼내온 데이터: $savedUser")
            } else {
                Log.e("DataStoreVerify", "❌ [확인 실패] 저장 직후 데이터를 꺼낼 수 없습니다.")
            }
        }.map { } // 결과를 Unit으로 변환
    }


    // [추가] 닉네임 업데이트 구현
    override suspend fun updateNickname(nickname: String): ApiResult<Unit> {
        val request = UpdateNicknameRequest(nickname = nickname)
        // API 호출 후 결과를 ApiResult<Unit>으로 변환
        return handleApi { userApiService.updateNickname(request) }.map { }
    }

    // [추가] 프로필 이미지 URL 업데이트 구현
    override suspend fun updateProfileImage(imageUrl: String): ApiResult<Unit> {


        val request = UpdateProfileImageRequest(profileImage = imageUrl)

        Log.d("ProfileEditViewModel", "Requesting: $request")


//        val request = UpdateProfileImageRequest(profileImage = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSvyEPjOGflKIsX8Z4NVdyCEFT3BDLYkpKMuA&s")


        // API 호출 후 결과를 ApiResult<Unit>으로 변환
        return handleApi { userApiService.updateProfileImage(request) }.map { }
    }

    override suspend fun clearUserProfileData() {
        // DataStore의 사용자 프로필 정보를 초기값으로 덮어쓰거나 지우는 로직
        userDataStore.clear() // datastore를 전부 지우는 함수 같음
        Log.i("DataStore", "✅ DataStore의 사용자 프로필 데이터가 초기화되었습니다.")
    }


    // ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ

    // ---------------- [로그 추가된 부분] ----------------
    /**
     * 메인 위치 수정 구현
     */
    override suspend fun editMyLocation(locationId: Int, locationName: String): ApiResult<Unit> {

        // ❗️[로그 추가]
        Log.d("LocationUpdate", "  > UserRepositoryImpl: API 요청 시작...")
        Log.d("LocationUpdate", "  > API Endpoint: userApiService.updateUserLocation")
        Log.d("LocationUpdate", "  > DTO: UpdateUserLocation(locationId = $locationId)")

        // 1. API 호출 (닉네임, 프로필 이미지는 null로 보내 변경 없음을 알림)
        val result = handleApi {
            userApiService.updateUserLocation(
                UpdateUserLocation(locationId = locationId)
            )
        }

        // 2. API 호출이 성공했을 때만 로컬 DataStore의 위치 정보 갱신
        if (result is ApiResult.Success) {
            // ❗️[로그 추가]
            Log.d("LocationUpdate", "  > ✅ API 호출 성공. 로컬 DataStore 업데이트...")
            userDataStore.saveUserLocation(locationId, locationName)
        } else if (result is ApiResult.Error) {
            // ❗️[로그 추가]
            Log.e(
                "LocationUpdate",
                "  > ❌ API 호출 실패. 코드: ${result.code}, 메시지: ${result.errorBody?.message}"
            )
        } else if (result is ApiResult.Exception) {
            // ❗️[로그 추가]
            Log.e("LocationUpdate", "  > ❌ API 호출 예외 발생.", result.e)
        }

        // API 결과 반환 (결과 데이터는 Unit으로 변환)
        return result.map { }
    }

    /**
     * 즐겨찾기 목록 가져오기 구현
     */
    override fun getFavoriteLocations(): Flow<List<Pair<Int, String>>> {
        // DataStore의 Flow를 그대로 반환
        return userDataStore.favoriteLocationsFlow
    }

    /**
     * 즐겨찾기 추가 구현
     */
    override suspend fun addFavoriteLocation(id: Int, name: String) {
        userDataStore.addFavoriteLocation(id, name)
    }

    /**
     * 로그아웃 구현
     * (기존 withdrawAccount에서 이름 변경)
     */
    override suspend fun signOut(): ApiResult<Unit> { // <--- 함수명 변경
        Log.d(
            "Logout",
            "UserRepository: 서버 로그아웃 API (POST /api/v1/auth/sign-out) 요청 시작..."
        ) // <--- 로그 수정

        // 1. 서버에 로그아웃 API 호출
        val apiResult = handleApi { userApiService.signOut() }.map { } // <--- 호출할 함수명 변경

        if (apiResult is ApiResult.Success) {
            Log.i("Logout", "✅ 서버 로그아웃 성공.") // <--- 로그 수정
            // 로그아웃 시에는 로컬 프로필 정보를 지우지 않습니다.
            // userDataStore.clear() // <-- 이 코드는 회원탈퇴 시에만 필요하므로 주석 처리 (또는 삭제)
        } else {
            Log.e("Logout", "❌ 서버 로그아웃 실패: $apiResult") // <--- 로그 수정
        }

        // 3. ViewModel에 API 호출 결과 반환
        return apiResult
    }
    // ---------------------------------------------------


    // ------------------ [이 부분 추가] ------------------
    /**
     * 회원탈퇴 구현
     */
    override suspend fun deleteAccount(): ApiResult<Unit> {
        Log.d("Withdrawal", "UserRepository: 회원탈퇴 API (DELETE /api/v1/me) 요청 시작...")

        // 1. 서버에 계정 삭제 API 호출
        val apiResult = handleApi { userApiService.deleteAccount() }.map { }

        // 2. [중요] 서버 요청이 성공했을 때만 로컬 DataStore의 사용자 프로필 데이터 삭제
        if (apiResult is ApiResult.Success) {
            Log.i("Withdrawal", "✅ 서버 탈퇴 성공. 로컬 UserProfile 데이터를 삭제합니다.")
            userDataStore.clear() // <-- 계정이 삭제됐으니 로컬 프로필도 삭제
        } else {
            Log.e("Withdrawal", "❌ 서버 탈퇴 실패: $apiResult")
        }

        // 3. ViewModel에 API 호출 결과 반환
        return apiResult
    }
    // ---------------------------------------------------


}