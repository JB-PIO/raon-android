package com.example.raon.features.search.data.local


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSearchDao {

    // 검색어를 추가 (이미 있으면 timestamp만 덮어쓰기)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(search: RecentSearchEntity)

    // 최근 검색어 목록을 시간순으로 가져오기 (예: 20개)
    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT 20")
    fun getRecentSearches(): Flow<List<RecentSearchEntity>>

    // 특정 검색어 삭제
    @Query("DELETE FROM recent_searches WHERE `query` = :query")
    suspend fun delete(query: String)

    // 전체 삭제
    @Query("DELETE FROM recent_searches")
    suspend fun deleteAll()
}