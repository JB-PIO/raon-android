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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
                    onItemClick = onItemClick
                    // isFavoriteList, onFavoriteClick 등은 기본값(false, empty) 사용
                )

                if (uiState.isLoading && !uiState.isRefreshing) { // 로딩 상태 중복 방지
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

// 5. 기존에 이 파일에 있던 ItemList 및 ItemListItem 함수 정의를 삭제했습니다.