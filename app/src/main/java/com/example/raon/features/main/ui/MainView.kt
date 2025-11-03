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
// [삭제] import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
// [삭제] import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
// [삭제] import androidx.lifecycle.Lifecycle
// [삭제] import androidx.lifecycle.LifecycleEventObserver
// [삭제] import androidx.lifecycle.LifecycleOwner
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

    // 🔽🔽🔽 [삭제] 🔽🔽🔽
    // STOMP 연결/해제 로직은 ChatService가 담당하므로
    // View의 생명주기에 맞춰 연결/해제하던 이 블록 전체가 필요 없어졌습니다.
    /*
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mainViewModel.connectToStomp() // <--- 이 함수가 더 이상 존재하지 않음
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mainViewModel.disconnectFromStomp() // <--- 이 함수가 더 이상 존재하지 않음
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    */
    // 🔼🔼🔼 [삭제] 🔼🔼🔼


    // ------------------ [유지] ------------------
    // 알림 권한 요청은 ChatService가 알림을 띄우기 위해
    // 여전히 필요하므로 유지합니다.
    RequestNotificationPermission()
    // ---------------------------------------------------


    // [유지] ViewModel에서 실제 데이터 가져오기
    val userProfile by mainViewModel.userProfile.collectAsStateWithLifecycle()
    val favoriteLocations by mainViewModel.favoriteLocations.collectAsStateWithLifecycle()

    val fullAddress = userProfile?.address
    val mainAddressName = fullAddress?.split(" ")?.lastOrNull()

    // [유지] 현재 위치를 LocationUiModel로 변환
    val currentLocation = userProfile?.let {
        LocationUiModel(id = it.locationId, name = it.address)
    }

    val mainUiState by mainViewModel.uiState.collectAsState() // [수정] collectAsStateWithLifecycle() 권장
    val bottomNavController = rememberNavController()

    var isLocationMenuOpen by remember { mutableStateOf(false) }

    val navItemList = listOf(
        NavItem("홈", Icons.Default.Home, "home"),
        NavItem("검색", Icons.Default.Search, "searchInput"),
        NavItem("등록", Icons.Default.AddCircle, "addItem"),
        NavItem("채팅", Icons.Default.Send, "chatRoomList"),
        NavItem("프로필", Icons.Default.Person, "profile"),
    )

    // (이하 Scaffold 및 NavHost 코드는 모두 동일하게 유지)
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
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
            NavHost(
                navController = bottomNavController,
                startDestination = navItemList[0].route,
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

        // (드롭다운 로직도 동일하게 유지)
        LocationDropdownWithDimmingOnMain(
            isMenuOpen = isLocationMenuOpen,
            availableAddresses = favoriteLocations,
            currentLocation = currentLocation,
            selectedAddressName = fullAddress ?: "위치 없음",
            topBarHeight = 56.dp,
            onAddressSelected = { location ->
                mainViewModel.selectNewMainLocation(location)
                isLocationMenuOpen = false
            },
            onNavigateToAddAddress = {
                navController.navigate("location?mode=favorite")
                isLocationMenuOpen = false
            },
            onDismiss = { isLocationMenuOpen = false }
        )
    }
}


// ----------------------------------------------------------------------
// 부속 컴포저블 (변경 없음)
// ----------------------------------------------------------------------

@Composable
fun LocationDropdownWithDimmingOnMain(
    isMenuOpen: Boolean,
    availableAddresses: List<LocationUiModel>,
    currentLocation: LocationUiModel?,
    selectedAddressName: String,
    topBarHeight: Dp,
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
            .fillMaxSize()
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
            .zIndex(11f)
            .statusBarsPadding()
            .padding(start = 16.dp, top = topBarHeight)
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .padding(vertical = 4.dp)
        ) {
            // 1. 즐겨찾기 목록
            availableAddresses.forEach { location ->
                LocationMenuItem(
                    text = location.name.split(" ").lastOrNull() ?: location.name,
                    isSelected = location.name == selectedAddressName
                ) {
                    onAddressSelected(location)
                }
            }

            // 2. 현재 위치
            currentLocation?.let {
                val isAlreadyInFavorites = availableAddresses.any { it.id == currentLocation.id }

                if (!isAlreadyInFavorites) {
                    if (availableAddresses.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    LocationMenuItem(
                        text = it.name.split(" ").lastOrNull() ?: it.name,
                        isSelected = it.name == selectedAddressName
                    ) {
                        onAddressSelected(it)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // 내 동네 설정
            LocationMenuItem(
                text = "주소 추가",
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
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) BrandDarkText else Color.Gray,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}


// (알림 권한 요청 로직도 동일하게 유지)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

        val notificationPermissionState = rememberPermissionState(
            permission = Manifest.permission.POST_NOTIFICATIONS
        )

        LaunchedEffect(Unit) {
            when {
                !notificationPermissionState.status.isGranted -> {
                    notificationPermissionState.launchPermissionRequest()
                }
            }
        }
    }
}