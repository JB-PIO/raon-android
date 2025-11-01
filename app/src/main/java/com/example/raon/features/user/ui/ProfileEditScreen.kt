package com.example.raon.features.user.ui

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.raon.R
import com.example.raon.core.common.AppConstants
import com.example.raon.ui.theme.BrandDarkText
import com.example.raon.ui.theme.BrandYellow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onClose: () -> Unit,
    viewModel: ProfileEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // 1. BottomSheet를 제어하기 위한 상태 변수 추가
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    val isDoneButtonEnabled = !uiState.isLoading &&
            uiState.nickname.isNotBlank() &&
            (uiState.nickname != uiState.initialNickname || uiState.newImageUri != null || uiState.isImageRemoved) // 이미지 삭제 여부도 조건에 추가

    // 2. BottomSheet UI를 화면에 추가
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showBottomSheet = false
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandYellow,
                        contentColor = BrandDarkText
                    )
                ) {
                    Text("앨범에서 선택")
                }

                // 조건: 1. 새로 선택한 이미지가 있거나 (newImageUri != null)
                //      2. 기존 이미지가 있으면서 (currentImageUrl != null)
                //         기본 이미지가 아닐 때 (currentImageUrl != AppConstants.DEFAULT_PROFILE_URL)
                if (uiState.newImageUri != null || (uiState.currentImageUrl != null && uiState.currentImageUrl != AppConstants.DEFAULT_PROFILE_URL)) {
                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                showBottomSheet = false
                                viewModel.onImageRemoved() // ViewModel에 이미지 삭제 요청
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("프로필 사진 삭제", color = Color.Red)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showBottomSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray.copy(alpha = 0.2f),
                        contentColor = Color.DarkGray
                    )
                ) {
                    Text("닫기")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("프로필 수정") },
                navigationIcon = {
                    IconButton(onClick = { onClose() }) {
                        Icon(Icons.Default.Close, contentDescription = "닫기")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.onSaveChanges {
                                onClose()
                            }
                        },
                        enabled = isDoneButtonEnabled,
                        //  1. colors 파라미터를 추가합니다.
                        colors = ButtonDefaults.textButtonColors(
                            //  2. 활성화 시 배경색
                            containerColor = BrandYellow,
                            //  3. 활성화 시 글자색
                            contentColor = BrandDarkText,

                            //  4. 비활성화 시 배경색
                            disabledContainerColor = Color.Gray.copy(alpha = 0.2f),
                            //  5. 비활성화 시 글자색
                            disabledContentColor = Color.Gray.copy(alpha = 0.7f)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text("완료", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            ProfileImagePicker(
                viewableImageUrl = uiState.viewableCurrentImageUrl,
                currentImageUrl = uiState.currentImageUrl,
                newImageUri = uiState.newImageUri,
                onClick = {
                    // 3. 이미지 클릭 시 갤러리를 바로 여는 대신 BottomSheet를 띄움
                    showBottomSheet = true
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "닉네임",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
            OutlinedTextField(
                value = uiState.nickname,
                onValueChange = viewModel::onNicknameChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("닉네임을 입력하세요") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandYellow,
                    unfocusedBorderColor = Color(0xFFCCCCCC),
                    focusedLabelColor = BrandYellow,
                    unfocusedLabelColor = Color(0xFF9E9E9E)
                )
            )
        }
    }
}

@Composable
fun ProfileImagePicker(
    viewableImageUrl: String?, // Presigned URL 또는 null
    currentImageUrl: String?,
    newImageUri: Uri?,
    onClick: () -> Unit
) {
    // Box는 컨테이너 역할만 하고, 실제 이미지/아이콘에 클립과 테두리를 적용합니다.
    Box(
        modifier = Modifier
            .size(120.dp) // 전체 영역 크기
            .clickable(onClick = onClick), // 영역 전체 클릭 가능
        contentAlignment = Alignment.Center // 자식들을 중앙에 배치
    ) {
        val model: Any? = newImageUri ?: viewableImageUrl

        if (model == null || model == AppConstants.DEFAULT_PROFILE_URL) {

            Log.d("ProfileImagePicker", "if : 기본 이미지 : $model")

            // 기본 프로필 이미지
            AsyncImage(
                model = R.drawable.user_icon,
                contentDescription = "프로필 사진",
                modifier = Modifier
                    .size(100.dp)
//                    .fillMaxSize() // 120.dp 채우기
                    .clip(CircleShape) // 이미지를 원형으로 자르기
                    .border(2.dp, Color.LightGray, CircleShape), // 테두리 추가
                contentScale = ContentScale.Crop
            )

        } else {

            Log.d("ProfileImagePicker", "else : 실제 이미지 : $model")


            // 2. 사용자가 선택한/기존 이미지
            AsyncImage(
                model = model,
                contentDescription = "프로필 사진",
                modifier = Modifier
                    .fillMaxSize() // 120.dp 채우기
                    .clip(CircleShape) // 이미지를 원형으로 자르기
                    .border(2.dp, Color.LightGray, CircleShape), // 테두리 추가
                contentScale = ContentScale.Crop
            )
        }

        // 3. 카메라 아이콘 (이 Box는 부모 Box의 clip에 영향을 받지 않음)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd) // 부모 Box(120.dp)의 오른쪽 하단에 정렬
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, Color.LightGray, CircleShape)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "사진 변경",
                modifier = Modifier.size(20.dp),
                tint = Color.DarkGray
            )
        }
    }
}