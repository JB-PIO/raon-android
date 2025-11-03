package com.example.raon.features.main.ui

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect // 🔽🔽🔽 [필수 Import]
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner // 🔽🔽🔽 [필수 Import]
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle // 🔽🔽🔽 [필수 Import]
import androidx.lifecycle.LifecycleEventObserver // 🔽🔽🔽 [필수 Import]
import androidx.lifecycle.LifecycleOwner // 🔽🔽🔽 [필수 Import]
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.raon.features.chat.ui.ChatListScreen
import com.example.raon.features.chat.ui.ChatListTopAppBar
import com.example.raon.features.item.ui.list.HomeScreenTopAppBar
import com.example.raon.features.item.ui.list.ItemListScreen
import com.example.raon.features.item.ui.list.LocationUiModel
import com.example.raon.features.user.ui.ProfileScreen
import com.example.raon.features.user.ui.ProfileTopAppBar
import com.example.raon.navigation.NavItem
import com.example.raon.ui.theme.BrandDarkText
import com.example.raon.ui.theme.BrandYellow
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainView(
    modifier: Modifier = Modifier,
    navController: NavController,
    mainViewModel: MainViewModel = hiltViewModel()  // MainViewModel
) {

    // 🔽🔽🔽 [필수 추가] 생명주기 감지 및 STOMP 연결/해제 🔽🔽🔽
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                // 앱이 화면에 다시 보일 때 (홈 화면에서 복귀 등)
                Lifecycle.Event.ON_RESUME -> {
                    mainViewModel.connectToStomp()
                }
                // 앱이 배경으로 사라질 때 (홈 화면 누름 등)
                Lifecycle.Event.ON_PAUSE -> {
                    mainViewModel.disconnectFromStomp()
                }

                else -> {} // ON_CREATE, ON_START, ON_STOP, ON_DESTROY 등
            }
        }

        // 옵저버 추가
        lifecycleOwner.lifecycle.addObserver(observer)

        // Composable이 화면에서 사라질 때 옵저버 제거
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    // 🔼🔼🔼 여기까지 추가 🔼🔼🔼


    // ------------------ [이 부분 추가] ------------------
    // MainView가 Composable 트리에 들어오는 순간 권한 요청 로직 실행
    RequestNotificationPermission()
    // ---------------------------------------------------


    // [수정] ViewModel에서 실제 데이터 가져오기
    val userProfile by mainViewModel.userProfile.collectAsStateWithLifecycle()
    val favoriteLocations by mainViewModel.favoriteLocations.collectAsStateWithLifecycle()

    val fullAddress = userProfile?.address
    val mainAddressName = fullAddress?.split(" ")?.lastOrNull()

    // [추가] 현재 위치를 LocationUiModel로 변환
    val currentLocation = userProfile?.let {
        LocationUiModel(id = it.locationId, name = it.address)
    }

    val mainUiState by mainViewModel.uiState.collectAsState()
    val bottomNavController = rememberNavController()

    var isLocationMenuOpen by remember { mutableStateOf(false) }

    val navItemList = listOf(
        NavItem("홈", Icons.Default.Home, "home"),
        NavItem("검색", Icons.Default.Search, "searchInput"),
        NavItem("등록", Icons.Default.AddCircle, "addItem"),
        NavItem("채팅", Icons.Default.Send, "chatRoomList"),
        NavItem("프로필", Icons.Default.Person, "profile"),
    )

    // Scaffold를 Box로 감싸기
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            // Scaffold 자체는 Box 안에서 전체를 채기.
            modifier = Modifier.fillMaxSize(),
            topBar = {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                when (currentRoute) {
                    "home" -> HomeScreenTopAppBar(
                        onNavigateToSearch = { navController.navigate("searchInput") {} },
                        address = mainAddressName ?: "위치 없음",
                        onLocationClick = { isLocationMenuOpen = true },
                    )

                    "chatRoomList" -> ChatListTopAppBar(navController)
                    "profile" -> ProfileTopAppBar(navController)
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    modifier = Modifier.height(80.dp)
                ) {
                    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    navItemList.forEach { navItem ->
                        NavigationBarItem(
                            selected = currentRoute == navItem.route,
                            onClick = {
                                if (navItem.route == "addItem") {
                                    navController.navigate("addItem")
                                } else if (navItem.route == "searchInput") {
                                    navController.navigate("searchInput")
                                } else {
                                    bottomNavController.navigate(navItem.route) {
                                        popUpTo(bottomNavController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = navItem.icon,
                                    contentDescription = "Icon"
                                )
                            },
                            label = { Text(text = navItem.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandDarkText,
                                unselectedIconColor = Color(0xFF9E9E9E),
                                indicatorColor = BrandYellow,
                                selectedTextColor = BrandDarkText,
                                unselectedTextColor = Color(0xFF9E9E9E)
                            )
                        )
                    }
                }
            }

        ) { innerPadding ->
            // NavHost는 Scaffold의 content 영역에만 배치하기
            NavHost(
                navController = bottomNavController,
                startDestination = navItemList[0].route,
                // NavHost가 innerPadding을 적용하여 Top/Bottom Bar 영역을 피합니다.
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                composable("home") {
                    ItemListScreen(
                        onNavigateToSearch = {
                            navController.navigate("searchScreen")
                        },
                        onItemClick = { itemId ->
                            navController.navigate("itemDetail/$itemId")
                        },
                        onNavigateToAddAddress = {
                            navController.navigate("addressInput")
                        },
                        onLocationClick = { isLocationMenuOpen = true }
                    )
                }
                composable("chatRoomList") {
                    ChatListScreen(
                        onChatRoomClick = { chatRoomId, opponentId, itemId ->
                            // [수정] AppNavigation.kt의 정의와 일치하도록 수정
                            navController.navigate("chatRoom/$chatRoomId")
                            Log.d(
                                "ChatClick",
                                "MainView -> Navigating to chatRoom: ID=$chatRoomId"
                            )
                        },
                        myUserId = userProfile?.userId ?: -1,
                        chatRooms = mainUiState.chatRooms
                    )
                }
                composable("profile") {
                    ProfileScreen(
                        onNavigateToProfileEditScreen = {
                            navController.navigate("profileEdit")
                        },
                        onNavigateToSalesHistoryScreen = {
                            navController.navigate("salesHistory")
                        },
                        onNavigateToFavoritesScreen = {
                            navController.navigate("favorites")
                        },
                        navController = navController
                    )
                }
            }
        }

        // 드롭다운과 그림자를 Scaffold의 *형제*로 뺍니다.
        LocationDropdownWithDimmingOnMain(
            isMenuOpen = isLocationMenuOpen,
            availableAddresses = favoriteLocations, // 👈 [수정] ViewModel의 실제 즐겨찾기 목록
            currentLocation = currentLocation, // 👈 [수정] ViewModel의 실제 현재 위치
            selectedAddressName = fullAddress ?: "위치 없음", // 👈 [수정] ViewModel의 실제 주소
            topBarHeight = 56.dp,
            onAddressSelected = { location ->
                mainViewModel.selectNewMainLocation(location) // 👈 [수정] ViewModel 함수 호출
                isLocationMenuOpen = false
            },
            onNavigateToAddAddress = {
                // [수정] 올바른 경로("location")와 "favorite" 모드로 호출
                navController.navigate("location?mode=favorite")
                isLocationMenuOpen = false
            },
            onDismiss = { isLocationMenuOpen = false }
        )
    }
}


// ----------------------------------------------------------------------
// 부속 컴포저블
// ----------------------------------------------------------------------

@Composable
fun LocationDropdownWithDimmingOnMain(
    isMenuOpen: Boolean,
    availableAddresses: List<LocationUiModel>,
    currentLocation: LocationUiModel?, // 👈 [수정] 현재 위치 추가
    selectedAddressName: String,
    topBarHeight: Dp, // 고정 Dp 또는 동적 Dp를 받음
    onAddressSelected: (LocationUiModel) -> Unit,
    onNavigateToAddAddress: () -> Unit,
    onDismiss: () -> Unit
) {
    // === 1. 어두운 배경 (그림자) 레이어 ===
    AnimatedVisibility(
        visible = isMenuOpen,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .fillMaxSize() // 이제 이 fillMaxSize가 진짜 화면 전체를 덮습니다.
            .zIndex(10f)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    onClick = onDismiss,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
        )
    }

    // === 2. 커스텀 드롭다운 메뉴 레이어 ===
    AnimatedVisibility(
        visible = isMenuOpen,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .zIndex(11f) // 그림자 위에 위치
            .statusBarsPadding() // 상단 상태바 영역을 피합니다.
            .padding(start = 16.dp, top = topBarHeight) // 상태바 아래 + TopAppBar 높이만큼 띄웁니다.
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .padding(vertical = 4.dp)
        ) {
            // [수정] 1. 즐겨찾기 목록 (최대 2개)
            availableAddresses.forEach { location ->
                LocationMenuItem(
                    // 👈 [수정] 분리 로직을 여기서 적용
                    text = location.name.split(" ").lastOrNull() ?: location.name,
                    isSelected = location.name == selectedAddressName
                ) {
                    onAddressSelected(location)
                }
            }

            // [수정] 2. 현재 위치 (즐겨찾기에 없는 경우에만 표시)
            currentLocation?.let {
                // ID로 비교하여 즐겨찾기에 이미 있는지 확인
                val isAlreadyInFavorites = availableAddresses.any { it.id == currentLocation.id }

                if (!isAlreadyInFavorites) {
                    // 즐겨찾기가 1개라도 있으면 구분선 추가
                    if (availableAddresses.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    LocationMenuItem(
                        // 👈 [수정] 분리 로직을 여기서 적용
                        text = it.name.split(" ").lastOrNull() ?: it.name,
                        isSelected = it.name == selectedAddressName
                    ) {
                        onAddressSelected(it) // 동일한 콜백 사용
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // 내 동네 설정
            LocationMenuItem(
                text = "주소 추가", // [수정] 여기는 분리 로직을 적용 안 함
                isSelected = false
            ) {
                onNavigateToAddAddress()
            }
        }
    }
}


@Composable
fun LocationMenuItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = text, // [수정] 분리 로직을 여기서 제거
        style = MaterialTheme.typography.titleMedium,
        // [수정] 요청하신 대로 색상과 굵기 변경
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) BrandDarkText else Color.Gray,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}


// 알림 권한 물어보기 UI
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestNotificationPermission() {
    // Android 13 (API 33) 이상에서만 POST_NOTIFICATIONS 권한 필요
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

        // 1. 권한 상태를 관리하는 객체를 가져옵니다. (주석 해제)
        val notificationPermissionState = rememberPermissionState(
            permission = Manifest.permission.POST_NOTIFICATIONS
        )

        // 2. MainView가 로드될 때 (단 한 번) 실행합니다. (주석 해제)
        LaunchedEffect(Unit) {
            when {
                // 권한이 부여되지 않은 상태에서만 요청
                !notificationPermissionState.status.isGranted -> {
                    // 권한 요청 다이얼로그를 띄웁니다.
                    notificationPermissionState.launchPermissionRequest()
                }
            }
        }
    }
}