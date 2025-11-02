package com.example.raon.features.auth.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.raon.R
import com.example.raon.features.auth.ui.state.LoginResult
//import com.example.raon.features.auth.ui.viewmodel.LoginResult
import com.example.raon.features.auth.ui.viewmodel.LoginViewModel
import com.example.raon.ui.theme.BrandDarkText
import com.example.raon.ui.theme.BrandYellow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    loginViewModel: LoginViewModel = hiltViewModel()
) {
    // UI 상태 관리를 위한 변수들
    val email = loginViewModel.email
    val password = loginViewModel.password
    var passwordVisible by remember { mutableStateOf(false) }
    val loginResult by loginViewModel.loginResult.collectAsState()

    // 현재 로딩 중인지 판단하는 상태 변수 추가
    // loginResult가 LoginResult.Loading 상태일 때 true
    val isLoading = loginResult is LoginResult.Loading

    // Snackbar 상태와 코루틴 스코프 추가
    // Composable 함수의 최상단에 다른 상태 변수들과 함께 위치합니다.
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()


    // Faliure 상태일 때만 사용할 에러 메시지 변수 -> 사용 중인지 애매함
    val errorMessage = if (loginResult is LoginResult.Failure) {
        (loginResult as LoginResult.Failure).message
    } else {
        null
    }

    // 화면 등장 애니메이션을 위한 상태
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50) // SignUpScreen2와 동일한 속도
        visible = true
    }

    // 로그인 결과(State)가 변경되었을 때 탐지하여 화면 전환 처리
    LaunchedEffect(key1 = loginResult) {
        when (loginResult) {
            is LoginResult.Success -> {
                // 로그인 성공 시 화면 전환 콜백 실행
                onLoginSuccess()
            }

            is LoginResult.Failure -> {
                // 로그인 실패 시 에러 로그 출력 (Toast나 Snackbar로 사용자에게 알림을 주도록 설계)
//                val errorMessage = (loginResult as LoginResult.Faliure).message
//                Log.e("LoginScreen", "Login failed: $errorMessage")
            }

            is LoginResult.ServerError -> {
                // 로그인 실패 시 에러 로그 출력 (Snackbar로 사용자에게 알림을 주도록 설계)
                val errorMessage = (loginResult as LoginResult.ServerError).message
                Log.e("LoginScreen", "Login failed: $errorMessage")

                // scope.launch를 사용해 코루틴 내에서 Snackbar를 띄웁니다.
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "네트워크 연결이 원활하지 않습니다. 잠시 후 다시 시도해 주세요.",
//                        message = LoginResult.Error(errorMessage).message,

                    )
                }
            }

            else -> {
                // Idle 또는 Loading 상태에서는 아무것도 하지 않음
            }
        }
    }




    Scaffold(
        // Scaffold에 snackbarHost를 연결하는 부분
        // Scaffold 컴포저블의 파라미터로 전달
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },

        containerColor = Color.White
    ) { paddingValues ->
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(300)) + slideInHorizontally( // SignUpScreen2와 동일한 애니메이션
                initialOffsetX = { it },
                animationSpec = tween(300)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "로그인",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                Spacer(modifier = Modifier.height(32.dp))

                // 이메일 입력 필드
                LoginTextField(
                    value = email,
                    onValueChange = loginViewModel::onEmailChange,
                    label = "이메일 주소",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 비밀번호 입력 필드
                LoginPasswordTextField(
                    value = password,
                    onValueChange = loginViewModel::onPasswordChange,
                    label = "비밀번호",
                    isVisible = passwordVisible,
                    onVisibilityChange = { passwordVisible = !passwordVisible }
                )

                // errorMessage가 있을 때만 Text를 표시
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "이메일 또는 비밀번호를 다시 확인해주세요.",
                        color = Color.Red, // 또는 MaterialTheme.colorScheme.error
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }



                Spacer(modifier = Modifier.height(32.dp))

                // '로그인' 버튼
                Button(
                    onClick = {
                        Log.d("LoginTest", "로그인 버튼 클릭")
                        loginViewModel.login(email, password)
                        Log.d("LoginTest", "로그인 버튼 클릭2")


                    },
                    enabled = !isLoading,
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

                    // isLoading 상태에 따라 버튼 안의 내용물을 변경
                    if (isLoading) {
                        // 로딩 중일 때: 동그란 로딩 아이콘 표시
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF3C3C3C),
                            strokeWidth = 2.dp // 로딩 아이콘의 두께
                        )
                    } else {
                        // 로딩 중이 아닐 때: '로그인' 텍스트 표시
                        Text(text = "로그인", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }


//                    Text(text = "로그인", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(16.dp))

                // '회원가입' 텍스트 버튼
                TextButton(onClick = {
                    /*  회원가입 화면으로 이동 */
                    onNavigateToSignUp()
                }) {
                    Text(
                        "계정이 없으신가요? 회원가입",
                        color = Color(0xFF666666),
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * 일반 텍스트 입력을 위한 커스텀 OutlinedTextField
 */
@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color(0xFF666666)) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = BrandYellow
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandYellow,
            unfocusedBorderColor = Color(0xFFCCCCCC),
            focusedLabelColor = BrandYellow,
            unfocusedLabelColor = Color(0xFF9E9E9E)
        )
    )
}

/**
 * 비밀번호 입력을 위한 커스텀 OutlinedTextField (보기/숨기기 기능 포함)
 */
@Composable
private fun LoginPasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isVisible: Boolean,
    onVisibilityChange: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color(0xFF666666)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFFFDCC31)
            )
        },
        trailingIcon = {
            IconButton(onClick = onVisibilityChange) {
                Icon(
                    painter = if (isVisible) painterResource(id = R.drawable.eyes) else painterResource(
                        id = R.drawable.eyes_hidden
                    ),
                    contentDescription = if (isVisible) "비밀번호 숨기기" else "비밀번호 보기",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Black
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFFDCC31),
            unfocusedBorderColor = Color(0xFFCCCCCC),
            focusedLabelColor = Color(0xFFFDCC31),
            unfocusedLabelColor = Color(0xFF9E9E9E)
        )
    )
}

