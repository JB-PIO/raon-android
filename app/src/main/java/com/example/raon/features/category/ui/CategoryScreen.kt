package com.example.raon.features.category.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.raon.features.category.data.local.CategoryEntity // import 추가
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    onBackClick: () -> Unit,
    onCategoryClick: (category: CategoryEntity) -> Unit, // 클릭 시 객체 전체를 넘기도록 변경
    viewModel: CategoryViewModel = hiltViewModel()
) {
    // ViewModel의 StateFlow들을 구독합니다.
    val categories by viewModel.categories.collectAsStateWithLifecycle()    // viewmodel에서 받아오는 카테고리 데이터들
    val categoryPath by viewModel.categoryPath.collectAsStateWithLifecycle()    // 카테고리 경로들

    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize(),
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 400)
        ) + fadeIn(animationSpec = tween(durationMillis = 300))
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text("카테고리") },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "뒤로 가기"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                    // 구독한 categoryPath를 Breadcrumb Composable에 전달
                    Breadcrumb(path = categoryPath)
                }
            }
        ) { innerPadding ->
            if (categories.isEmpty() && visible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 8.dp)
                ) {
                    items(
                        items = categories,
                        key = { category -> category.categoryId }
                    ) { category ->
                        CategoryItem(
                            categoryName = category.name,
                            onItemClick = {
                                // 클릭된 카테고리 객체 전체를 전달
                                onCategoryClick(category)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Breadcrumb(path: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 변경 사항 시작: path.forEachIndexed 대신 일반 forEach를 사용하고,
        //                 리스트의 크기에 따라 > 표시 여부를 결정합니다.
        path.forEachIndexed { index, item ->
            val isLastItem = index == path.lastIndex

            Text(
                text = item,
                fontSize = 14.sp,
                fontWeight = if (isLastItem) FontWeight.Bold else FontWeight.Normal,
                color = if (isLastItem) Color.Black else Color.Gray
            )

            // 변경: 마지막 아이템이면서 동시에 path 리스트에 "전체" 외의 다른 아이템이 없을 때만 '>'를 그리지 않습니다.
            // 즉, path가 ["전체"] 일 때는 '>'를 그리지 않고,
            // path가 ["전체", "남성의류"] 일 때는 "남성의류" 뒤에 '>'를 그리지 않습니다.
            // 그리고 path에 "전체" 외의 다른 아이템이 있다면 (index < path.lastIndex), ">"를 그립니다.
            if (index < path.lastIndex) { // 현재 아이템이 마지막 아이템이 아닐 때만 '>'를 그립니다.
                Text(
                    text = " > ",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryItem(
    categoryName: String,
    onItemClick: () -> Unit
) {
    Text(
        text = categoryName,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(16.dp)
    )
}