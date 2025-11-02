package com.example.raon.features.search.ui


// import 추가
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.raon.core.ui.component.ItemListComponoents
import com.example.raon.features.category.data.local.CategoryEntity
import com.example.raon.features.category.ui.CategoryViewModel
import com.example.raon.ui.theme.BrandYellow
import kotlinx.coroutines.launch
import com.example.raon.core.ui.model.ItemListUiModel as CoreItemUiModel


// 화면 콘텐츠
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultScreen(
    modifier: Modifier = Modifier,
    searchQuery: String,
    searchViewModel: SearchResultViewModel = hiltViewModel(),
    // 카테고리 뷰모델 추가
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    onItemClick: (Int) -> Unit = {},     // 아이템 클릭. 기본값 빈 함수
    onCloses: () -> Unit = {},  // 닫기
    onNavigateToHome: () -> Unit = {}   // 홈가기

) {

    // 뷰모델 uistate 구독
    val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()
    // 카테고리 뷰모델 state 구독 (Room DB 최상위 카테고리 목록)
    val categories by categoryViewModel.categories.collectAsStateWithLifecycle()

    // 스크롤 상태와 PullToRefresh 상태 추가
    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()

    // 뷰모델 UI 모델을 공용 컴포넌트 UI 모델로 변환
    val coreItems = uiState.products.map { item ->
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
            isFavorite = false // 검색 결과는 찜 아님 (false)
        )
    }

    // 첫 진입 시 searchQuery로 검색 시작 (한 번만 실행되도록 수정)
    LaunchedEffect(key1 = Unit) {
        searchViewModel.onSearch(searchQuery)
    }

    var showSortBottomSheet by remember { mutableStateOf(false) }
    var showCategoryBottomSheet by remember { mutableStateOf(false) }
    var showLocationBottomSheet by remember { mutableStateOf(false) }
    var showPriceBottomSheet by remember { mutableStateOf(false) }

    val sortBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val categoryBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val locationBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val priceBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()


    // ==========================================================
    // 1. 정렬 옵션 맵 정의
    val sortOptionsMap = remember {
        mapOf(
            "createdAt,desc" to "최신순",
            "viewCount,desc" to "조회수순",
            "price,asc" to "낮은 가격순",
            "price,desc" to "높은 가격순",
            "location,asc" to "가까운순"
        )
    }

    // 2. 현재 UI State의 정렬 값으로 표시 텍스트 찾기
    val currentSortDisplayName = sortOptionsMap[uiState.sortOption] ?: "정렬"
    // ==========================================================


    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                SearchAppBar(
                    query = uiState.searchQuery,
                    onQueryChange = { newQuery -> searchViewModel.onQueryChanged(newQuery) },
                    false,
                    onSearch = { /* 검색 로직 */ },
                    onBackClick = { onCloses() },
                    onHomeClick = { onNavigateToHome() }
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ==========================================================
            // 3. FilterControls에 현재 정렬 표시 텍스트 전달
            FilterControls(
                minPrice = uiState.minPrice,
                maxPrice = uiState.maxPrice,
                categoryName = uiState.categoryName,
                currentSortName = currentSortDisplayName,
                onSortClick = { showSortBottomSheet = true },
                onCategoryClick = { showCategoryBottomSheet = true },
                onLocationClick = { showLocationBottomSheet = true },
                onPriceClick = { showPriceBottomSheet = true }
            )
            // ==========================================================
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))

            // 4. PullToRefreshBox로 리스트 영역 감싸기
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { searchViewModel.refresh() }, // ViewModel의 refresh 호출
                modifier = Modifier.fillMaxSize(),
                state = pullToRefreshState
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 5. 공용 아이템 리스트 사용 (listState, isLoading 전달)
                    ItemListComponoents(
                        items = coreItems,
                        onItemClick = onItemClick,
                        listState = listState,
                        isLoading = uiState.isLoading
                    )

                    // 6. 최초 로딩 시 (아이템이 없을 때) 중앙 스피너 표시
                    if (uiState.isLoading && !uiState.isRefreshing && coreItems.isEmpty()) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 7. 에러 메시지 표시
                    uiState.error?.let { message ->
                        Text(
                            text = message,
                            color = Color.Red,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }

                    // 8. 검색 결과가 없는 경우
                    if (!uiState.isLoading && !uiState.isRefreshing && coreItems.isEmpty() && uiState.error == null) {
                        Text(
                            text = "'${uiState.searchQuery}'에 대한 검색 결과가 없습니다.",
                            color = Color.Gray,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }


    // --- 바텀시트들 (기존 코드와 동일) ---
    // ==========================================================
    if (showSortBottomSheet) {
        SortBottomSheet(
            sheetState = sortBottomSheetState,
            currentSortOption = uiState.sortOption,
            sortOptions = sortOptionsMap,
            onSortSelected = { newSortValue ->
                searchViewModel.onSortChanged(newSortValue)
                scope.launch { sortBottomSheetState.hide() }.invokeOnCompletion {
                    if (!sortBottomSheetState.isVisible) showSortBottomSheet = false
                }
            },
            onDismissRequest = {
                scope.launch { sortBottomSheetState.hide() }.invokeOnCompletion {
                    if (!sortBottomSheetState.isVisible) showSortBottomSheet = false
                }
            }
        )
    }
    // ==========================================================

    if (showCategoryBottomSheet) {
        CategoryFilterBottomSheet(
            sheetState = categoryBottomSheetState,
            categories = categories,
            currentCategoryId = uiState.categoryId,
            onDismissRequest = {
                scope.launch { categoryBottomSheetState.hide() }.invokeOnCompletion {
                    if (!categoryBottomSheetState.isVisible) showCategoryBottomSheet = false
                }
            },
            onApplyClick = { selectedCategory ->
                searchViewModel.onCategoryChanged(
                    selectedCategory?.categoryId?.toInt(),
                    selectedCategory?.name
                )
                scope.launch { categoryBottomSheetState.hide() }.invokeOnCompletion {
                    if (!categoryBottomSheetState.isVisible) showCategoryBottomSheet = false
                }
            },
            onResetClick = {
                searchViewModel.onCategoryChanged(null, null)
                scope.launch { categoryBottomSheetState.hide() }.invokeOnCompletion {
                    if (!categoryBottomSheetState.isVisible) showCategoryBottomSheet = false
                }
            }
        )
    }

    if (showLocationBottomSheet) {
        SimpleBottomSheet(
            title = "지역 선택",
            content = { Text("지역 선택 내용은 여기에 들어갑니다.") },
            sheetState = locationBottomSheetState,
            onDismissRequest = {
                scope.launch { locationBottomSheetState.hide() }.invokeOnCompletion {
                    if (!locationBottomSheetState.isVisible) showLocationBottomSheet = false
                }
            }
        )
    }

    if (showPriceBottomSheet) {
        PriceFilterBottomSheet(
            sheetState = priceBottomSheetState,
            initialMinPrice = uiState.minPrice,
            initialMaxPrice = uiState.maxPrice,
            onDismissRequest = {
                scope.launch { priceBottomSheetState.hide() }.invokeOnCompletion {
                    if (!priceBottomSheetState.isVisible) showPriceBottomSheet = false
                }
            },
            onApplyClick = { min, max ->
                searchViewModel.onPriceChanged(min, max)
                // 닫는 로직은 PriceFilterBottomSheet 내부에서 처리
            }
        )
    }

    // --- (신규) 페이징 트리거 ---

    // 9. 리스트의 끝에 도달했는지 감지하는 로직
    val isAtEnd = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount == 0) {
                false
            } else {
                val lastVisibleItem = visibleItemsInfo.lastOrNull()
                lastVisibleItem != null && lastVisibleItem.index == layoutInfo.totalItemsCount - 1
            }
        }
    }

    // 10. 리스트 끝에 도달했고, 로딩 중이 아닐 때 loadMoreItems 호출
    LaunchedEffect(isAtEnd.value, uiState.isLoading) {
        if (isAtEnd.value && !uiState.isLoading && !uiState.isRefreshing) {
            searchViewModel.loadMoreItems()
        }
    }
}


// ==========================================================
// FilterControls 함수 수정 (기존 코드와 동일)
@Composable
fun FilterControls(
    minPrice: Int?,
    maxPrice: Int?,
    categoryName: String?,
    currentSortName: String, // 1. currentSortName 파라미터 추가
    onSortClick: () -> Unit,
    onLocationClick: () -> Unit,
    onPriceClick: () -> Unit,
    onCategoryClick: () -> Unit
) {
    var isChecked by remember { mutableStateOf(true) }

    // 가격 범위 텍스트 생성
    val priceText = when {
        minPrice != null && maxPrice != null -> {
            // 10,000원 - 50,000원
            "%,d원".format(minPrice) + " - " + "%,d원".format(maxPrice)
        }

        minPrice != null -> {
            // 10,000원 이상
            "%,d원".format(minPrice) + " 이상"
        }

        maxPrice != null -> {
            // 50,000원 이하
            "%,d원".format(maxPrice) + " 이하"
        }

        else -> "가격" // 기본값
    }

    // 카테고리 텍스트, 적용 상태
    val categoryText = categoryName ?: "카테고리"
    val isCategoryFilterApplied = categoryName != null

    // 가격 필터 적용 확인
    val isPriceFilterApplied = minPrice != null || maxPrice != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 상단 행: 필터 드롭다운 버튼들
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 2. "최신순" 대신 currentSortName 사용
            FilterDropdownButton(
                text = currentSortName,
                onClick = onSortClick,
                // 3. (선택) 기본값 아니면 강조
                isApplied = currentSortName != "최신순"
            )
//            FilterDropdownButton(text = "위치 정렬", onClick = onLocationClick) // (주석 처리됨)

            // "가격" 대신 priceText 사용
            // 가격 버튼에 isApplied 전달
            FilterDropdownButton(
                text = priceText,
                onClick = onPriceClick,
                isApplied = isPriceFilterApplied
            )

            // 카테고리 버튼
            FilterDropdownButton(
                text = categoryText,
                onClick = onCategoryClick,
                isApplied = isCategoryFilterApplied
            )
        }

        // '판매중만 보기' 스위치
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = isChecked,
                onCheckedChange = { isChecked = it },
                modifier = Modifier.scale(0.75f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BrandYellow,
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
                ),
                thumbContent = null
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("판매중만 보기", fontSize = 14.sp)
        }
    }
}
// ==========================================================

// --- 이하 다른 Composable 함수들 (FilterDropdownButton, SortBottomSheet, 등) ---
// --- (기존 코드와 동일) ---

@Composable
fun FilterDropdownButton(
    text: String,
    onClick: () -> Unit,
    isApplied: Boolean = false // isApplied 파라미터 추가
) {

    // isApplied 따라 색상 변경
    val borderColor = if (isApplied) BrandYellow else Color.LightGray
    val backgroundColor = if (isApplied) BrandYellow.copy(alpha = 0.1f) else Color.Transparent
    val textColor = if (isApplied) Color.Black else Color.Black.copy(alpha = 0.8f)
    val iconColor = if (isApplied) Color.Black else Color.Gray

    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            // 조건부 배경
            .background(backgroundColor, RoundedCornerShape(8.dp))
            // 조건부 테두리
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text,
            fontSize = 14.sp,
            maxLines = 1,
            color = textColor, // 조건부 텍스트 색
            fontWeight = if (isApplied) FontWeight.SemiBold else FontWeight.Normal // 조건부 굵기
        )
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = iconColor // 조건부 아이콘 색
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    sheetState: SheetState,
    currentSortOption: String,
    sortOptions: Map<String, String>,
    onSortSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .background(Color.LightGray, RoundedCornerShape(100))
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "정렬",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            sortOptions.forEach { (apiValue, displayName) ->
                val isSelected = currentSortOption == apiValue

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSortSelected(apiValue) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName,
                            fontSize = 16.sp,
                            color = if (isSelected) BrandYellow else Color.Black,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (displayName == "추천순") {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "정보",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFF76707)
                            )
                        }
                    }

                    if (isSelected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "선택됨",
                            tint = BrandYellow,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterBottomSheet(
    sheetState: SheetState,
    categories: List<CategoryEntity>,
    currentCategoryId: Int?,
    onDismissRequest: () -> Unit,
    onApplyClick: (CategoryEntity?) -> Unit,
    onResetClick: () -> Unit
) {

    var selectedCategory by remember(currentCategoryId) {
        mutableStateOf(categories.find { it.categoryId.toInt() == currentCategoryId })
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .background(Color.LightGray, RoundedCornerShape(100))
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "카테고리",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(
                    items = categories,
                    key = { it.categoryId }
                ) { category ->
                    val isSelected = selectedCategory?.categoryId == category.categoryId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {

                                selectedCategory = if (isSelected) null else category
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = {
                                selectedCategory = if (isSelected) null else category
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = BrandYellow
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = category.name, fontSize = 16.sp)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        selectedCategory = null
                        onResetClick()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray.copy(alpha = 0.5f),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("초기화", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        onApplyClick(selectedCategory)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("적용하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleBottomSheet(
    title: String,
    content: @Composable () -> Unit,
    sheetState: SheetState,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .background(Color.LightGray, RoundedCornerShape(100))
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            content()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceFilterBottomSheet(
    sheetState: SheetState,
    initialMinPrice: Int?,
    initialMaxPrice: Int?,
    onDismissRequest: () -> Unit,
    onApplyClick: (min: Int?, max: Int?) -> Unit
) {
    var minPrice by remember { mutableStateOf(initialMinPrice?.toString() ?: "") }
    var maxPrice by remember { mutableStateOf(initialMaxPrice?.toString() ?: "") }

    val minAsInt = minPrice.toIntOrNull()
    val maxAsInt = maxPrice.toIntOrNull()

    val isError = minAsInt != null && maxAsInt != null && minAsInt > maxAsInt

    val customTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color.Black,
        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
        errorBorderColor = MaterialTheme.colorScheme.error
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .background(Color.LightGray, RoundedCornerShape(100))
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "가격",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = minPrice,
                    onValueChange = { minPrice = it },
                    placeholder = { Text("최소 금액") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = customTextFieldColors,
                    isError = isError
                )
                Text(
                    text = "-",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                OutlinedTextField(
                    value = maxPrice,
                    onValueChange = { maxPrice = it },
                    placeholder = { Text("최대 금액") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = customTextFieldColors,
                    isError = isError
                )
            }

            if (isError) {
                Text(
                    text = "최소 금액이 최대 금액보다 높아요",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        minPrice = ""
                        maxPrice = ""
                        onApplyClick(null, null)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray.copy(alpha = 0.5f),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("초기화", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        onApplyClick(minAsInt, maxAsInt)
                        onDismissRequest() // 바텀시트 닫기
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isError,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("적용하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}