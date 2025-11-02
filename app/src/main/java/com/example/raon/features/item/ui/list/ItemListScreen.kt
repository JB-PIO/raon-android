@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.raon.features.item.ui.list

// 2. 공용 모델 import (별칭 사용)
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.raon.core.ui.component.ItemListComponoents
import com.example.raon.features.main.ui.MainViewModel
import com.example.raon.core.ui.model.ItemListUiModel as CoreItemUiModel


@Composable
fun ItemListScreen(
    modifier: Modifier = Modifier,
    viewModel: ItemListViewModel = hiltViewModel(),
    mainviewModel: MainViewModel = hiltViewModel(),
    onNavigateToSearch: () -> Unit,
    onNavigateToAddAddress: () -> Unit,
    onItemClick: (Int) -> Unit,  // ItemDetail 페이지로 이동 이벤트
    onLocationClick: () -> Unit // [수정] MainView로 메뉴 열기 이벤트를 전달하기 위한 파라미터 추가
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    // LazyColumn의 스크롤 상태를 추적할 State 생성
    val listState = rememberLazyListState()

    // 3. ViewModel의 UI 모델을 공용 컴포넌트의 UI 모델로 변환(매핑)합니다.
    val coreItems = uiState.items.map { item ->
        CoreItemUiModel(
            id = item.id,
            title = item.title,
            location = item.location,
            timeAgo = item.timeAgo,
            price = item.price,
            imageUrl = item.imageUrl,
            comments = item.comments,
            likes = item.likes,
            viewCount = item.viewCount,
            status = item.status,
            isFavorite = false, // ItemListScreen에서는 찜하기 기능을 사용하지 않으므로 false

        )
    }

    Column(modifier = modifier.fillMaxSize()) {

        // Refresh 기능
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.weight(1f), // 남은 공간 모두 사용
            state = pullToRefreshState // pullToRefreshState를 state로 전달
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                // 4. 기존 ItemList 대신 공용 ItemListComponoents를 호출합니다.
                ItemListComponoents(
                    items = coreItems, // 매핑된 리스트 전달
                    onItemClick = onItemClick,
                    // 공용 컴포넌트에 listState와 isLoading 상태 전달
                    listState = listState,
                    isLoading = uiState.isLoading
                    // isFavoriteList, onFavoriteClick 등은 기본값(false, empty) 사용
                )

                // 최초 로딩 시에만 중앙에 표시 (데이터가 없을 때)
                if (uiState.isLoading && !uiState.isRefreshing && coreItems.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = Color.Red,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            }
        }
    }

    // 리스트의 끝에 도달했는지 감지하는 로직
    val isAtEnd = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount == 0) {
                false
            } else {
                val lastVisibleItem = visibleItemsInfo.lastOrNull()
                // 마지막으로 보이는 아이템의 인덱스가 (전체 아이템 개수 - 1)과 같은지 확인
                lastVisibleItem != null && lastVisibleItem.index == layoutInfo.totalItemsCount - 1
            }
        }
    }

    // 👇 [수정 8] LaunchedEffect가 uiState.hasNextPage 값의 변경을 감지하도록 키에 추가
    LaunchedEffect(isAtEnd.value, uiState.isLoading, uiState.hasNextPage) {
        // isAtEnd가 true이고, 추가 로딩(isLoading)이나 새로고침(isRefreshing) 중이 아닐 때
        // 👇 [수정 9] 그리고 다음 페이지가 있을 때(hasNextPage == true)만 로드
        if (isAtEnd.value && !uiState.isLoading && !uiState.isRefreshing && uiState.hasNextPage) {
            viewModel.loadMoreItems()
        }
    }
}

// --- 이하 부속 Composable 함수들 ---

@Composable
fun HomeScreenTopAppBar(
    address: String,
    onNavigateToSearch: () -> Unit,
    onLocationClick: () -> Unit // [추가] 주소 클릭 이벤트만 상위로 전달
) {

    Column(modifier = Modifier.statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                // 기존 주소 표시 UI (클릭 가능하게)
                Row(
                    modifier = Modifier.clickable(onClick = onLocationClick), // 클릭 시 메뉴 열기 이벤트 호출
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = address,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        // MaterialTheme 사용
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "지역 선택",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            // TODO: 검색, 알림 아이콘 등을 여기에 배치
            // IconButton(onClick = onNavigateToSearch) {
            //     Icon(Icons.Default.Search, contentDescription = "검색")
            // }
        }
    }
}