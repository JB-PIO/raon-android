package com.example.raon.features.main.ui

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
import androidx.compose.foundation.layout.statusBarsPadding // 👈 [추가]
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainView(
    modifier: Modifier = Modifier,
    navController: NavController,
    mainViewModel: MainViewModel = hiltViewModel()  // MainViewModel
) {

    val userProfile by mainViewModel.userProfile.collectAsStateWithLifecycle()
    val fullAddress = userProfile?.address
    val mainAddressName = fullAddress?.split(" ")?.lastOrNull()

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
    // 이 Box가 그림자와 드롭다운을 포함하는 최상위 컨테이너가 됩니다.
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            // Scaffold 자체는 Box 안에서 전체를 채우기.
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
        //    이렇게 하면 Scaffold (TopBar, BottomBar 포함) 위에 그려집니다.
        LocationDropdownWithDimmingOnMain(
            isMenuOpen = isLocationMenuOpen,
            // UI 확인용 임시 데이터
            availableAddresses = listOf(
                LocationUiModel(id = 1, name = "서울특별시 강남구 대자동"),
                LocationUiModel(id = 2, name = "서울특별시 강남구 원종2동")
            ),
            selectedAddressName = fullAddress ?: "위치 없음",
            // innerPadding을 쓸 수 없으므로, 표준 TopAppBar 높이 56.dp를 사용합니다.
            //    (HomeScreenTopAppBar가 56.dp보다 크면 이 값만 조절하면 됩니다)
            topBarHeight = 56.dp,
            onAddressSelected = { location ->
                // mainViewModel.onAddressSelected(location) // (기능 연결 전 주석 처리)
                isLocationMenuOpen = false
            },
            onNavigateToAddAddress = {
                navController.navigate("addressInput")
                isLocationMenuOpen = false
            },
            onDismiss = { isLocationMenuOpen = false }
        )
    } //
}


// ----------------------------------------------------------------------
// 부속 컴포저블
// ----------------------------------------------------------------------

@Composable
fun LocationDropdownWithDimmingOnMain(
    isMenuOpen: Boolean,
    availableAddresses: List<LocationUiModel>,
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
            // 주소 목록
            availableAddresses.forEach { location ->
                LocationMenuItem(
                    text = location.name,
                    isSelected = location.name == selectedAddressName
                ) {
                    onAddressSelected(location)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // 내 동네 설정
            LocationMenuItem(
                text = "내 동네 설정",
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
        text = text.split(" ").lastOrNull() ?: text, // 마지막 "동" 이름만 표시
        style = MaterialTheme.typography.titleMedium,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}