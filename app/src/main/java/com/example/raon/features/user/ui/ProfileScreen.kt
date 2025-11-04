package com.example.raon.features.user.ui


import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.raon.R
import com.example.raon.core.common.AppConstants
import com.example.raon.features.user.domain.model.User
import com.example.raon.ui.theme.BrandDarkText
import com.example.raon.ui.theme.BrandYellow


/**
 * ProfileScreen: 화면의 콘텐츠만 책임집니다.
 */
@Composable
fun ProfileScreen(
    onNavigateToProfileEditScreen: () -> Unit,
    onNavigateToSalesHistoryScreen: () -> Unit,
    onNavigateToFavoritesScreen: () -> Unit,
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Scaffold를 제거하고 Column으로 바로 시작합니다.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 데이터 상태에 따라 헤더 UI를 분기 처리합니다.
        if (userProfile == null) {
            // 데이터 로딩 중이거나 없을 때 보여줄 UI
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp), // 헤더 영역의 대략적인 높이 지정
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandYellow)
            }
        } else {
            // 데이터가 있을 때 실제 프로필 정보를 보여줍니다.
            ProfileHeader(
                user = userProfile!!,
                uiState = uiState,
                onNavigateToProfileEditScreen = onNavigateToProfileEditScreen
            )
        }

        Divider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant)
        ProfileMenuList(
            navController = navController,
            onNavigateToSalesHistoryScreen,
            onNavigateToFavoritesScreen
        )
    }
}

/**
 * TopAppBar: MainView에서 호출하여 사용합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopAppBar(navController: NavController) {
    TopAppBar(
        title = { Text("프로필") },
        actions = {

            // 중복으로 일단 주석
//            IconButton(onClick = { navController.navigate("settings_screen") }) {
//                Icon(
//                    imageVector = Icons.Default.Settings,
//                    contentDescription = "설정으로 이동"
//                )
//            }
        }
    )
}

/**
 * 프로필 상단 헤더 UI
 */
@Composable
fun ProfileHeader(
    user: User,
    uiState: ProfileUiState,
    onNavigateToProfileEditScreen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val imageModel: Any = uiState.viewableProfileImageUrl // Presigned URL 우선 사용
            ?: R.drawable.user_icon // Presigned URL이 null이면 기본 아이콘 사용

        // ▼▼▼ 핵심 로직 ▼▼▼
        // AsyncImage에 전달할 모델을 여기서 결정합니다.
        val imageModel2 =
            if (user.profileImage == AppConstants.DEFAULT_PROFILE_URL || user.profileImage.isNullOrEmpty()) {
                // 서버에서 받은 값이 약속된 URL이거나, null 또는 비어있다면
                // 앱에 내장된 기본 이미지를 보여줍니다.
                R.drawable.user_icon // (ic_default_profile은 이전에 추가한 벡터 이미지 이름입니다)
            } else {
                // 그 외의 모든 경우는 실제 이미지 URL로 간주합니다.


                user.profileImage
            }


        AsyncImage(
//            model = user.profileImage ?: "https://i.pravatar.cc/300",
            model = imageModel,
//            model = "https://image-notepet.akamaized.net/card_news/201907/8ee7feb500e154ba61bc0f8fad06b07f.jpg",


            contentDescription = "프로필 사진",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .border(2.dp, Color.LightGray, CircleShape),
            contentScale = ContentScale.Crop

        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = user.nickname,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = user.address,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                /* TODO: 프로필 수정 화면으로 이동 */
                onNavigateToProfileEditScreen()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandYellow,
                contentColor = BrandDarkText
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(text = "프로필 수정", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * 프로필 메뉴 리스트 UI
 */
@Composable
fun ProfileMenuList(
    navController: NavController,
    onNavigateToSalesHistoryScreen: () -> Unit,
    onNavigateToFavoritesScreen: () -> Unit
) {
    Column {
        Text(
            text = "나의 거래",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = BrandDarkText,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
//        MenuRow(icon = Icons.Filled.ShoppingCart, title = "구매내역", onClick = { /*TODO*/ })
        Divider(modifier = Modifier.padding(horizontal = 16.dp))

        // 판매내역
        MenuRow(
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            title = "판매내역",
            onClick = { onNavigateToSalesHistoryScreen() })
        Divider(modifier = Modifier.padding(horizontal = 16.dp))

        // 관심목록
        MenuRow(
            icon = Icons.Filled.FavoriteBorder, title = "관심목록",
            onClick = { onNavigateToFavoritesScreen() })
        Divider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant)
        Text(
            text = "기타",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = BrandDarkText,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        MenuRow(
            icon = Icons.Filled.Settings,
            title = "앱 설정",
            onClick = { navController.navigate("settings_screen") }
        )
        Divider(modifier = Modifier.padding(horizontal = 16.dp))
        MenuRow(icon = Icons.Filled.Info, title = "공지사항", onClick = { /*TODO*/ })
    }
}

/**
 * 메뉴 리스트의 각 행 UI
 */
@Composable
fun MenuRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = BrandDarkText
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = "이동",
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )
    }
}