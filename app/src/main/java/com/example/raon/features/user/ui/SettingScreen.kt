package com.example.raon.features.user.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.raon.ui.theme.BrandDarkText
import com.example.raon.ui.theme.BrandYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onBackClick: () -> Unit,
    onNavigateToWithdrawal: () -> Unit, // 회원탈퇴 화면으로 이동 콜백
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var isNotificationOn by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(text = "로그아웃", fontWeight = FontWeight.Bold) },
            text = { Text(text = "정말 로그아웃하시겠어요?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logout()
                        onLogout()
                        showLogoutDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandYellow,
                        contentColor = BrandDarkText
                    )
                ) {
                    Text("로그아웃")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(
                        "닫기",
                        color = BrandDarkText
                    )
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            item { SettingsCategoryHeader("계정") }
            item { SettingsMenuItem(title = "프로필 수정", onClick = { /* TODO */ }) }
            item { SettingsMenuItem(title = "계정 및 로그인 정보", onClick = { /* TODO */ }) }
            item { SettingsCategoryHeader("알림") }
            item {
                SettingsToggleItem(
                    title = "채팅 및 활동 알림",
                    checked = isNotificationOn,
                    onCheckedChange = { isNotificationOn = it },
                )
            }
            item { SettingsCategoryHeader("정보") }
            item { SettingsMenuItem(title = "공지사항", onClick = { /* TODO */ }) }
            item { SettingsMenuItem(title = "서비스 이용약관", onClick = { /* TODO */ }) }
            item { SettingsMenuItem(title = "개인정보 처리방침", onClick = { /* TODO */ }) }
            item { SettingsInfoItem(title = "앱 버전", value = "1.0.0") }
            item {
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    HorizontalDivider()
                    Text(
                        text = "로그아웃",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLogoutDialog = true }
                            .padding(16.dp)
                    )
                    HorizontalDivider()
                    Text(
                        text = "회원탈퇴",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToWithdrawal() } // 콜백 호출
                            .padding(16.dp)
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = Color.DarkGray,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsMenuItem(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.NavigateNext,
            contentDescription = null,
            tint = Color.Gray
        )
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                // 켜졌을 때 원형 핸들 색상 (보통 흰색)
                checkedThumbColor = Color.White,
                // 켜졌을 때 트랙(배경) 색상 (보라색 계열)
                checkedTrackColor = BrandYellow, // 예시 보라색

                // --- 비활성화(꺼짐) 상태 색상 (선택 사항) ---
                // 꺼졌을 때 원형 핸들 색상
                // uncheckedThumbColor = Color.White,
                // 꺼졌을 때 트랙(배경) 색상
                // uncheckedTrackColor = Color.Gray
            )
        )
    }
}

@Composable
private fun SettingsInfoItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, modifier = Modifier.weight(1f))
        Text(text = value, color = Color.Gray)
    }
}