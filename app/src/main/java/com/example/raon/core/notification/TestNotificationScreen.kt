package com.example.raon.core.notification

// app/src/main/java/com/example/raon/features/chat/ui/TestNotificationScreen.kt (예시 파일)

//package com.example.raon.features.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// 임시 ChatMessage 모델 (실제 사용 시 DTO 변환 필요)
// 실제 ChatMessage 모델이 없다면 다음과 같이 정의되었다고 가정합니다.
data class TestChatMessage(
    val messageId: Long,
    val chatId: Long,
    val senderNickname: String,
    val content: String,
    // ... (알림에는 content, senderNickname, chatId만 사용)
)

@Composable
fun TestNotificationScreen() {
    // 1. Context 획득 (알림 매니저 접근에 필요)
    val context = LocalContext.current

    // 2. 알림 헬퍼 인스턴스 생성
    val notificationHelper = ChatNotificationHelper(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text("버튼을 눌러 헤드업 알림 테스트", modifier = Modifier.padding(bottom = 24.dp))

        Button(onClick = {
            // 버튼 클릭 시 알림 띄우기 로직 실행
            val testMessage = TestChatMessage(
                messageId = System.currentTimeMillis(),
                chatId = 12345L, // 알림 클릭 시 이동할 채팅방 ID
                senderNickname = "라온",
                content = "안녕하세요!",
            )

            // 3. 알림 표시 함수 호출
//            notificationHelper.showChatNotification(testMessage)

        }) {
            Text("헤드업 알림 띄우기 (HIGH Priority)")
        }
    }
}