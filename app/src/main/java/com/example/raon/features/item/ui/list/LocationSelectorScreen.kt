package com.example.raon.features.item.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

// 이미지의 주소 목록 데이터 클래스
data class Location(val name: String)

// Experimental API (CenterAlignedTopAppBar) 사용 승인
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectorScreen() {
    // 1. 상태 관리: 현재 선택된 주소
    var selectedLocation by remember { mutableStateOf(Location("대자동")) }
    // 2. 상태 관리: 메뉴 열림/닫힘 상태 (배경 그림자 활성화 상태)
    var isMenuOpen by remember { mutableStateOf(false) }

    // 이미지의 주소 목록
    val locations = listOf(
        Location("원종2동"),
        Location("대자동")
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    // 클릭 영역 (앵커 역할)
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                        Row(
                            modifier = Modifier
                                .clickable { isMenuOpen = true } // 클릭 시 상태 변경
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedLocation.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "위치 변경 아이콘",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* 검색 아이콘 클릭 로직 */ }) {
                        Icon(Icons.Default.Search, contentDescription = "검색 아이콘")
                    }
                    IconButton(onClick = { /* 알림 아이콘 클릭 로직 */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = "알림 아이콘")
                    }
                }
            )
        },
        content = { paddingValues ->

            // 전체 화면을 덮는 Box 레이아웃 (배경, 메뉴, 콘텐츠 모두 포함)
            Box(
                modifier = Modifier
                    .padding(paddingValues) // TopBar 아래에 콘텐츠 배치
                    .fillMaxSize()
            ) {

                // === 3. 어두운 배경 (그림자) 레이어 ===
                // 메뉴가 열렸을 때만 반투명한 배경이 나타납니다.
                AnimatedVisibility(
                    visible = isMenuOpen,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f) // 일반 콘텐츠(0f)보다 위에 위치
                ) {
                    // 배경 클릭 시 메뉴 닫기 및 그림자 제거 (Ripple 효과 제거)
                    Spacer(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)) // 배경 그림자 색상과 투명도
                            .clickable(
                                onClick = { isMenuOpen = false },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                    )
                }

                // === 4. 커스텀 드롭다운 메뉴 레이어 ===
                if (isMenuOpen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.5f) // 너비를 적절히 조정
                            .align(Alignment.TopStart) // TopBar의 시작점에 맞춤
                            .zIndex(2f) // 그림자 레이어(1f) 위에 위치하도록 설정
                            .background(MaterialTheme.colorScheme.surface) // 메뉴 배경색
                            .padding(vertical = 8.dp)
                    ) {
                        // 메뉴 항목 함수
                        LocationMenuItem(text = "원종2동") {
                            selectedLocation = Location("원종2동"); isMenuOpen = false
                        }
                        LocationMenuItem(text = "대자동") {
                            selectedLocation = Location("대자동"); isMenuOpen = false
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        LocationMenuItem(text = "내 동네 설정") { /* 설정 로직 */ isMenuOpen = false }
                    }
                }

                // === 5. 실제 콘텐츠 레이어 ===
                Text(
                    text = "현재 선택된 위치: ${selectedLocation.name}\n(이 아래에 게시물 목록이 표시됩니다.)",
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.Center)
                )
            }
        }
    )
}

@Composable
fun LocationMenuItem(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

// 프리뷰
@Preview(showBackground = true)
@Composable
fun LocationSelectorScreenPreview() {
    MaterialTheme {
        LocationSelectorScreen()
    }
}