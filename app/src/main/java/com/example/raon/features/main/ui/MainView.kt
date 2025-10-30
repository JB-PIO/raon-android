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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.example.raon.features.user.ui.ProfileScreen
import com.example.raon.features.user.ui.ProfileTopAppBar
import com.example.raon.navigation.NavItem
import com.example.raon.ui.theme.BrandYellow

// comp 자동 완성 키워드
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainView(
    modifier: Modifier = Modifier,
    navController: NavController,
    mainViewModel: MainViewModel = hiltViewModel()  // MainViewModel 실
) {

    // 구독 시작 코드
    val userProfile by mainViewModel.userProfile.collectAsStateWithLifecycle()
    val mainaddress = userProfile?.address?.split(" ")?.lastOrNull()    // -> 풀 주소의 마지막 동만 가져오기

    val mainUiState by mainViewModel.uiState.collectAsState()
    val bottomNavController = rememberNavController()

    // [추가] MainView에서 드롭다운 메뉴 열림 상태를 관리합니다. (화면 전체 그림자용)
    var isLocationMenuOpen by remember { mutableStateOf(false) }


    // 불변 List 자료구조 사용
    val navItemList = listOf(
        NavItem("홈", Icons.Default.Home, "home"),
        NavItem("검색", Icons.Default.Search, "searchInput"),
        NavItem("등록", Icons.Default.AddCircle, "addItem"),
        NavItem("채팅", Icons.Default.Send, "chatRoomList"),
        NavItem("프로필", Icons.Default.Person, "profile"),
    )

    var selectedIndex by remember {
        mutableStateOf(0)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            when (currentRoute) {
                "home" -> HomeScreenTopAppBar(
                    onNavigateToSearch = { navController.navigate("searchInput") {} },
                    address = mainaddress ?: "내 주소",
                    // [수정] MainView의 상태를 변경하는 이벤트를 전달하여 메뉴 열기
                    onLocationClick = { isLocationMenuOpen = true },
                )

                "chatRoomList" -> ChatListTopAppBar(navController)
                "profile" -> ProfileTopAppBar(navController)
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                modifier = Modifier.height(115.dp)
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
                            Icon(imageVector = navItem.icon, contentDescription = "Icon")
                        },
                        label = {
                            Text(text = navItem.label)

                        },
                        colors = NavigationBarItemDefaults.colors(
                            // 선택된 상태의 아이콘 색상 (Text 색상도 함께 적용됨)
                            selectedIconColor = Color.Red,
                            // 선택되지 않은 상태의 아이콘 색상
                            unselectedIconColor = Color.Gray,
                            // 선택된 상태의 배경색
                            indicatorColor = BrandYellow,
//                            indicatorColor = Color.Yellow.copy(alpha = 0.2f),

                            // 선택된 상태의 텍스트 색상 (label에 적용됨)
                            selectedTextColor = Color.Red,
                            // 선택되지 않은 상태의 텍스트 색상
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }

    ) { innerPadding ->
        // [수정] NavHost를 Box로 감싸고, 이 Box에 패딩을 적용하여 드롭다운이 전체를 덮도록 합니다.
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // **NavHost** (탭 콘텐츠)
            NavHost(
                navController = bottomNavController,
                startDestination = navItemList[0].route,
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
                        // [추가] MainView의 메뉴 열기 이벤트를 ItemListScreen으로 전달
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

            // [추가] 최상위 MainView에 드롭다운 메뉴와 그림자를 추가
//            LocationDropdownWithDimmingOnMain(
//                isMenuOpen = isLocationMenuOpen,
////                availableAddresses = mainUiState.availableLocations,
//                availableAddresses = mainUiState.availableLocations,
//
//                selectedAddressName = mainaddress ?: "내 주소",
//                onAddressSelected = { location ->
//                    // 실제 ViewModel 로직 호출
//                    // mainViewModel.onAddressSelected(location)
//                    isLocationMenuOpen = false
//                },
//                onNavigateToAddAddress = {
//                    navController.navigate("addressInput")
//                    isLocationMenuOpen = false
//                },
//                onDismiss = { isLocationMenuOpen = false }
//            )
        }
    }
}


// ----------------------------------------------------------------------
// [추가] MainView에서 사용하기 위한 부속 컴포저블 및 데이터 클래스 정의
// ----------------------------------------------------------------------

// LocationUiModel이 ItemListScreen.kt의 model 패키지에 있다면 여기에 복사 또는 별도 공유 파일 사용
data class LocationUiModel(
    val name: String,
    val isSelected: Boolean = false,
    val id: Int = 0 // 필요하다면 ID 추가
)

/**
 * [추가] 드롭다운 메뉴와 배경 그림자 컴포저블 (MainView 전체에 적용)
 */
@Composable
fun LocationDropdownWithDimmingOnMain(
    isMenuOpen: Boolean,
    availableAddresses: List<LocationUiModel>,
    selectedAddressName: String,
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
            .zIndex(1f) // 가장 위에 위치
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
    if (isMenuOpen) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                // TopBar 바로 아래에 위치하도록 조정
                .statusBarsPadding()
                .padding(start = 16.dp, top = 56.dp) // TopAppBar 높이를 고려하여 조정 (대략 56dp)
                .zIndex(2f) // 그림자 위에 위치
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

/**
 * [추가] 드롭다운 메뉴의 개별 항목 컴포저블
 */
@Composable
fun LocationMenuItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}