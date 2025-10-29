package com.example.raon.core.common

/**
 * 앱 전체에서 공통으로 사용되는 상수 값을 정의하는 Object Class
 */
object AppConstants {


    // --- 서버별 기본 URL ---
    //     const val RAON_SERVER_URL = "http://10.0.2.2:4000/" // 에뮬레이터용
    //     const val RAON_SERVER_URL = "http://192.168.24.50:4000/" // 기기 연결용

    //     const val RAON_SERVER_URL = "https://158.179.164.210/" // 실제 앱 용 (ip)
    const val RAON_SERVER_URL = "https://raon.store/" // 실제 앱 용 (도메인)


    const val DEFAULT_PROFILE_URL = "http://default.profile"    // 기본 프로필 URL

    // S3 이미지 기본 주소 -> 서버에서 가져온 데이터에서 순수 파일 이름만 추출하기 위해서 필요
    const val S3_BASE_URL = "https://raon-market-images-prod.s3.ap-northeast-2.amazonaws.com/"


}