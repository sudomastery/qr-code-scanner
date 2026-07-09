package com.sudomastery.qrscanner

import android.app.Application
import com.sudomastery.qrscanner.data.ScanDatabase
import com.sudomastery.qrscanner.data.SettingsRepository

class QrApp : Application() {

    val database: ScanDatabase by lazy { ScanDatabase.build(this) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
}
