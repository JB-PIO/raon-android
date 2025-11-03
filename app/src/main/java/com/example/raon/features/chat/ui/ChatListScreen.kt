package com.example.raon.features.chat.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
// [수정] DTO(ChatRoomInfo) 대신 Domain Model(ChatRoom)을 import 합니다.
import com.example.raon.features.chat.domain.model.ChatRoom
import com.example.raon.ui.theme.BrandDarkText
import com.example.raon.ui.theme.BrandYellow
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit


/**
 * TopAppBar: MainView에서 호출하여 사용합니다. (변경 없음)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListTopAppBar(navController: NavController) {
    TopAppBar(
        title = { Text("채팅방") },
//        actions = {
//            IconButton(onClick = { navController.navigate("settings_screen") }) {
//                Icon(
//                    imageVector = Icons.Default.Settings,
//                    contentDescription = "설정으로 이동"
//                )
//            }
//        }
    )
}

@Composable
fun ChatListScreen(
    onChatRoomClick: (chatRoomId: Long, opponentId: Int, itemId: Long) -> Unit,
    myUserId: Int, // 👈 파라미터로 myUserId를 추가합니다.
    // [수정] DTO(ChatRoomInfo) 대신 Domain Model(ChatRoom)을 받습니다.
    chatRooms: List<ChatRoom>
) {


    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = chatRooms,
            // [수정] chatRoom.chatId -> chatRoom.roomId (도메인 모델의 필드명 사용)
            key = { it.roomId } // 각 아이템의 고유 키
        ) { chatRoom -> // 'chatRoom'은 이제 List<ChatRoom>의 'ChatRoom' 타입입니다.
            ChatListItem(
                chatRoom = chatRoom,
                myUserId = myUserId,
                onClick = {
                    // [수정] Room에서 가져온 도메인 모델의 ID를 사용합니다.
                    // (이 ID들이 ChatRoom 모델에 포함되어 있어야 합니다)
                    val opponentId = if (myUserId == chatRoom.sellerId) {
                        chatRoom.buyerId // 내가 판매자면 상대방은 구매자 ID
                    } else {
                        chatRoom.sellerId // 내가 구매자면 상대방은 판매자 ID
                    }

                    // 채티방 터치 이벤트 발생시 실행하는 함수 -> chatroomId, sellerId 전달
                    onChatRoomClick(
                        chatRoom.roomId,    // [수정] 채팅방 Id (Room)
                        opponentId,         // [수정] 상대방 Id (Room)
                        chatRoom.productId.toLong()  // [수정] 채팅방 Item Id (Room)
                    )

                    Log.d("채팅프로세스1", "ChatListScreen ->  채팅방 ID: ${chatRoom.roomId}")
                }
            )
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
        }
    }
}

@Composable
private fun ChatListItem(
    // [수정] DTO(ChatRoomInfo) 대신 Domain Model(ChatRoom)을 받습니다.
    chatRoom: ChatRoom,
    myUserId: Int,
    onClick: () -> Unit
) {
    // [삭제] 'opponent' 객체 로직 (Domain Model에 이미 opponentName이 있음)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 상대방 프로필 이미지 (또는 상품 썸네일)
        Log.d("chatroomList", "url확인 : ${chatRoom.opponentProfileUrl}")

        AsyncImage(
            // [수정] Domain Model의 필드(opponentProfileUrl)를 사용합니다. (Room)
            model = chatRoom.opponentProfileUrl,
            contentDescription = "채팅 상대 프로필 이미지",
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 'myUserId'를 사용해 상대방 닉네임을 결정합니다.
                val opponentName = if (myUserId == chatRoom.sellerId) {
                    chatRoom.buyerNickname
                } else {
                    chatRoom.sellerNickname
                }

                Text(
                    // [수정] Domain Model의 필드(opponentName)를 사용합니다. (Room)
                    text = opponentName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    // [수정] Domain Model의 필드(lastMessageTime)를 사용합니다. (Room)
                    text = formatTimeAgo(chatRoom.lastMessageTime),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 마지막 채팅 내용
            Text(
                // [수정] Domain Model의 필드(lastMessage)를 사용합니다. (Room)
                text = chatRoom.lastMessage.ifEmpty { "대화 내용이 없습니다." },
                color = Color.Gray,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 안 읽은 메시지 개수 (있을 경우에만 표시)
        // [수정] Domain Model의 필드(unreadCount)를 사용합니다. (Room)
        if (chatRoom.unreadCount > 0) {
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = chatRoom.unreadCount.toString(),
                color = BrandDarkText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(BrandYellow, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

// 시간 포맷팅을 위한 헬퍼 함수
@Composable
// [수정] Domain Model의 lastMessageTime은 null이 아니므로 String을 받습니다.
private fun formatTimeAgo(dateTimeString: String): String {
    if (dateTimeString.isEmpty()) return ""
    return try {
        // Z(UTC) 정보가 없는 시간이므로 "T"를 추가하고 임의의 Z를 붙여 OffsetDateTime으로 파싱
        val createdTime = OffsetDateTime.parse(dateTimeString.replace(" ", "T") + "Z")
        val now = OffsetDateTime.now()

        val minutes = ChronoUnit.MINUTES.between(createdTime, now)
        val hours = ChronoUnit.HOURS.between(createdTime, now)
        val days = ChronoUnit.DAYS.between(createdTime, now)

        when {
            minutes < 1 -> "방금 전"
            minutes < 60 -> "${minutes}분 전"
            hours < 24 -> "${hours}시간 전"
            days < 7 -> "${days}일 전"
            else -> "${createdTime.monthValue}월 ${createdTime.dayOfMonth}일"
        }
    } catch (e: Exception) {
        Log.e("ChatListScreen", "formatTimeAgo 파싱 실패: $dateTimeString", e)
        "시간 정보 없음"
    }
}