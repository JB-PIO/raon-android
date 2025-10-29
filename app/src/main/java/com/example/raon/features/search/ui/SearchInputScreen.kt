package com.example.raon.features.search.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SearchInputScreen(
    onNavigateToSearchResult: (String) -> Unit = {},
    onCloses: () -> Unit = {},  // 닫기 이벤트
    onNavigateToHome: () -> Unit = {}   // 홈가기 이벤트


) {
    var searchQuery by remember { mutableStateOf("") }
    val recentSearches = remember {
        mutableStateListOf("아이폰", "아이폰13미니", "아이폰12", "갤럭시", "운동화", "맥북")
//        mutableStateListOf("아이폰", "아이폰13미니", "아이폰12", "갤럭시", "운동화", "맥북")

    }
    val focusManager = LocalFocusManager.current

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {

            // 상단 검색바 부분
            SearchAppBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = {

                    // 검색시 비었는지 확인하고 비어있지 않으면 검색 결과 화면으로 이동 -> 검색어 넘겨줌
                    if (searchQuery.isNotBlank()) {
                        focusManager.clearFocus() // 키보드 내리기
                        onNavigateToSearchResult(searchQuery)

                    }

                },      // 검색 이벤트
                onBackClick = {
                    onCloses()
                },
                onHomeClick = {
                    onNavigateToHome()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 최근 검색어 섹션
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "최근 검색",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "전체 삭제",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 세로로 쌓인 최근 검색어 목록
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    recentSearches.forEach { search ->
                        RecentSearchItem(
                            text = search,
                            onDelete = { recentSearches.remove(search) }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun RecentSearchItem(
    text: String,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // 패딩 조정
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = text, fontSize = 16.sp)
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.Clear,
                contentDescription = "Delete search term",
                modifier = Modifier.size(18.dp), // 아이콘 크기 조정
                tint = Color.Gray
            )
        }
    }
}

