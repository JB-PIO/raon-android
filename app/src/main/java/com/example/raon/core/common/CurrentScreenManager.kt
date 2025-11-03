package com.example.raon.core.common


import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton


// 현재 화면이 어디 인지를 담아 둘 싱글턴 객체
// 1. 어디 화면 인지
// 2. 앱의 상태가 어떤지

@Singleton
class CurrentScreenManager @Inject constructor() {

    // 1. 현재 사용자가 보고 있는 채팅방의 ID
    //    (null 이면 채팅방이 아닌 다른 화면에 있다는 의미)
    private val _currentChatRoomId = MutableStateFlow<Long?>(null)
    val currentChatRoomId: StateFlow<Long?> = _currentChatRoomId.asStateFlow()

    fun setCurrentChatRoom(roomId: Long) {
        _currentChatRoomId.value = roomId
    }

    fun clearCurrentChatRoom() {
        _currentChatRoomId.value = null
    }

    // 2. 앱이 포그라운드에 있는지 여부
    private val _isAppInForeground = MutableStateFlow(false)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    fun setAppInForeground(isInForeground: Boolean) {
        _isAppInForeground.value = isInForeground
    }
}