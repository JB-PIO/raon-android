package com.example.raon.features.profile.ui

// ▼▼▼ [1] 사용하지 않는 import 제거 ▼▼▼
// import android.util.Log // 👈 제거
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
// import com.example.raon.core.common.toInstant // 👈 제거
// import com.example.raon.core.common.toKSTLocalDateTime // 👈 제거
// import com.example.raon.core.common.toRelativeTimeString // 👈 제거
import com.example.raon.core.ui.component.ItemListComponoents
import com.example.raon.core.ui.model.ItemListUiModel
import com.example.raon.ui.theme.BrandDarkText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// ▲▲▲ [1] import 제거 완료 ▲▲▲

private val tabs = listOf("판매중", "예약중", "거래완료")
private val statusValues = listOf("AVAILABLE", "RESERVED", "SOLD")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SalesHistoryScreen(

    navBackStackEntry: NavBackStackEntry,
    onBackClick: () -> Unit,
    onItemClick: (itemId: Int) -> Unit,
    onNavigateToBuyerSelection: (itemId: Int) -> Unit,
    viewModel: SalesHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedItemId by remember { mutableStateOf<Int?>(null) }
    var selectedItemPageIndex by remember { mutableStateOf(0) }

    // --- 이벤트 처리 ---
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is SalesHistoryEvent.NavigateToBuyerSelection -> onNavigateToBuyerSelection(event.itemId)
                is SalesHistoryEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // --- 구매자 선택 결과 수신 ---
    val selectedBuyerIdResult by navBackStackEntry
        .savedStateHandle
        .getStateFlow<Int?>("selected_buyer_id", null)
        .collectAsState()

    LaunchedEffect(selectedBuyerIdResult) {
        selectedBuyerIdResult?.let { resultValue ->
            uiState.selectedItemIdForStatusChange?.let { itemIdToUpdate ->
                val buyerIdToSend: Int? = if (resultValue == -1) null else resultValue

                // ▼▼▼ [핵심 수정] 무한 루프를 방지하기 위해 'executeUpdateStatus'를 직접 호출 ▼▼▼
                viewModel.executeUpdateStatus(itemIdToUpdate, "SOLD", buyerIdToSend)
            }
            // 처리가 끝난 결과는 반드시 null로 다시 설정하여 중복 실행을 방지
            navBackStackEntry.savedStateHandle["selected_buyer_id"] = null
        }
    }

    // --- UI (이하 수정 없음) ---
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                when (selectedItemPageIndex) {
                    0 -> { // "판매중" 탭
                        BottomSheetMenuItem(
                            icon = Icons.Default.HourglassEmpty,
                            text = "예약중으로 변경",
                            onClick = {
                                selectedItemId?.let {
                                    // '예약중'은 분기 함수(updateProductStatus)를 호출해도 됨
                                    viewModel.updateProductStatus(
                                        it,
                                        statusValues[1]
                                    )
                                }
                                scope.launch { sheetState.hide() }
                                    .invokeOnCompletion { showBottomSheet = false }
                            }
                        )
                        BottomSheetMenuItem(
                            icon = Icons.Default.CheckCircleOutline,
                            text = "거래완료로 변경",
                            onClick = {
                                selectedItemId?.let {
                                    // '거래완료'는 분기 함수(updateProductStatus)를 호출
                                    viewModel.updateProductStatus(
                                        it,
                                        statusValues[2]
                                    )
                                }
                                scope.launch { sheetState.hide() }
                                    .invokeOnCompletion { showBottomSheet = false }
                            }
                        )
                    }

                    1 -> { // "예약중" 탭
                        BottomSheetMenuItem(
                            icon = Icons.Default.Storefront,
                            text = "판매중으로 변경",
                            onClick = {
                                selectedItemId?.let {
                                    viewModel.updateProductStatus(
                                        it,
                                        statusValues[0]
                                    )
                                }
                                scope.launch { sheetState.hide() }
                                    .invokeOnCompletion { showBottomSheet = false }
                            }
                        )
                        BottomSheetMenuItem(
                            icon = Icons.Default.CheckCircleOutline,
                            text = "거래완료로 변경",
                            onClick = {
                                selectedItemId?.let {
                                    viewModel.updateProductStatus(
                                        it,
                                        statusValues[2]
                                    )
                                }
                                scope.launch { sheetState.hide() }
                                    .invokeOnCompletion { showBottomSheet = false }
                            }
                        )
                    }

                    2 -> { // "거래완료" 탭
                        BottomSheetMenuItem(
                            icon = Icons.Default.Storefront,
                            text = "판매중으로 변경",
                            onClick = {
                                selectedItemId?.let {
                                    viewModel.updateProductStatus(
                                        it,
                                        statusValues[0]
                                    )
                                }
                                scope.launch { sheetState.hide() }
                                    .invokeOnCompletion { showBottomSheet = false }
                            }
                        )
                        BottomSheetMenuItem(
                            icon = Icons.Default.HourglassEmpty,
                            text = "예약중으로 변경",
                            onClick = {
                                selectedItemId?.let {
                                    viewModel.updateProductStatus(
                                        it,
                                        statusValues[1]
                                    )
                                }
                                scope.launch { sheetState.hide() }
                                    .invokeOnCompletion { showBottomSheet = false }
                            }
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("나의 판매내역") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,

                containerColor = MaterialTheme.colorScheme.surface, // 예: surface 색상
                // ▼▼▼ [핵심 수정] 파라미터에 타입을 강제로 명시 ▼▼▼
                // ▼▼▼ [핵심 수정] 람다 파라미터로 'tabPositions'를 받습니다 ▼▼▼
//                indicator = { tabPositions ->
//
//                    // ▼▼▼ [핵심 수정] pagerState를 사용해 현재 탭의 TabPosition을 전달 ▼▼▼
//                    if (tabPositions.isNotEmpty()) { // 리스트가 비어있지 않을 때만
//                        TabRowDefaults.PrimaryIndicator(
//                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
//                            color = BrandYellow
//                        )
//                    }
//                }

            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) },
                        selectedContentColor = BrandDarkText,
                        unselectedContentColor = Color.Gray
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val itemsToShow = when (page) {
                    0 -> uiState.sellingItems
                    1 -> uiState.reservedItems
                    2 -> uiState.soldItems
                    else -> emptyList()
                }

                SalesListContent(
                    isLoading = uiState.isLoading,
                    items = itemsToShow,
                    emptyMessage = "'${tabs[page]}' 내역이 없습니다.",
                    onItemClick = onItemClick,
                    onSettingsClick = { itemId ->
                        selectedItemId = itemId
                        selectedItemPageIndex = page
                        showBottomSheet = true
                    }
                )
            }
        }
    }
}

@Composable
private fun SalesListContent(
    isLoading: Boolean,
    items: List<ItemListUiModel>,
    emptyMessage: String,
    onItemClick: (Int) -> Unit,
    onSettingsClick: (itemId: Int) -> Unit
) {
    // ▼▼▼ [2] 'formattedItems'를 만들던 'remember' 로직 전체 제거 ▼▼▼
    // val formattedItems = remember(items) { ... } // 👈 이 블록 전체 제거
    // ▲▲▲ [2] 제거 완료 ▲▲▲

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (isLoading && items.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        } else if (items.isEmpty()) {
            Text(text = emptyMessage, modifier = Modifier.padding(top = 16.dp))
        } else {
            // ▼▼▼ [3] 'items' 원본 리스트를 그대로 전달 ▼▼▼
            ItemListComponoents(
                // items = formattedItems, // 👈 수정 전
                items = items, // 👈 수정 후 (원본)
                onItemClick = onItemClick,
                showSettingsIcon = true,
                onSettingsClick = onSettingsClick
            )
            // ▲▲▲ [3] 전달 완료 ▲▲▲
        }
    }
}

@Composable
private fun BottomSheetMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = text)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text)
    }
}

// ▼▼▼ [4] 파일 하단의 'formatTimeAgo' 헬퍼 함수 전체 제거 ▼▼▼
// private fun formatTimeAgo(dateTimeString: String): String { ... } // 👈 이 함수 전체 제거
// ▲▲▲ [4] 제거 완료 ▲▲▲