package com.example.raon.core.database

// 🔽🔽🔽 1. 새로 만든 Chat Entity/DAO를 import 합니다. 🔽🔽🔽
import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.raon.features.category.data.local.CategoryDao
import com.example.raon.features.category.data.local.CategoryEntity
import com.example.raon.features.chat.data.local.ChatDao
import com.example.raon.features.chat.data.local.ChatMessageEntity
import com.example.raon.features.chat.data.local.ChatRoomEntity
import com.example.raon.features.search.data.local.RecentSearchDao
import com.example.raon.features.search.data.local.RecentSearchEntity

// room에서 사
@Database(
    entities = [
        CategoryEntity::class,
        RecentSearchEntity::class,
        // 🔽🔽🔽 2. 새 Entity 두 개를 entities 배열에 추가합니다. 🔽🔽🔽
        ChatMessageEntity::class,
        ChatRoomEntity::class
    ],
    version = 3, // 🔽🔽🔽 3. DB 스키마가 변경됐으므로 버전을 올립니다. (기존 2에서 3으로) 🔽🔽🔽
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun recentSearchDao(): RecentSearchDao

    // 🔽🔽🔽 4. 새 ChatDao를 위한 추상 함수를 추가합니다. 🔽🔽🔽
    abstract fun chatDao(): ChatDao
}