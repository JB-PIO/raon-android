package com.example.raon.features.search.ui


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
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.raon.features.search.ui.model.SearchItemUiModel
import com.example.raon.ui.theme.BrandYellow
import kotlinx.coroutines.launch


// 화면 콘텐츠
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultScreen(
    modifier: Modifier = Modifier,
    searchQuery: String,
    searchViewModel: SearchResultViewModel = hiltViewModel(),
    onItemClick: (Int) -> Unit = {},     // onItemClick, 기본값으로 빈 함수
    onCloses: () -> Unit = {},  // 닫기 이벤트
    onNavigateToHome: () -> Unit = {}   // 홈가기 이벤트

) {

    // viewModel의 uistate 구독
    val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()

    // 화면이 처음 나타날 때 전달받은 searchQuery로 검색 시작
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
            // uiState의 가격 정보를 FilterControls로 전달
            FilterControls(
                minPrice = uiState.minPrice,
                maxPrice = uiState.maxPrice,
                onSortClick = { showSortBottomSheet = true },
                onCategoryClick = { showCategoryBottomSheet = true },
                onLocationClick = { showLocationBottomSheet = true },
                onPriceClick = { showPriceBottomSheet = true }
            )
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
            // 임시 데이터인 productList를 전달
            ProductList(
                products = uiState.products,
                onItemClick = onItemClick
            )
        }
    }


    // 바텀 시트들은 Scaffold 외부에 두어 전체 화면을 덮도록 합니다.
    if (showSortBottomSheet) {
        SortBottomSheet(
            sheetState = sortBottomSheetState,
            onDismissRequest = {
                scope.launch { sortBottomSheetState.hide() }.invokeOnCompletion {
                    if (!sortBottomSheetState.isVisible) showSortBottomSheet = false
                }
            }
        )
    }

    if (showCategoryBottomSheet) {
        CategoryFilterBottomSheet(
            sheetState = categoryBottomSheetState,
            onDismissRequest = {
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

    // PriceFilterBottomSheet 호출
    if (showPriceBottomSheet) {
        PriceFilterBottomSheet(
            sheetState = priceBottomSheetState,
            // 수정됨: 현재 가격 정보를 바텀시트로 전달
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


// 수정됨: minPrice, maxPrice 파라미터 추가
@Composable
fun FilterControls(
    minPrice: Int?,
    maxPrice: Int?,
    onSortClick: () -> Unit,
    onLocationClick: () -> Unit,
    onPriceClick: () -> Unit,
    onCategoryClick: () -> Unit
) {
    var isChecked by remember { mutableStateOf(true) }

    // 가격 범위에 따라 표시될 텍스트 생성
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

    // 가격 필터가 적용되었는지 확인하는 변수
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
            FilterDropdownButton(text = "최신순", onClick = onSortClick)
            FilterDropdownButton(text = "위치 정렬", onClick = onLocationClick) // 요청에 따라 텍스트 변경

            // "가격" 대신 동적으로 생성된 priceText 사용
            //  가격 버튼에 isApplied 값을 전달
            FilterDropdownButton(
                text = priceText,
                onClick = onPriceClick,
                isApplied = isPriceFilterApplied // <-- 여기를 수정
            )

            FilterDropdownButton(text = "카테고리", onClick = onCategoryClick)
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


@Composable
fun FilterDropdownButton(
    text: String,
    onClick: () -> Unit,
    isApplied: Boolean = false // "적용됨" 상태를 받는 파라미터 추가
) {

    // isApplied 값에 따라 색상 결정
    val borderColor = if (isApplied) BrandYellow else Color.LightGray
    val backgroundColor = if (isApplied) BrandYellow.copy(alpha = 0.1f) else Color.Transparent
    val textColor = if (isApplied) Color.Black else Color.Black.copy(alpha = 0.8f)
    val iconColor = if (isApplied) Color.Black else Color.Gray

    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            // 조건부 배경색 적용
            .background(backgroundColor, RoundedCornerShape(8.dp))
            // 조건부 테두리색 적용
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text,
            fontSize = 14.sp,
            maxLines = 1,
            color = textColor, // 조건부 텍스트 색상
            fontWeight = if (isApplied) FontWeight.SemiBold else FontWeight.Normal // 조건부 굵기
        )
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = iconColor // 조건부 아이콘 색상
        )
    }
}


@Composable
fun ProductList(
    products: List<SearchItemUiModel>,
    onItemClick: (Int) -> Unit
) {
    LazyColumn {
        items(
            items = products,
            key = { it.id }
        ) { product ->
            ProductListItem(
                item = product,
                onClick = { onItemClick(product.id) }
            )
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
        }
    }
}

@Composable
fun ProductListItem(
    item: SearchItemUiModel,
    onClick: () -> Unit
) {


    // 마지막 동만 추출한 텍스트
    val lastlocation = item.location.split(" ").lastOrNull()


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = item.title,
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.height(100.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${lastlocation}",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                Text(
                    text = "${item.timeAgo}",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "%,d원".format(item.price),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
            if (item.comments > 0 || item.likes > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.comments > 0) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "댓글",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = item.comments.toString(), fontSize = 13.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (item.likes > 0) {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = "좋아요",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = item.likes.toString(), fontSize = 13.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit
) {
    val sortOptions = listOf("추천순", "최신순", "낮은 가격순", "높은 가격순", "가까운순")
    var selectedSortOption by remember { mutableStateOf("추천순") }

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

            sortOptions.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedSortOption = option }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = option,
                            fontSize = 16.sp,
                            color = if (selectedSortOption == option) MaterialTheme.colorScheme.primary else Color.Black,
                            fontWeight = if (selectedSortOption == option) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (option == "추천순") {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "정보",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFF76707)
                            )
                        }
                    }

                    if (selectedSortOption == option) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "선택됨",
                            tint = MaterialTheme.colorScheme.primary,
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
    onDismissRequest: () -> Unit
) {
    val categories = listOf(
        "디지털기기", "생활가전", "가구/인테리어", "유아동", "유아도서", "생활/주방",
        "여성의류", "남성패션/잡화", "뷰티/미용", "스포츠/레저", "취미/게임/음반", "도서",
        "티켓/교환권", "가공식품", "반려동물용품", "식물", "기타", "삽니다"
    )
    val selectedCategories = remember { mutableStateListOf<String>() }

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
                items(categories) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedCategories.contains(category)) {
                                    selectedCategories.remove(category)
                                } else {
                                    selectedCategories.add(category)
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedCategories.contains(category),
                            onCheckedChange = { isChecked ->
                                if (isChecked) selectedCategories.add(category)
                                else selectedCategories.remove(category)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = category, fontSize = 16.sp)
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
                    onClick = { selectedCategories.clear() },
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
                        onDismissRequest()
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


// initialMinPrice, initialMaxPrice 파라미터 추가
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceFilterBottomSheet(
    sheetState: SheetState,
    initialMinPrice: Int?,
    initialMaxPrice: Int?,
    onDismissRequest: () -> Unit,
    onApplyClick: (min: Int?, max: Int?) -> Unit
) {
    // 상태를 initial 값으로 초기화 (null이면 빈 문자열)
    var minPrice by remember { mutableStateOf(initialMinPrice?.toString() ?: "") }
    var maxPrice by remember { mutableStateOf(initialMaxPrice?.toString() ?: "") }


    // 1. 입력값을 Int로 즉시 변환
    val minAsInt = minPrice.toIntOrNull()
    val maxAsInt = maxPrice.toIntOrNull()


    // 2. 실시간으로 에러 상태 계산
    // (두 값이 모두 null이 아니고, 최소값이 최대값보다 클 때)
    val isError = minAsInt != null && maxAsInt != null && minAsInt > maxAsInt

    // 텍스트 필드 색상 정의
    val customTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color.Black, // 터치(포커스) 시 검은색
        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f), // 기본 상태 회색

        // 3. 에러 상태일 때의 테두리 색상 (기본 MaterialTheme 색상 사용)
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

                    // 4. 실시간 에러 상태를 TextField에 반영
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
// 4. 실시간 에러 상태를 TextField에 반영
                    isError = isError

                )
            }

            // 5. 에러 메시지를 isError 값에 따라 표시
            if (isError) {
                Text(
                    text = "최소 금액이 최대 금액보다 높아요", // 이미지와 동일한 텍스트
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
                        // 추가됨: ViewModel 상태도 함께 초기화
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

                        onDismissRequest() // 바텀 시트 닫기
                    },
                    modifier = Modifier.weight(1f),
                    //  6. isError가 true이면 버튼 비활성화
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