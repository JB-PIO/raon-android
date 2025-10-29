package com.example.raon.features.auth.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.raon.R
import com.example.raon.features.auth.ui.state.SignUpResult
import com.example.raon.features.auth.ui.viewmodel.SignUpViewModel
import com.example.raon.ui.theme.BrandDarkText
import com.example.raon.ui.theme.BrandYellow
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignupSuccess: () -> Unit,
    signUpViewModel: SignUpViewModel = hiltViewModel()
) {
    val uiState by signUpViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val isFormValid = uiState.nickname.isNotBlank() &&
            uiState.email.isNotBlank() &&
            uiState.password.isNotBlank() &&
            uiState.passwordCheck.isNotBlank() &&
            uiState.userLocation.isNotBlank()

    // ... (LaunchedEffect, Box, Scaffold 등 나머지 코드는 동일) ...
    LaunchedEffect(key1 = uiState.signUpResult) {
        when (val result = uiState.signUpResult) {

            // 회원가입 성공시 처리 과정
            is SignUpResult.Success -> {
                Toast.makeText(context, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                onSignupSuccess()
                signUpViewModel.resultConsumed()        // SignUpResult를 다시 초기화 시키는 코드

            }

            is SignUpResult.Failure -> {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                signUpViewModel.resultConsumed()
            }

            else -> {}
        }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        visible = true
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Scaffold(
            containerColor = Color.White
        ) { paddingValues ->
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(tween(300)) + slideInHorizontally(
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
                        text = "회원가입",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    SignUpTextField(
                        value = uiState.nickname,
                        onValueChange = signUpViewModel::onNicknameChange,
                        label = "닉네임",
                        leadingIcon = Icons.Default.Person
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    SignUpTextField(
                        value = uiState.email,
                        onValueChange = signUpViewModel::onEmailChange,
                        label = "이메일 주소",
                        leadingIcon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    PasswordTextField(
                        value = uiState.password,
                        onValueChange = signUpViewModel::onPasswordChange,
                        label = "비밀번호",
                        isVisible = uiState.isPasswordVisible,
                        onVisibilityChange = signUpViewModel::togglePasswordVisibility
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    PasswordTextField(
                        value = uiState.passwordCheck,
                        onValueChange = signUpViewModel::onPasswordCheckChange,
                        label = "비밀번호 확인",
                        isVisible = uiState.isPasswordCheckVisible,
                        onVisibilityChange = signUpViewModel::togglePasswordCheckVisibility
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    // 새롭게 디자인된 주소 표시 UI
                    LocationDisplayField(
                        address = uiState.userLocation
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { signUpViewModel.signUp() },
                        enabled = isFormValid && uiState.signUpResult != SignUpResult.Loading,
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
                        Text(text = "가입하기", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // ✨ "이미 계정이 있으신가요?" TextButton이 제거되었습니다.
                }
            }
        }

        if (uiState.signUpResult == SignUpResult.Loading) {
            CircularProgressIndicator()
        }
    }
}

// OutlinedTextField 대신 커스텀 UI로 완전히 교체!
@Composable
private fun LocationDisplayField(
    address: String
) {
    val displayText = if (address.isNotBlank()) address else "주소 정보 없음"

    // 라벨과 내용 박스를 분리하기 위해 Column 사용
    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. "내 동네" 라벨을 텍스트 필드 밖으로 이동
        Text(
            text = "내 동네",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF666666),
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        // 2. 아이콘과 주소를 담는 Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 3. 배경색과 모양을 지정하여 다른 필드와 구분
                .background(
                    color = Color(0xFFF5F5F5), // 연한 회색 배경
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = Color(0xFFFDCC31)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333)
            )
        }
    }
}


// ... (SignUpTextField, PasswordTextField는 기존 코드와 동일) ...
@Composable
private fun SignUpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
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
                tint = Color(0xFFFDCC31)
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFFDCC31),
            unfocusedBorderColor = Color(0xFFCCCCCC),
            focusedLabelColor = Color(0xFFFDCC31),
            unfocusedLabelColor = Color(0xFF9E9E9E)
        )
    )
}

@Composable
private fun PasswordTextField(
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