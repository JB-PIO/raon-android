package com.example.raon.features.chat.data.remote.dto.ai


import com.google.gson.annotations.SerializedName

/**
 * AI 이미지 분석 API 응답의 "data" 필드에 해당하는 DTO
 */
data class ImageAnalysisResponseDto(
    @SerializedName("results")
    val results: List<ImageAnalysisResult>
)

/**
 * 개별 이미지 분석 결과
 */
data class ImageAnalysisResult(
    @SerializedName("imageId")
    val imageId: Int,

    @SerializedName("imageUrl")
    val imageUrl: String,

    @SerializedName("result")
    val result: String, // "SAFE", "WARNING", "DANGER" 등

    @SerializedName("maxScore")
    val maxScore: Double?, // nullable

    @SerializedName("similarImageUrls")
    val similarImageUrls: List<String>? // nullable
)


// ImageAnalysisResponseDto의 확장 함수로 정의
fun ImageAnalysisResponseDto.toDomainModel(): List<com.example.raon.features.chat.ui.ImageAnalysisResult> {
    return this.results.map { dtoResult ->
        com.example.raon.features.chat.ui.ImageAnalysisResult(
            imageUrl = dtoResult.imageUrl,
            result = dtoResult.result,
            similarImages = dtoResult.similarImageUrls ?: emptyList() // null일 경우 빈 리스트로 처리
        )
    }
}