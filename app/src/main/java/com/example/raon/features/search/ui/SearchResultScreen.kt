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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

    // 첫 진입 시 searchQuery로 검색 시작
    LaunchedEffect(key1 = searchQuery) {
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
    // (API 요청값 <-> 표시 텍스트)
    val sortOptionsMap = remember {
        mapOf(
            "createdAt,desc" to "최신순",
            "viewCount,desc" to "조회수순", // (서버 명세 확인 필요)
            "price,asc" to "낮은 가격순",
            "price,desc" to "높은 가격순",
            "location,asc" to "가까운순"   // (서버 명세 확인 필요)
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
//                    onQueryChange = { newQuery = it },
                    onQueryChange = { newQuery -> searchViewModel.onQueryChanged(newQuery) },
                    false,

                    onSearch = { /* 검색 로직 */ },
                    onBackClick = {
                        /* 뒤로가기 */
                        onCloses()
                    },
                    onHomeClick = {
                        /* 홈으로 */
                        onNavigateToHome()
                    }
                )
            }
        },
    ) { paddingValues ->
        // Box 및 로딩/에러 UI 제거, 원래 Column 구조로 복귀
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
                categoryName = uiState.categoryName, // <--수정
                currentSortName = currentSortDisplayName, // <-- "최신순" 대신 전달
                onSortClick = { showSortBottomSheet = true },
                onCategoryClick = { showCategoryBottomSheet = true },
                onLocationClick = { showLocationBottomSheet = true },
                onPriceClick = { showPriceBottomSheet = true }
            )
            // ==========================================================
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))

            // 4. 공용 아이템 리스트 사용
            ItemListComponoents(
                items = coreItems,
                onItemClick = onItemClick
            )
        }
    }


    // 바텀시트는 Scaffold 외부에 둠 (전체 화면 덮기)
    // ==========================================================
    // 4. SortBottomSheet 호출부 수정
    if (showSortBottomSheet) {
        SortBottomSheet(
            sheetState = sortBottomSheetState,
            currentSortOption = uiState.sortOption, // 1. 현재 정렬 값(API) 전달
            sortOptions = sortOptionsMap,         // 2. 전체 맵 전달
            onSortSelected = { newSortValue ->
                // 3. 새 옵션 선택 시 뷰모델에 알림
                searchViewModel.onSortChanged(newSortValue)
                // 4. 바텀시트 닫기
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

    // 카테고리 바텀시트
    if (showCategoryBottomSheet) {
        CategoryFilterBottomSheet(
            sheetState = categoryBottomSheetState,
            categories = categories, // 카테고리 목록 전달
            currentCategoryId = uiState.categoryId, // 현재 ID 전달
            onDismissRequest = {
                scope.launch { categoryBottomSheetState.hide() }.invokeOnCompletion {
                    if (!categoryBottomSheetState.isVisible) showCategoryBottomSheet = false
                }
            },
            onApplyClick = { selectedCategory -> // 콜백 변경 (CategoryEntity? 반환)
                // 뷰모델 상태 업데이트
                searchViewModel.onCategoryChanged(
                    selectedCategory?.categoryId?.toInt(),
                    selectedCategory?.name
                )
                // 시트 닫기
                scope.launch { categoryBottomSheetState.hide() }.invokeOnCompletion {
                    if (!categoryBottomSheetState.isVisible) showCategoryBottomSheet = false
                }
            },
            onResetClick = {
                // 뷰모델 상태 초기화
                searchViewModel.onCategoryChanged(null, null)
                // 시트 닫기
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

    // 가격 필터 바텀시트 호출
    if (showPriceBottomSheet) {
        PriceFilterBottomSheet(
            sheetState = priceBottomSheetState,
            // 현재 가격 정보 전달
            initialMinPrice = uiState.minPrice,
            initialMaxPrice = uiState.maxPrice,
            onDismissRequest = {
                scope.launch { priceBottomSheetState.hide() }.invokeOnCompletion {
                    if (!priceBottomSheetState.isVisible) showPriceBottomSheet = false
                }
            },
            onApplyClick = { min, max ->
                searchViewModel.onPriceChanged(min, max)
            }
        )
    }
}


// ==========================================================
// FilterControls 함수 수정
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


// ProductList 관련 코드 삭제됨


// ==========================================================
// SortBottomSheet 함수 전체 수정
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    sheetState: SheetState,
    // 1. 파라미터 변경
    currentSortOption: String,      // 현재 API 값 (예: "price,asc")
    sortOptions: Map<String, String>, // 전체 정렬 맵
    onSortSelected: (String) -> Unit, // 콜백 (API 값 반환)
    onDismissRequest: () -> Unit
) {
    // 2. 로컬 상태 2줄 삭제

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

            // 3. 맵으로 루프
            sortOptions.forEach { (apiValue, displayName) ->
                // 4. isSelected 로직 변경
                val isSelected = currentSortOption == apiValue

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 5. 클릭 시 apiValue 콜백 전달
                        .clickable { onSortSelected(apiValue) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName, // 6. displayName 사용
                            fontSize = 16.sp,
                            // 7. isSelected 사용
                            color = if (isSelected) BrandYellow else Color.Black,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (displayName == "추천순") { // displayName으로 비교
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "정보",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFF76707)
                            )
                        }
                    }

                    // 8. isSelected 사용
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
// ==========================================================


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


// 초기 min/max 파라미터 추가
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceFilterBottomSheet(
    sheetState: SheetState,
    initialMinPrice: Int?,
    initialMaxPrice: Int?,
    onDismissRequest: () -> Unit,
    onApplyClick: (min: Int?, max: Int?) -> Unit
) {
    // 상태를 초기값으로 세팅 (null이면 빈 문자열)
    var minPrice by remember { mutableStateOf(initialMinPrice?.toString() ?: "") }
    var maxPrice by remember { mutableStateOf(initialMaxPrice?.toString() ?: "") }


    // 1. Int로 변환
    val minAsInt = minPrice.toIntOrNull()
    val maxAsInt = maxPrice.toIntOrNull()


    // 2. 실시간 에러 계산
    // (min > max 이면 에러)
    val isError = minAsInt != null && maxAsInt != null && minAsInt > maxAsInt

    // 텍스트필드 색상 정의
    val customTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color.Black, // 포커스 시 검은색
        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f), // 기본 상태 회색

        // 3. 에러 시 테두리 색
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
            // 1. 타이틀
            Text(
                text = "가격",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 2. 가격 입력 필드
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

                    // 4. 에러 상태 반영
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
                    // 4. 에러 상태 반영
                    isError = isError

                )
            }

            // 5. 에러 메시지 표시
            if (isError) {
                Text(
                    text = "최소 금액이 최대 금액보다 높아요", // (에러 텍스트)
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                )
            }

            // 3. 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 초기화 버튼
                Button(
                    onClick = {
                        minPrice = ""
                        maxPrice = ""
                        // 뷰모델 상태도 초기화
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
                // 적용하기 버튼
                Button(
                    onClick = {
                        val minAsInt = minPrice.toIntOrNull()
                        val maxAsInt = maxPrice.toIntOrNull()

                        onApplyClick(minAsInt, maxAsInt)

                        onDismissRequest() // 바텀시트 닫기
                    },
                    modifier = Modifier.weight(1f),
                    // 6. 에러 시 버튼 비활성화
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