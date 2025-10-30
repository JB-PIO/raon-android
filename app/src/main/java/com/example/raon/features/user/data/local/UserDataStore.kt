package com.example.raon.features.user.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.raon.core.common.AppConstants
import com.example.raon.features.user.domain.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// DataStore 인스턴스를 앱 컨텍스트를 이용해 싱글톤으로 생성
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_profile_prefs")

/**
 * 사용자 프로필 정보를 기기에 저장하고 관리하는 클래스. (Data 계층)
 * Repository를 통해서만 접근하는 것을 권장합니다.
 */
@Singleton
class UserDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    // DataStore에 데이터를 저장할 때 사용할 Key들을 정의합니다.
    private companion object {
        val KEY_USER_ID = intPreferencesKey("user_id")
        val KEY_NICKNAME = stringPreferencesKey("user_nickname")
        val KEY_EMAIL = stringPreferencesKey("user_email")
        val KEY_PROFILE_IMAGE = stringPreferencesKey("user_profile_image")
        val KEY_ADDRESS = stringPreferencesKey("user_address")

        val KEY_LOCATION_ID = intPreferencesKey("user_location_id") // ◀◀◀ 이 줄을 추가하세요


        val KEY_FAVORITE_LOCATIONS = stringSetPreferencesKey("user_favorite_locations")


    }

    /**
     * User Domain 모델 객체를 받아 DataStore에 저장합니다.
     * @param user 저장할 사용자 정보 (Domain Model)
     */
    suspend fun saveUserProfile(user: User) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_ID] = user.userId
            preferences[KEY_NICKNAME] = user.nickname
            preferences[KEY_EMAIL] = user.email
            preferences[KEY_PROFILE_IMAGE] =
                user.profileImage ?: AppConstants.DEFAULT_PROFILE_URL // null이면 빈 문자열 저장
            preferences[KEY_ADDRESS] = user.address
            preferences[KEY_LOCATION_ID] = user.locationId // ◀◀◀ 이 줄을 추가하세요

        }
    }

    // ---------------- [추가된 코드] ----------------
    /**
     * API를 통해 메인 위치 변경이 성공했을 때, 로컬 DataStore의 위치 정보만 갱신합니다.
     */
    suspend fun saveUserLocation(locationId: Int, address: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ADDRESS] = address
            preferences[KEY_LOCATION_ID] = locationId
        }
    }

    /**
     * DataStore에 저장된 사용자 프로필 정보를 Flow 형태로 제공합니다.
     * 데이터가 변경될 때마다 새로운 User 객체를 자동으로 발행(emit)합니다.
     * 저장된 정보가 없으면 null을 발행합니다.
     */
    val userProfileFlow: Flow<User?> = context.dataStore.data.map { preferences ->
        val userId = preferences[KEY_USER_ID]

        // userId가 존재하지 않으면 로그인하지 않은 상태로 간주하여 null을 반환합니다.
        if (userId == null) {
            null
        } else {
            User(
                userId = userId,
                nickname = preferences[KEY_NICKNAME] ?: "사용자",
                email = preferences[KEY_EMAIL] ?: "",
                profileImage = preferences[KEY_PROFILE_IMAGE], // null일 수 있음
                address = preferences[KEY_ADDRESS] ?: "",
                locationId = preferences[KEY_LOCATION_ID] ?: 1
            )
        }
    }


    // ---------------- [추가된 코드] ----------------
    /**
     * 저장된 즐겨찾기 위치 목록을 Flow<List<Pair<ID, 주소>>> 형태로 제공합니다.
     */
    val favoriteLocationsFlow: Flow<List<Pair<Int, String>>> =
        context.dataStore.data.map { preferences ->
            preferences[KEY_FAVORITE_LOCATIONS].orEmpty()
                .mapNotNull { storedString ->
                    // "id,이름" 형식 파싱
                    val parts = storedString.split(",", limit = 2)
                    if (parts.size == 2) {
                        parts[0].toIntOrNull()?.let { id ->
                            Pair(id, parts[1])
                        }
                    } else {
                        null // 형식이 잘못된 데이터는 무시
                    }
                }
                // 저장된 Set은 순서를 보장하지 않으므로, addFavoriteLocation에서 순서를 관리
                // 여기서는 List로 변환
                .toList()
        }

    /**
     * 새 즐겨찾기 위치를 추가합니다. (최대 2개)
     * @param id 위치 ID
     * @param name 위치의 전체 주소
     */
    suspend fun addFavoriteLocation(id: Int, name: String) {
        context.dataStore.edit { preferences ->
            // 순서를 관리하기 위해 List로 변환
            val currentFavoritesList = preferences[KEY_FAVORITE_LOCATIONS].orEmpty()
                .toMutableList()

            val newFavoriteString = "$id,$name"

            // 1. 이미 목록에 있으면 제거 (순서를 최신으로 갱신하기 위함)
            currentFavoritesList.removeIf { it.startsWith("$id,") }

            // 2. 새 위치를 리스트의 맨 뒤(최신)에 추가
            currentFavoritesList.add(newFavoriteString)

            // 3. 만약 2개를 초과하면, 가장 오래된(맨 앞) 항목을 제거
            while (currentFavoritesList.size > 2) {
                currentFavoritesList.removeAt(0)
            }

            // 4. 최종 리스트를 Set으로 변환하여 다시 저장
            preferences[KEY_FAVORITE_LOCATIONS] = currentFavoritesList.toSet()
        }
    }

    /**
     * 로그아웃 시 저장된 모든 사용자 정보를 삭제합니다.
     */
    suspend fun clear() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}