package com.example.raon

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.raon.core.common.CurrentScreenManager
// [수정됨] 1. Helper import
import com.example.raon.core.notification.ChatNotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application(), LifecycleEventObserver {

    @Inject
    lateinit var currentScreenManager: CurrentScreenManager

    // [수정됨] 2. ChatNotificationHelper 주입
    @Inject
    lateinit var chatNotificationHelper: ChatNotificationHelper

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // [수정됨] 3. 앱 시작 시 알림 채널 생성
        chatNotificationHelper.createNotificationChannel()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                currentScreenManager.setAppInForeground(true)
            }

            Lifecycle.Event.ON_STOP -> {
                currentScreenManager.setAppInForeground(false)
            }

            else -> {}
        }
    }
}