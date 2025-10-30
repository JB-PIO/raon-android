package com.example.raon.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.raon.features.category.data.local.CategoryDao
import com.example.raon.features.category.data.local.CategoryEntity

// room에서 사
@Database(
    entities = [CategoryEntity::class], // 1️⃣ 이 데이터베이스가 관리할 테이블(Entity) 목록
    version = 1,                         // 2️⃣ 데이터베이스 버전
    exportSchema = false                 // 스키마 내보내기 여부 (보통 false로 둠)
)
abstract class AppDatabase : RoomDatabase() {

    // 3️⃣ 이 데이터베이스에 포함될 DAO(Data Access Object)들을 선언
    abstract fun categoryDao(): CategoryDao

    // 만약 다른 DAO가 있다면 여기에 추가
    // abstract fun userDao(): UserDao
    // abstract fun itemDao(): ItemDao
}