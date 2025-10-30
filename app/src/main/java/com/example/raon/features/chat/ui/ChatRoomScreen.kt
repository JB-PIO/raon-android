package com.example.raon.features.chat.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.raon.features.chat.domain.model.ChatMessage
import com.example.raon.ui.theme.BrandDarkText
import com.example.raon.ui.theme.BrandYellow
import com.example.raon.ui.theme.ChatBackgroundColor
import com.example.raon.ui.theme.DarkGrayText
import com.example.raon.ui.theme.OtherBubbleColor
import com.example.raon.ui.theme.OtherTextColor
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    onBackClick: (chatId: Long) -> Unit,
    onNavigateToItemDetail: (itemId: Int, chatRoomId: Long) -> Unit,
    viewModel: ChatRoomViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val analysisSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.messages) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    if (uiState.imageAnalysisResult != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissImageAnalysis() },
            sheetState = analysisSheetState
        ) {
            AnalysisResultBottomSheetContent(
                results = uiState.imageAnalysisResult!!,
                onClose = {
                    scope.launch {
                        analysisSheetState.hide()
                    }.invokeOnCompletion {
                        if (!analysisSheetState.isVisible) {
                            viewModel.dismissImageAnalysis()
                        }
                    }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            uiState.opponentNickname ?: "",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            val idToSend = viewModel.chatRoomId
                            Log.d(
                                "ChatReadDebug",
                                "1. Back button clicked in ChatRoomScreen. Sending chatId: $idToSend"
                            )
                            onBackClick(idToSend)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = DarkGrayText,
                        navigationIconContentColor = DarkGrayText
                    )
                )
                ProductInfoBar(
                    productImageUrl = uiState.productInfo?.viewableThumbnailUrl ?: "",
                    productStatus = uiState.productInfo?.status ?: "",
                    productName = uiState.productInfo?.productName ?: "",
                    productPrice = uiState.productInfo?.price?.let { "%,d원".format(it) } ?: "",
                    isBuyer = uiState.isCurrentUserBuyer,
                    onAIImageAnalyzeClick = { viewModel.startImageAnalysis() },
                    onProductInfoClick = {
                        uiState.productInfo?.itemId?.let { id ->
                            onNavigateToItemDetail(id, viewModel.chatRoomId)
                        }
                    }
                )
                Divider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)
            }
        },
        bottomBar = {
            MessageInput(
                onSendClick = { text -> viewModel.sendMessage(text) },
                onAIDetectFraud = { viewModel.detectFraud() },
                isBuyer = uiState.isCurrentUserBuyer
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(ChatBackgroundColor)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {

                    items(uiState.messages.reversed()) { message ->
                        MessageBubble(message = message)
                    }
                }
                AnimatedVisibility(visible = uiState.fraudWarningMessage != null) {
                    FraudWarningBanner(
                        message = uiState.fraudWarningMessage.orEmpty(),
                        onClose = { viewModel.closeWarningBanner() }
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.isAnalyzingImage,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.matchParentSize()
            ) {
                Box(
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "AI가 이미지를 분석하고 있습니다...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.isDetectingFraud,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.matchParentSize()
            ) {
                Box(
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "대화 내용으로 사기를 분석하고 있습니다...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun AnalysisResultBottomSheetContent(
    results: List<ImageAnalysisResult>,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text(
            "AI 이미지 분석 결과",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(results) { result ->
                val resultColor = when (result.result) {
                    "SAFE" -> Color(0xFF2E7D32)
                    "WARNING" -> Color(0xFFEF6C00)
                    else -> Color(0xFFC62828)
                }
                val resultIcon = when (result.result) {
                    "SAFE" -> Icons.Default.CheckCircle
                    else -> Icons.Default.Warning
                }
                val resultText = when (result.result) {
                    "SAFE" -> "안전"
                    "WARNING" -> "도용 의심"
                    else -> "위험"
                }
                Row(verticalAlignment = Alignment.Top) {
                    AsyncImage(
                        model = result.imageUrl,
                        contentDescription = "분석 이미지",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = resultIcon,
                                contentDescription = resultText,
                                tint = resultColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(resultText, color = resultColor, fontWeight = FontWeight.Bold)
                        }
                        if (result.similarImages.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(
                                modifier = Modifier.height(70.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                items(result.similarImages) { similarUrl ->
                                    AsyncImage(
                                        model = similarUrl,
                                        contentDescription = "유사 이미지",
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .padding(top = 4.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.LightGray.copy(alpha = 0.5f),
                contentColor = Color.Black
            )
        ) { Text("확인", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun ProductInfoBar(
    productImageUrl: String,
    productStatus: String,
    productName: String,
    productPrice: String,
    isBuyer: Boolean,
    onAIImageAnalyzeClick: () -> Unit,
    onProductInfoClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProductInfoClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = productImageUrl,
            contentDescription = "Product Image",
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$productStatus  $productName",
                color = DarkGrayText,
                fontSize = 14.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = productPrice,
                color = DarkGrayText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isBuyer) {
            Spacer(modifier = Modifier.width(12.dp))
            Row(
                modifier = Modifier
                    .background(
                        color = Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onAIImageAnalyzeClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI 분석",
                    tint = DarkGrayText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "AI 사진 분석하기",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkGrayText
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!message.isFromMe) {
            Spacer(modifier = Modifier.width(8.dp))
        }

        val bubbleColor = if (message.isFromMe) BrandYellow else OtherBubbleColor
        val textColor = if (message.isFromMe) BrandDarkText else OtherTextColor

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = bubbleColor,
            tonalElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = message.content,
                    color = textColor,
                    fontSize = 16.sp
                )
                Text(
                    text = message.timestamp,
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun MessageInput(
    onSendClick: (String) -> Unit,
    onAIDetectFraud: () -> Unit,
    isBuyer: Boolean
) {
    var text by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 0.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* 첨부파일 기능 */ }) {
                Icon(Icons.Default.Add, contentDescription = "Attach File", tint = Color.Gray)
            }

            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("메시지 보내기", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                maxLines = 4
            )

            if (isBuyer) {
                IconButton(onClick = { onAIDetectFraud() }) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI 분석",
                        tint = Color.Gray
                    )
                }
            }

            if (text.isBlank()) {
                IconButton(onClick = { /* 이모티콘 창 열기 */ }) {
                    Icon(
                        Icons.Default.SentimentSatisfied,
                        contentDescription = "Emoji",
                        tint = Color.Gray
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        onSendClick(text)
                        text = ""
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = BrandYellow
                    )
                }
            }
        }
    }
}

@Composable
private fun FraudWarningBanner(
    message: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF3E0))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "경고",
            tint = Color(0xFFE65100)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            color = Color(0xFF4E342E),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "경고 닫기",
                tint = Color.Gray
            )
        }
    }
}