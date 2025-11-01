package com.example.raon.features.search.domain.repository

import com.example.raon.features.search.data.local.RecentSearchEntity
import com.example.raon.features.search.ui.model.SearchItemUiModel
import kotlinx.coroutines.flow.Flow

/**
 * 검색 기능에 대한 데이터 소스 규칙을 정의하는 인터페이스입니다.
 * ViewModel은 이 인터페이스에만 의존합니다.
 */
interface SearchRepository {

    /**
     * 다양한 조건으로 상품 목록을 검색합니다.
     *
     * @param keyword 검색할 키워드 (필수)
     * @param page 요청할 페이지 번호 (필수)
     * @param size 페이지 당 아이템 수
     * @param sort 정렬 조건 (예: "createdAt,desc")
     * @param categoryId 카테고리 ID
     * @param locationId 지역 ID
     * @param minPrice 최소 가격
     * @param maxPrice 최대 가격
     * @param condition 상품 상태 ("USED", "NEW")
     * @param tradeType 거래 유형 ("DIRECT", "DELIVERY")
     * @param status 판매 상태 ("AVAILABLE", "RESERVED", "SOLD")
     * @return 성공 시 상품 리스트를 담은 Result, 실패 시 예외를 담은 Result
     */
    suspend fun searchProducts(
        keyword: String,
        page: Int,
        size: Int = 20,
        sort: String?,
        categoryId: Int?,
        locationId: Int?,
        minPrice: Int?,
        maxPrice: Int?,
        condition: String?,
        tradeType: String?,
        status: String?
    ): Result<List<SearchItemUiModel>>

    /** 최근 검색어 목록을 Flow로 가져오기 (최신순) */
    fun getRecentSearches(): Flow<List<RecentSearchEntity>>

    /** 최근 검색어를 추가 (이미 있으면 시간만 갱신) */
    suspend fun addRecentSearch(query: String)

    /** 특정 최근 검색어를 삭제 */
    suspend fun deleteRecentSearch(query: String)

    /** 모든 최근 검색어를 삭제 */
    suspend fun deleteAllRecentSearches()


}