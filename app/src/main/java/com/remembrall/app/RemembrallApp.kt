package com.remembrall.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.amap.api.location.AMapLocationClient
import com.remembrall.app.notify.NotifyHelper

class RemembrallApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AMapLocationClient.updatePrivacyShow(this, true, true)
        AMapLocationClient.updatePrivacyAgree(this, true)
        NotifyHelper.createChannels(this)
    }
}
