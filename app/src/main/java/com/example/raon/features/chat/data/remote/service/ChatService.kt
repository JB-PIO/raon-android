package com.example.raon.features.chat.data.remote.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.raon.R
import com.example.raon.core.common.CurrentScreenManager
import com.example.raon.core.notification.ChatNotificationHelper
import com.example.raon.features.chat.data.remote.dto.ChatMessageDto
import com.example.raon.features.chat.domain.repository.ChatRepository
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * STOMP 연결을 관리하고 백그라운드에서 메시지를 수신하는 ForegroundService
 */
@AndroidEntryPoint
class ChatService : Service() {

    @Inject
    lateinit var chatRepository: ChatRepository

    @Inject
    lateinit var chatNotificationHelper: ChatNotificationHelper

    @Inject
    lateinit var currentScreenManager: CurrentScreenManager

    @Inject
    lateinit var gson: Gson // (Gson이 Hilt로 주입되도록 모듈 설정 필요)

    // 서비스 자체의 CoroutineScope
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val FOREGROUND_CHANNEL_ID = "raon_service_channel"
        const val FOREGROUND_CHANNEL_NAME = "Raon 서비스"
        const val FOREGROUND_NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.raon.service.START"
        const val ACTION_STOP = "com.example.raon.service.STOP"

        // [추가] 서비스가 실행 중인지 확인하는 static 플래그
        private val isServiceRunning = AtomicBoolean(false)

        // [추가] ViewModel에서 사용할 확인용 함수
        fun isRunning(): Boolean = isServiceRunning.get()
    }

    override fun onCreate() {
        super.onCreate()
        // 서비스용 포그라운드 알림 채널 생성
        createForegroundNotificationChannel()
        // 채팅 메시지 알림 채널 생성 (기존 Helper의 로직)
        chatNotificationHelper.createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                // [수정] compareAndSet: "현재 false면 true로 바꾸고 true 반환"
                // 즉, 이 블록은 "최초 1회"만 실행됨
                if (isServiceRunning.compareAndSet(false, true)) {
                    Log.d("ChatService", "🚀 ChatService 최초 시작 (ACTION_START)")
                    // 서비스를 Foreground로 만듭니다.
                    startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification())

                    // STOMP 연결 및 메시지 수신/저장 로직 실행
                    connectAndObserveStomp()
                } else {
                    Log.d("ChatService", "🔵 ChatService는 이미 실행 중입니다.")
                }
            }

            ACTION_STOP -> {
                Log.d("ChatService", "🛑 ChatService 중지 요청 (ACTION_STOP)")
                isServiceRunning.set(false) // [추가] 중지 시 플래그를 false로
                serviceScope.cancel() // 코루틴 중지
                stopForeground(true)  // 포그라운드 상태 해제
                stopSelf()            // 서비스 스스로 종료
            }
        }
        // 서비스가 비정상 종료 시 OS가 다시 시작하도록 설정
        return START_STICKY
    }

    /**
     * STOMP 연결, DB 캐싱, 알림 구독을 모두 처리하는 메인 함수
     */
    private fun connectAndObserveStomp() {
        serviceScope.launch {
            try {
                // 1. STOMP 연결
                Log.d("ChatService", "STOMP 연결 시도...")
                // TODO: 0L 대신 실제 유저 ID나 고유 값을 넘겨야 합니다.
                chatRepository.connectStomp(0L)
                Log.d("ChatService", "✅ STOMP 연결 성공")

                // 2. [DB 저장] 메시지 캐싱 코루틴 실행
                launch {
                    Log.d("ChatService", "🚀 STOMP DB Caching Collector 시작")
                    try {
                        chatRepository.cacheStompMessages()
                    } catch (e: Exception) {
                        Log.e("ChatService", "❌ STOMP Caching (DB Save) Coroutine 실패", e)
                    }
                }

                // 3. [알림] 메시지 구독 코루틴 실행
                launch {
                    Log.d("ChatService", "🚀 STOMP Notification Observer 시작")
                    chatRepository.observeMessages(0L) // (Repo에서 DB 저장을 안 함)
                        .catch { e -> Log.e("ChatService", "❌ STOMP global observe error", e) }
                        .collect { messageJson ->
                            handleNotificationLogic(messageJson)
                        }
                }

            } catch (e: Exception) {
                Log.e("ChatService", "❌ STOMP 연결 또는 구독 실패", e)
                // TODO: 재연결 로직 (예: 5초 후 다시 connectAndObserveStomp() 호출)
                isServiceRunning.set(false) // [추가] 연결 실패 시 재시작 가능하도록 플래그 리셋
            }
        }
    }

    /**
     * MainViewModel의 observeGlobalMessages에 있던 알림 분기 로직
     */
    private fun handleNotificationLogic(messageJson: String) {
        try {
            val messageDto = gson.fromJson(messageJson, ChatMessageDto::class.java)

            val currentChatId = currentScreenManager.currentChatRoomId.value
            val isAppInForeground = currentScreenManager.isAppInForeground.value

            Log.d("NotificationDebug", "--- [ChatService] 새 메시지 수신 ---")
            Log.d("NotificationDebug", "앱 포그라운드 상태: $isAppInForeground")
            Log.d("NotificationDebug", "현재 보고있는 채팅방 ID: $currentChatId")
            Log.d("NotificationDebug", "수신된 메시지 채팅방 ID: ${messageDto.chatId}")

            if (isAppInForeground) {
                if (currentChatId == messageDto.chatId) {
                    Log.d("NotificationDebug", "판단: CASE 1 (현재 채팅방). 알림 없음.")
                } else {
                    Log.d("NotificationDebug", "판단: CASE 2 (다른 화면). 헤드업 알림 시도.")
                    chatNotificationHelper.showChatNotification(messageDto)
                }
            } else {
                Log.d("NotificationDebug", "판단: CASE 3 (백그라운드). 기본 알림 시도.")
                chatNotificationHelper.showChatNotification(messageDto)
            }
        } catch (e: Exception) {
            Log.e("ChatService", "❌ Failed to parse global message", e)
        }
    }

    override fun onDestroy() {
        Log.d("ChatService", "onDestroy: 서비스가 종료됩니다. STOMP 연결 해제 시도.")
        isServiceRunning.set(false) // [추가] 서비스가 어떤 이유로든 종료되면 플래그 리셋
        // 서비스가 종료될 때 STOMP 연결 해제
        serviceScope.launch {
            try {
                chatRepository.disconnectStomp()
                Log.d("ChatService", "✅ STOMP 연결 해제 완료")
            } catch (e: Exception) {
                Log.e("ChatService", "❌ STOMP 연결 해제 실패", e)
            }
        }
        serviceScope.cancel() // 모든 코루틴 취소
        super.onDestroy()
    }

    // ForegroundService는 onBind()가 필수이지만, 바인딩 안 할 거면 null 반환
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // --- Foreground Service Notification 관련 ---

    private fun createForegroundNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                FOREGROUND_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW // 사용자를 방해하지 않도록 LOW로 설정
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * 서비스가 실행 중임을 알리는 '영구 알림' 생성
     */
    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle("Raon 실행 중")
            .setContentText("실시간 채팅 메시지를 수신 중입니다.")
            .setSmallIcon(R.drawable.raon_icon) // TODO: 앱 아이콘으로 변경
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // 사용자가 스와이프로 지울 수 없게 함
            .build()
    }
}