package com.example.batteryampere

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowWidthSizeClass
import com.example.batteryampere.ui.theme.BatteryAmpereTheme
import kotlinx.coroutines.delay
import kotlin.math.*

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BatteryAmpereTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BatteryScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun BatteryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val batteryManager = remember { context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager }
    
    var batteryState by remember { mutableStateOf(BatteryState()) }

    LaunchedEffect(Unit) {
        while (true) {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            
            val currentNow = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            val processedCurrent = if (abs(currentNow) > 10000) currentNow / 1000 else currentNow
            
            val voltageMV = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
            val voltageV = voltageMV / 1000f
            
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale) else 0
            
            val tempDC = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN) 
                ?: BatteryManager.BATTERY_STATUS_UNKNOWN
            
            val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            val pluggedSource = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> "Battery"
            }
            
            val technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
            
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                             status == BatteryManager.BATTERY_STATUS_FULL

            batteryState = BatteryState(
                currentMA = processedCurrent,
                voltageV = voltageV,
                level = batteryPct,
                temperature = tempDC,
                status = status,
                isCharging = isCharging,
                pluggedSource = pluggedSource,
                technology = technology
            )
            
            delay(1000)
        }
    }

    BatteryContent(batteryState = batteryState, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryContent(batteryState: BatteryState, modifier: Modifier = Modifier) {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isExpanded = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isExpanded) {
            // Tablet / Desktop Layout: Side-by-side
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AmperageGauge(
                        currentMA = batteryState.currentMA,
                        isCharging = batteryState.isCharging,
                        modifier = Modifier.size(400.dp)
                    )
                }
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "Battery Ampere",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    ChargingStatusCard(batteryState)
                    MetricsGrid(batteryState)
                }
            }
        } else {
            // Mobile Layout: Vertical
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Battery Ampere",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                AmperageGauge(
                    currentMA = batteryState.currentMA,
                    isCharging = batteryState.isCharging,
                    modifier = Modifier.size(280.dp)
                )

                ChargingStatusCard(batteryState)

                MetricsGrid(batteryState)
            }
        }
    }
}

@Composable
fun AmperageGauge(currentMA: Int, isCharging: Boolean, modifier: Modifier = Modifier) {
    val primaryColor = if (isCharging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val secondaryColor = if (isCharging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    
    val maxMA = 5000f
    val clampedMA = currentMA.toFloat().coerceIn(-maxMA, maxMA)
    val sweepAngle = 240f
    val startAngle = 150f
    
    val progress = (clampedMA + maxMA) / (2 * maxMA)
    val currentAngle = sweepAngle * progress

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.width * 0.08f
            drawArc(
                color = secondaryColor.copy(alpha = 0.3f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    0f to secondaryColor,
                    0.5f to primaryColor,
                    1f to primaryColor
                ),
                startAngle = startAngle,
                sweepAngle = currentAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${abs(currentMA)}",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = if (modifier.toString().contains("400")) 80.sp else 56.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = primaryColor
            )
            Text(
                text = "mA",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ChargingStatusCard(state: BatteryState) {
    val containerColor = if (state.isCharging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (state.isCharging) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = if (state.isCharging) Icons.Rounded.Bolt else Icons.Rounded.BatteryStd,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (state.isCharging) "Charging" else "Discharging",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (state.isCharging) "Source: ${state.pluggedSource}" else "On Battery Power",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Text(
                text = "${state.level}%",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MetricsGrid(state: BatteryState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricCard(
                label = "Voltage",
                value = "${state.voltageV} V",
                icon = Icons.Rounded.ElectricBolt,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Temperature",
                value = "${state.temperature} °C",
                icon = Icons.Rounded.Thermostat,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricCard(
                label = "Technology",
                value = state.technology,
                icon = Icons.Rounded.Memory,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Status",
                value = getStatusString(state.status),
                icon = Icons.Rounded.Info,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

private fun getStatusString(status: Int): String = when (status) {
    BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
    BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
    BatteryManager.BATTERY_STATUS_FULL -> "Full"
    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
    else -> "Unknown"
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BatteryScreenPreview() {
    BatteryAmpereTheme {
        BatteryContent(
            batteryState = BatteryState(
                currentMA = 450,
                voltageV = 4.2f,
                level = 85,
                temperature = 32.5f,
                isCharging = true,
                pluggedSource = "USB",
                technology = "Li-ion"
            )
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun BatteryScreenTabletPreview() {
    BatteryAmpereTheme {
        BatteryContent(
            batteryState = BatteryState(
                currentMA = -1200,
                voltageV = 3.8f,
                level = 42,
                temperature = 38.2f,
                isCharging = false,
                technology = "Li-poly"
            )
        )
    }
}
