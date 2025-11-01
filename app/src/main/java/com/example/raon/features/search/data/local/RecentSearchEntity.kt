package com.example.raon.features.search.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey


// 최근 검색어를 저장할 Entity
@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey
    val query: String, // 검색어를 PrimaryKey로 사용
    val timestamp: Long // 검색한 시간
)