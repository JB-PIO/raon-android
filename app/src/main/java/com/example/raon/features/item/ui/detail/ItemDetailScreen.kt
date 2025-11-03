package com.example.raon.features.item.ui.detail

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.raon.R
import com.example.raon.core.common.AppConstants
import com.example.raon.ui.theme.BrandDarkText
import com.example.raon.ui.theme.BrandYellow
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    viewModel: ItemDetailViewModel = hiltViewModel(),
    shouldRefresh: Boolean,
    onRefreshDone: () -> Unit,
    onBackClick: (isFavorite: Boolean) -> Unit,
    onNavigateToChatRoom: (Long) -> Unit,
    onNavigateToEdit: (itemId: Int) -> Unit
) {


    // 3. shouldRefresh 값이 true일 때만 이 효과가 실행됩니다.
    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            // ViewModel에 "새로고침 해줘!" 라고 이벤트를 보냅니다.
            viewModel.onEvent(ItemDetailEvent.Refresh)
            // "새로고침 명령 내렸으니 표식은 지워줘" 라고 NavHost에 알립니다.
            onRefreshDone()
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showNotFoundErrorDialog by remember { mutableStateOf(false) }

    var showFullScreenImage by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }


    val isMine = uiState.item?.isMine ?: false

    //  BackHandler를 추가하여 시스템 뒤로가기 동작을 처리합니다.
    BackHandler {
        // TopAppBar의 뒤로가기 아이콘을 눌렀을 때와 동일한 로직을 실행합니다.
        val currentFavoriteState = uiState.item?.isFavorite ?: false
        onBackClick(currentFavoriteState)
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ItemDetailEvent.NavigateToChatRoom -> onNavigateToChatRoom(event.chatId)
                is ItemDetailEvent.ProductDeleted -> {
                    Toast.makeText(context, "상품이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    onBackClick(false)
                }

                is ItemDetailEvent.ShowProductNotFoundError -> showNotFoundErrorDialog = true
                is ItemDetailEvent.ShowError -> Toast.makeText(
                    context,
                    event.message,
                    Toast.LENGTH_SHORT
                ).show()

                // 이 부분을 추가하여 Refresh 이벤트를 처리합니다.
                // 이 블록에서는 Refresh 이벤트를 받을 일이 없으므로 아무것도 하지 않습니다.
                is ItemDetailEvent.Refresh -> { /* Do nothing */
                }

                // [추가] 새로운 이벤트에 대한 처리입니다.
                // 이 스크린에서는 네비게이션만 하면 되므로, 아무것도 하지 않습니다.
                is ItemDetailEvent.ChatRoomCreated -> { /* Do nothing, handled by NavHost */
                } //
            }
        }
    }

    if (showFullScreenImage && selectedImageIndex != null && uiState.item != null) {
        FullScreenImageViewer(
            imageUrls = uiState.item!!.imageUrls,
            initialPage = selectedImageIndex!!,
            onDismiss = { showFullScreenImage = false }
        )
    }

    if (showNotFoundErrorDialog) {
        AlertDialog(
            onDismissRequest = {
                showNotFoundErrorDialog = false
                onBackClick(uiState.item?.isFavorite ?: false)
            },
            title = { Text("알림") },
            text = { Text("존재하지 않는 상품이거나 삭제되었습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    showNotFoundErrorDialog = false
                    onBackClick(uiState.item?.isFavorite ?: false)
                }) { Text("확인") }
            }
        )
    }

    if (sheetState.isVisible) {
        ModalBottomSheet(
            onDismissRequest = { scope.launch { sheetState.hide() } },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                ListItem(
                    headlineContent = { Text("수정", fontWeight = FontWeight.Medium) },
                    leadingContent = { Icon(Icons.Filled.Edit, contentDescription = "수정") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            uiState.item?.id?.let { itemId ->
                                onNavigateToEdit(itemId)
                            }
                            scope.launch { sheetState.hide() }
                        }
                )
//                Divider()
//                ListItem(
//                    headlineContent = { Text("상태 변경", fontWeight = FontWeight.Medium) },
//                    leadingContent = { Icon(Icons.Filled.SwapHoriz, contentDescription = "상태 변경") },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .clickable {
//                            Toast.makeText(context, "상태 변경", Toast.LENGTH_SHORT).show()
//                            scope.launch { sheetState.hide() }
//                        }
//                )
                Divider()
                ListItem(
                    headlineContent = {
                        Text(
                            "삭제",
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "삭제",
                            tint = Color.Red
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { sheetState.hide() }
                            showDeleteDialog = true
                        }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { scope.launch { sheetState.hide() } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray.copy(alpha = 0.5f),
                        contentColor = Color.Black
                    )
                ) { Text("닫기", fontWeight = FontWeight.Bold) }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("게시글을 삭제할까요?", fontWeight = FontWeight.Bold) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandYellow)
                ) { Text("삭제", color = BrandDarkText) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(
                        "취소",
                        color = BrandDarkText
                    )
                }
            }
        )
    }

    Scaffold(
        topBar = {
            ProductDetailTopAppBar(
                onBackClick = {
                    val currentFavoriteState = uiState.item?.isFavorite ?: false
                    onBackClick(currentFavoriteState)
                },
                isMine = isMine,
                onMoreClick = { scope.launch { sheetState.show() } }
            )
        },
        bottomBar = {
            if (!isMine) {
                uiState.item?.let { item ->
                    ProductBottomBar(
                        isFavorited = item.isFavorite,
                        onFavoriteClick = viewModel::onFavoriteButtonClicked,
                        onChatClick = viewModel::onChatButtonClicked
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.errorMessage != null -> Text(
                    uiState.errorMessage!!,
                    modifier = Modifier.align(Alignment.Center)
                )

                uiState.item != null -> {
                    val item = uiState.item!!
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            ProductImagePager(
                                imageUrls = item.imageUrls,
                                onImageClick = { index ->
                                    selectedImageIndex = index
                                    showFullScreenImage = true
                                }
                            )
                        }
                        item {
                            SellerProfile(
                                nickname = item.sellerNickname,
                                profileUrl = uiState.viewableSellerProfileImageUrl,
                                address = item.sellerAddress
                            )
                        }
                        item { Divider(color = Color.LightGray.copy(alpha = 0.5f)) }
                        item {
                            ProductInfo(
                                title = item.title,
                                price = item.price,
                                condition = item.condition,
                                category = item.category,
                                time = item.createdAt,
                                description = item.description,
                                stats = "관심 ${item.favoriteCount} · 조회 ${item.viewCount}"
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FullScreenImageViewer(
    imageUrls: List<String>,
    initialPage: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { imageUrls.size }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                AsyncImage(
                    model = imageUrls[pageIndex],
                    contentDescription = "전체 화면 이미지 ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "닫기",
                    tint = Color.White
                )
            }

            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val color = if (pagerState.currentPage == iteration) Color.White else Color.Gray
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDetailTopAppBar(
    onBackClick: () -> Unit,
    isMine: Boolean,
    onMoreClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            if (isMine) Text("내 상품", fontWeight = FontWeight.Bold)
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
            }
        },
        actions = {
            if (isMine) {
                IconButton(onClick = onMoreClick) {
                    Icon(Icons.Default.MoreVert, contentDescription = "더보기")
                }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductImagePager(imageUrls: List<String>, onImageClick: (Int) -> Unit) {
    if (imageUrls.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) { Text("이미지가 없습니다.") }
        return
    }
    val pagerState = rememberPagerState(pageCount = { imageUrls.size })
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) { pageIndex ->
            AsyncImage(
                model = imageUrls[pageIndex],
                contentDescription = "상품 이미지 ${pageIndex + 1}",
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onImageClick(pageIndex) },
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
            repeat(pagerState.pageCount) { iteration ->
                val color =
                    if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }
    }
}

@Composable
private fun SellerProfile(nickname: String, profileUrl: String?, address: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val imageModel: Any =
            if (profileUrl.isNullOrEmpty() || profileUrl == AppConstants.DEFAULT_PROFILE_URL) {
                R.drawable.user_icon
            } else {
                profileUrl
            }

        AsyncImage(
            model = imageModel,
            contentDescription = "판매자 프로필 사진",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(nickname, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(address, color = Color.Gray, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ProductInfo(
    title: String,
    price: Int,
    condition: String,
    category: String,
    time: String,
    description: String,
    stats: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("%,d원".format(price), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(category, color = Color.Gray, fontSize = 13.sp)
        Text(condition, color = Color.Gray, fontSize = 13.sp)
        Text(time, color = Color.Gray, fontSize = 13.sp)
        Text(description, fontSize = 16.sp, lineHeight = 24.sp)
        Text(stats, color = Color.Gray, fontSize = 13.sp)
    }
}

@Composable
private fun ProductBottomBar(
    isFavorited: Boolean,
    onFavoriteClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Surface(shadowElevation = 8.dp) {
        Column {
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "관심",
                        tint = if (isFavorited) Color.Red else Color.Unspecified,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = onChatClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandYellow,
                        contentColor = BrandDarkText
                    )
                ) { Text("채팅하기") }
            }
        }
    }
}