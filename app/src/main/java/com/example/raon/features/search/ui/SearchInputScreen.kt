package com.example.raon.features.search.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.raon.features.search.data.local.RecentSearchEntity

@Composable
fun SearchInputScreen(
    viewModel: SearchInputViewModel = hiltViewModel(), // 2. ViewModel 주입
    onNavigateToSearchResult: (String) -> Unit = {},
    onCloses: () -> Unit = {},  // 닫기 이벤트
    onNavigateToHome: () -> Unit = {}   // 홈가기 이벤트
) {
    var searchQuery by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle() // 3. ViewModel의 uiState 구독
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
                    val trimmedQuery = searchQuery.trim()
                    if (trimmedQuery.isNotBlank()) {
                        focusManager.clearFocus()

                        // --- 4. 수정된 부분 ---
                        viewModel.onSearch(trimmedQuery) // 1. ViewModel에 검색어 저장 요청
                        onNavigateToSearchResult(trimmedQuery) // 2. 결과 화면으로 이동
                        // --- 수정 끝 ---
                    }
                },
                onBackClick = onCloses,
                onHomeClick = onNavigateToHome
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 5. 최근 검색어 섹션 (ViewModel과 연동)
            RecentSearchesSection(
                searches = uiState.recentSearches,
                onSearchClick = { query ->
                    // 최근 검색어 클릭 시
                    focusManager.clearFocus()
                    viewModel.onSearch(query) // 클릭도 "검색"으로 간주하여 timestamp 갱신
                    onNavigateToSearchResult(query)
                },
                onDeleteClick = viewModel::onDeleteSearch,
                onDeleteAllClick = viewModel::onDeleteAllSearches
            )
        }
    }
}

// 6. 최근 검색어 UI (별도 Composable로 분리)
@Composable
fun RecentSearchesSection(
    searches: List<RecentSearchEntity>,
    onSearchClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onDeleteAllClick: () -> Unit,
) {
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
            if (searches.isNotEmpty()) { // 목록이 있을 때만 '전체 삭제' 표시
                TextButton(
                    onClick = onDeleteAllClick,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "전체 삭제",
                        style = MaterialTheme.typography.labelMedium, // labelSmall -> Medium
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (searches.isEmpty()) {
            Text(
                text = "최근 검색어가 없습니다.",
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else {
            // 세로로 쌓인 최근 검색어 목록
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = searches,
                    key = { it.query }
                ) { search ->
                    RecentSearchItem(
                        text = search.query,
                        onItemClick = { onSearchClick(search.query) }, // 항목 클릭
                        onDelete = { onDeleteClick(search.query) } // X 버튼 클릭
                    )
                }
            }
        }
    }
}


@Composable
fun RecentSearchItem(
    text: String,
    onItemClick: () -> Unit, // 7. 전체 Row 클릭 이벤트 추가
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick) // 8. Row 전체 클릭 가능하도록 수정
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f) // 9. 텍스트가 길어도 X 버튼을 밀어내도록
        )

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.Close, // 10. Clear -> Close 아이콘 (더 명확함)
                contentDescription = "Delete search term",
                modifier = Modifier.size(18.dp),
                tint = Color.Gray
            )
        }
    }
}