package com.example.raon.features.item.ui.add

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.raon.R
import com.example.raon.ui.theme.BrandDarkText
import com.example.raon.ui.theme.BrandYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    modifier: Modifier = Modifier,
    onUploadSuccess: () -> Unit,
//    onEditSuccess: () -> Unit,  // 수정
    onNavigationToCategory: () -> Unit,
    onClose: () -> Unit = {},
    viewModel: AddItemViewModel = hiltViewModel(),
    onClearCategoryResult: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEditing = viewModel.itemId != null && viewModel.itemId != -1
    val context = LocalContext.current

    // 등록/수정 성공 시 화면을 닫는 로직
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onUploadSuccess()
        }
    }

    // 에러 발생 시 로그를 남기거나 토스트를 띄우는 로직 (옵션)
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        viewModel.onEvent(AddItemEvent.AddImages(uris))
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isEditing) "게시글 수정" else "내 물건 팔기",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "닫기")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Button(
                onClick = { viewModel.onEvent(AddItemEvent.Submit) },
                enabled = uiState.title.isNotBlank() &&
                        uiState.isPriceValid &&
                        (if (isEditing) (uiState.existingImageUrls.isNotEmpty() || uiState.seletedImages.isNotEmpty()) else uiState.seletedImages.isNotEmpty()) &&
                        uiState.isCategoryValid &&
//                        uiState.description.isNotEmpty() &&
//                        !uiState.isLoading && // 1. 로딩 중이 아닐 때
//                        !uiState.isSuccess, // 2. 이미 성공한 상태가 아닐 때
                        uiState.description.isNotEmpty(),

                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandYellow,
                    contentColor = BrandDarkText
                )
            ) {
                Text(
                    if (isEditing) "수정 완료" else "등록 완료",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                ImageUploadSection(
                    existingImageUrls = uiState.existingImageUrls,
                    selectedImages = uiState.seletedImages,
                    maxImageCount = 5,
                    onAddImage = {
                        pickMediaLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemoveImage = { uri ->
                        viewModel.onEvent(AddItemEvent.RemoveImage(uri))
                    },
                    // 👇 TODO 주석을 풀고 ViewModel 이벤트를 호출하도록 수정
                    onRemoveExistingImage = { url ->
                        viewModel.onEvent(AddItemEvent.RemoveExistingImage(url))
                    }
                )
            }

            item { SectionHeader(title = "상품 정보") }

            item {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.onEvent(AddItemEvent.TitleChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("상품명") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                )
            }

            item {
                CategorySelectionField(
                    selectedCategoryName = uiState.selectedCategoryName,
                    onClick = onNavigationToCategory
                )
            }

            item {
                SectionHeader(title = "상품 상태")
                ProductConditionSelector(
                    selectedCondition = uiState.productCondition,
                    onConditionSelected = { condition ->
                        viewModel.onEvent(AddItemEvent.ProductConditionChanged(condition))
                    }
                )
            }

            item {
                SectionHeader(title = "가격")
                OutlinedTextField(
                    value = uiState.price,
                    onValueChange = { viewModel.onEvent(AddItemEvent.PriceChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0") },
                    leadingIcon = { Text("₩") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                )
            }

            item {
                SectionHeader(title = "자세한 설명")
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.onEvent(AddItemEvent.DescriptionChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp),
                    placeholder = { Text("브랜드, 모델명, 구매 시기, 하자 유무 등 상품 설명을 최대한 자세히 적어주세요.\n" + "개인정보(전화번호, SNS 계정 등)는 입력할 수 없어요.") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.description.length} / 2000",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageUploadSection(
    existingImageUrls: List<String>,
    selectedImages: List<Uri>,
    maxImageCount: Int,
    onAddImage: () -> Unit,
    onRemoveImage: (Uri) -> Unit,
    onRemoveExistingImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalImageCount = existingImageUrls.size + selectedImages.size

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                    .clickable(onClick = onAddImage, enabled = totalImageCount < maxImageCount),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_camera_alt_24),
                        contentDescription = "이미지 추가",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$totalImageCount/$maxImageCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        items(existingImageUrls) { imageUrl ->
            Box(modifier = Modifier.size(88.dp)) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "기존 이미지",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { onRemoveExistingImage(imageUrl) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "이미지 제거",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        items(selectedImages) { imageUri ->
            Box(modifier = Modifier.size(88.dp)) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "선택된 이미지",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { onRemoveImage(imageUri) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "이미지 제거",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelectionField(
    selectedCategoryName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val borderThickness = if (isPressed) 2.dp else 1.dp
    val borderColor =
        if (isPressed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(OutlinedTextFieldDefaults.MinHeight)
            .border(width = borderThickness, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectedCategoryName ?: "카테고리 선택",
                color = if (selectedCategoryName != null && selectedCategoryName != "카테고리 선택") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ProductConditionSelector(
    selectedCondition: ProductCondition,
    onConditionSelected: (ProductCondition) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProductCondition.values().forEach { condition ->
            val isSelected = selectedCondition == condition
            Button(
                onClick = { onConditionSelected(condition) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = if (isSelected) {
                    ButtonDefaults.buttonColors(
                        containerColor = BrandYellow,

                        contentColor = BrandDarkText

                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text(text = condition.displayName)
            }
        }
    }
}