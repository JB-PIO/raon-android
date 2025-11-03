package com.example.raon.features.chat.data.remote

import android.util.Log
import com.example.raon.features.auth.data.local.TokenManager
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException // 👈 CancellationException import
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.config.HeartBeat // 👈 HeartBeat import
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds // 👈 seconds import

// data class ChatMessageDto 는 StompService 파일 외부 또는 다른 DTO 파일에 정의되어야 함

@Singleton
class StompService @Inject constructor(
    private val tokenManager: TokenManager
) {

    private val stompClient = StompClient(OkHttpWebSocketClient()) {
        // ▼▼▼ 하트비트 설정 ▼▼▼
        heartBeat = HeartBeat(
            10.seconds, // 보내는 간격
            10.seconds  // 받는 간격
        )
        // ▲▲▲ 여기까지 ▲▲▲
    }
    private var session: StompSession? = null

    private val _messages = MutableSharedFlow<String>()
    val messages: Flow<String> get() = _messages.asSharedFlow()

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun connectAndSubscribe(chatRoomId: Long) {
        try {
            // ▼▼▼ 기존 로직 유지: 이미 세션이 있으면 함수 종료 ▼▼▼
            if (session != null) {
                Log.d("StompService", "Session already exists. Skipping connection.")
                return
            }
            // ▲▲▲ 여기까지 ▲▲▲

            val authToken = tokenManager.getAccessToken()

            if (authToken.isNullOrBlank()) {
                Log.e("StompService", "Auth token is null or blank. Connection aborted.")
                return
            }

            Log.d("StompService", "Attempting STOMP connection...") // 토큰 값 로그 제거

            val webSocketUrl = "ws://158.179.164.210/ws"

            // ▼▼▼ passcode 방식 유지 ▼▼▼
            session = stompClient.connect(
                url = webSocketUrl,
                passcode = authToken // 요청하신대로 passcode 사용
            )
            // ▲▲▲ 여기까지 ▲▲▲

            Log.d("StompService", "✅ STOMP connection successful! Session created.")

            val destination = "/user/chat"
            Log.d("StompService", "Subscribing to destination: $destination")

            // --- 메시지 구독 로직 (수정된 에러 처리 포함) ---
            scope.launch {
                try {
                    session?.subscribeText(destination)
                        ?.mapNotNull { chatmessage ->
                            try {
                                Log.d("StompService", "수신 데이터: $chatmessage")
                                _messages.emit(chatmessage)
                                chatmessage
                            } catch (e: Exception) {
                                Log.e("StompService", "Message emit or processing failed", e)
                                null
                            }
                        }
                        ?.catch { e ->
                            Log.e("StompService", "!!! Error in message receiving flow", e)
                            disconnect() // 에러 시 세션 정리 (null로 만듦)
                        }
                        ?.collect {
                            // No action needed here
                        }
                } catch (e: CancellationException) {
                    Log.d("StompService", "Subscription scope cancelled. Disconnecting session.")
                    disconnect() // 코루틴 취소 시 세션 정리
                } catch (e: Exception) {
                    Log.e(
                        "StompService",
                        "!!! Unhandled exception in subscription setup or flow",
                        e
                    )
                    disconnect() // 예상 못한 에러 시 세션 정리
                } finally {
                    Log.d("StompService", "Subscription flow ended.")
                    // Flow 종료 시 disconnect 호출은 주석 처리
                    // disconnect()
                }
            } // scope.launch 끝
        } catch (e: Exception) {
            // stompClient.connect 자체가 실패한 경우
            Log.e("StompService", "!!! STOMP connection failed", e)
            disconnect() // 연결 실패 시에도 세션 정리 (null로 만듦)
        }
    }

    suspend fun disconnect() {
        try {
            session?.disconnect() // null-safe 호출
            Log.d("StompService", "Stomp 연결종료 : Attempted to disconnect STOMP session.")
        } catch (e: Exception) {
            Log.e("StompService", "Failed to gracefully disconnect STOMP", e)
        } finally {
            if (session != null) {
                Log.d("StompService", "Setting session variable to null.")
            }
            session = null // 항상 null로 설정하여 정리
        }
    }
}