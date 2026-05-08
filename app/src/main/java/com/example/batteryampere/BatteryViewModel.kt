package com.example.batteryampere

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlin.math.abs

class BatteryViewModel(application: Application) : AndroidViewModel(application) {

    private val batteryManager =
        application.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    val batteryState = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(buildState(intent))
            }
        }

        val stickyIntent = getApplication<Application>()
            .registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        stickyIntent?.let { trySend(buildState(it)) }

        getApplication<Application>().registerReceiver(
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        awaitClose { getApplication<Application>().unregisterReceiver(receiver) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BatteryState()
    )

    private fun buildState(intent: Intent): BatteryState {
        val currentNow = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val processedCurrent = if (abs(currentNow) > 10_000) currentNow / 1000 else currentNow

        val voltageMV = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val voltageV = voltageMV / 1000f

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale) else 0

        val tempDC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)

        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val pluggedSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Battery"
        }

        val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        return BatteryState(
            currentMA = processedCurrent,
            voltageV = voltageV,
            level = batteryPct,
            temperature = tempDC,
            status = status,
            isCharging = isCharging,
            pluggedSource = pluggedSource,
            technology = technology
        )
    }
}
