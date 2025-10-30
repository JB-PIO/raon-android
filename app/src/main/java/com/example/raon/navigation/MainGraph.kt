package com.example.raon.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.raon.features.main.ui.MainView
import com.example.raon.features.user.ui.SettingsScreen
import com.example.raon.features.user.ui.WithdrawalScreen

/**
 * '메인 층'에 해당하는 네비게이션 그래프입니다.
 * 로그인 후 진입하는 MainView와 설정 화면 등의 흐름을 관리합니다.
 */
fun NavGraphBuilder.mainGraph(navController: NavController) {
    navigation(startDestination = "main_screen", route = "main_graph") {

        composable("main_screen") {
            // MainView는 '메인 층'의 입구 역할을 하며,
            // '별관'(addItem, itemDetail 등)으로 이동해야 하므로,
            // 건물 전체를 아는 상위 navController를 전달받습니다.
            MainView(navController = navController)
        }

        composable("settings_screen") {
            SettingsScreen(
                onLogout = {
                    navController.navigate("auth_graph") {
                        popUpTo("main_graph") { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() },
                onNavigateToWithdrawal = { navController.navigate("withdrawal_screen") }
            )
        }

        // ▼▼▼ [핵심 수정] 경로 이름을 "withdrawal"에서 "withdrawal_screen"으로 변경 ▼▼▼
        composable("withdrawal_screen") {
            WithdrawalScreen(
                onBackClick = { navController.popBackStack() },
                onWithdrawalSuccess = {
                    // 성공 시, 로그인 화면으로 이동하고 이전 기록을 모두 삭제
                    navController.navigate("auth_graph") {
                        popUpTo("main_graph") {
                            inclusive = true
                        } // popUpTo의 대상을 navController.graph.startDestinationId 대신 "main_graph"로 명확히 지정
                    }
                }
            )
        }


    }
}