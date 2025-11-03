package com.example.raon.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.raon.MainActivity
import com.example.raon.R
// [수정됨] 1. DTO를 직접 import 합니다. (도메인 모델 대신)
import com.example.raon.features.chat.data.remote.dto.ChatMessageDto
import dagger.hilt.android.qualifiers.ApplicationContext // [수정됨] 2. Hilt import
import javax.inject.Inject // [수정됨] 3. Hilt import
import javax.inject.Singleton // [수정됨] 4. Hilt import

/**
 * Android Notification을 생성하고 관리하는 헬퍼 클래스
 */
@Singleton // [수정됨] 5. 싱글톤으로 Hilt에 등록
class ChatNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context // [수정됨] 6. Hilt가 Context 주입
) {

    companion object {
        const val CHANNEL_ID = "raon_chat_channel"
        const val CHANNEL_NAME = "실시간 채팅 알림"
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * 알림 채널을 생성합니다. (Android 8.0/API 26 이상 필수)
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH // 헤드업 알림을 위한 HIGH 설정
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "새로운 채팅 메시지 수신 알림"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 채팅 메시지 알림을 띄웁니다.
     * @param chatMessage 수신된 메시지 정보 (DTO)
     */
    // [수정됨] 7. 파라미터를 도메인 모델(ChatMessage) 대신 DTO(ChatMessageDto)로 변경
    fun showChatNotification(chatMessage: ChatMessageDto) {
        val intent = Intent(context, MainActivity::class.java).apply {
            // TODO: 알림 클릭 시 특정 채팅방으로 이동하는 로직 추가
            // 예: putExtra("destination_route", "chat_room/${chatMessage.chatRoomId}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            chatMessage.chatId.toInt(), // [수정됨] DTO의 chatRoomId 사용
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.raon_icon)
            .setContentTitle(chatMessage.sender.nickname) // [수정됨] DTO의 sender.nickname 사용
            .setContentText(chatMessage.content) // [수정됨] DTO의 content 사용
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 250, 500))

        notificationManager.notify(chatMessage.chatId.toInt(), builder.build())
    }
}