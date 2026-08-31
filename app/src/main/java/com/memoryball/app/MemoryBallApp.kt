package com.memoryball.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.amap.api.location.AMapLocationClient
import com.memoryball.app.notify.NotifyHelper

class MemoryBallApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AMapLocationClient.updatePrivacyShow(this, true, true)
        AMapLocationClient.updatePrivacyAgree(this, true)
        NotifyHelper.createChannels(this)
    }
}
