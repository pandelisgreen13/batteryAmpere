package com.example.batteryampere

import android.os.BatteryManager

data class BatteryState(
    val currentMA: Int = 0,
    val voltageV: Float = 0f,
    val level: Int = 0,
    val temperature: Float = 0f,
    val status: Int = BatteryManager.BATTERY_STATUS_UNKNOWN,
    val isCharging: Boolean = false,
    val pluggedSource: String = "Battery",
    val technology: String = "Unknown"
)
