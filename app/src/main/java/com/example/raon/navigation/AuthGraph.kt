package com.example.raon.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.raon.features.auth.ui.AuthScreen
import com.example.raon.features.auth.ui.LoginScreen
import com.example.raon.features.auth.ui.SignUpScreen
import com.example.raon.features.location.domain.model.Location // 👈 [이 부분 추가]
import com.example.raon.features.location.ui.LocationSearchScreen
import com.example.raon.features.main.ui.MainViewModel // 👈 [이 부분 추가]
import com.example.raon.features.splash.SplashScreen

/**
 * '인증 층'에 해당하는 네비게이션 그래프입니다.
 * 스플래시 화면부터 로그인/회원가입까지의 흐름을 관리합니다.
 *
 * [수정] MainViewModel을 파라미터로 받도록 변경되었습니다.
 * 'location' 화면에서 즐겨찾기 저장을 위해 필요합니다.
 */
fun NavGraphBuilder.authGraph(
    navController: NavController,
    mainViewModel: MainViewModel // [이 부분 추가]
) {
    navigation(startDestination = "splash", route = "auth_graph") {

        // (기존 코드 동일)
        // 첫 로딩 화면 -> 토큰 인증 확인 위해서 필요
        composable("splash") {
            SplashScreen(
                onNavigateToAuth = {
                    navController.navigate("auth") {    // Auth 홈 화면으로 이동 -> 토큰 없음
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToMain = {     // Main 화면으로 이동 -> 토큰 있음
                    navController.navigate("main_graph") {
                        popUpTo("auth_graph") { inclusive = true }
                    }
                }
            )
        }

        // (기존 코드 동일)
        // Auth 홈 화면
        composable("auth") {
            AuthScreen(
                onNavigateToLogin = { navController.navigate("login") },
                // [수정] 'signup' 모드로 'location' 호출
                onNavigateToLocationForSignup = {
                    navController.navigate("location?mode=signup") // mode 파라미터 추가
                }
            )
        }

        // (기존 코드 동일)
        // Login 화면
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main_graph") {
                        popUpTo("auth") {
                            inclusive = true
                        }
                    }
                },
                onNavigateToSignUp = { // -> 로그인 화면에서 회원가입 이동
                    navController.navigate("location?mode=signup") {
                        // 'login' 화면을 백스택에서 제거합니다.
                        popUpTo("login") { inclusive = true }
                    }
                })
        }

        // (기존 코드 동일)
        // SginUp 하면
        composable(
            "signUp?locationId={locationId}&location={location}",
            arguments = listOf(
                navArgument("locationId") {
                    type = NavType.IntType
                    defaultValue = -1 // 기본값
                },
                navArgument("location") {
                    type = NavType.StringType
                    nullable = true // null 허용
                }
            )) {
            SignUpScreen(
                {
                    navController.navigate("main_graph") {
                        popUpTo("auth") {
                            inclusive = true
                        }
                    }
                },
            )
        }


        // ------------------ [이 부분 수정] ------------------
        // 위치 정하는 Screen
        composable(
            "location?mode={mode}", // 1. mode 파라미터 추가 (기존과 동일)
            arguments = listOf(
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = "signup" // 2. 기본값 "signup" (기존과 동일)
                }
            )
        ) { backStackEntry ->
            // 3. mode 값 읽기 (기존과 동일)
            val mode = backStackEntry.arguments?.getString("mode") ?: "signup"

            LocationSearchScreen(
                // 4. [수정] onNavigateToSignup -> onLocationSelected 로 변경
                onLocationSelected = { location: Location -> // 'location' 객체를 통째로 받음
                    // 5. [수정] location 객체에서 address와 id를 추출
                    val address = location.address
                    val locationId = location.locationId

                    if (mode == "signup") {
                        // [기존 기능] 회원가입 플로우
                        navController.navigate("signUp?locationId=${locationId}&location=${address}")
                    } else if (mode == "favorite") { // [수정] "favorite" 모드 분기
                        // [새 기능] 즐겨찾기 플로우
                        mainViewModel.addFavoriteLocation(locationId, address)
                        navController.popBackStack()
                    }
                },
                onBackClick = { // 뒤로 가기
                    navController.popBackStack()
                }
            )
        }
        // ---------------------------------------------------
    }
}