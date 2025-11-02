package com.example.raon.features.user.ui // SettingsScreen과 같은 경로에 생성

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.raon.ui.theme.BrandDarkText
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalScreen(
    onBackClick: () -> Unit,
    onWithdrawalSuccess: () -> Unit,
    viewModel: WithdrawalViewModel = hiltViewModel() // 새로 만든 ViewModel 주입
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // ViewModel로부터 오는 이벤트를 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is WithdrawalEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is WithdrawalEvent.WithdrawalSuccess -> {
                    onWithdrawalSuccess()
                }
            }
        }
    }

    // 최종 확인 다이얼로그
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("정말 탈퇴하시겠습니까?") },
            text = { Text("계정을 삭제하면 모든 데이터가 영구적으로 삭제되며, 다시는 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    viewModel.withdrawAccount() // ViewModel의 탈퇴 함수 호출
                }) {
                    Text("탈퇴", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("취소", color = BrandDarkText)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("회원탈퇴") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Text(
                    "회원탈퇴 전 꼭 확인해주세요",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "계정을 탈퇴하시면 회원님의 프로필, 게시물, 채팅 내역 등 모든 활동 정보가 영구적으로 삭제되며 다시는 복구할 수 없습니다.",
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onAgreementChanged(!uiState.agreedToTerms) }
                        .padding(vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = uiState.agreedToTerms,
                        onCheckedChange = { viewModel.onAgreementChanged(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.error
                        )

                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("위의 내용을 모두 확인했으며, 계정 삭제에 동의합니다.")
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = uiState.agreedToTerms && !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        disabledContainerColor = Color.LightGray
                    )
                ) {
                    Text("회원탈퇴", fontWeight = FontWeight.Bold)
                }
            }

            if (uiState.isLoading) {
                // 화면 전체를 덮는 로딩 인디케이터
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}