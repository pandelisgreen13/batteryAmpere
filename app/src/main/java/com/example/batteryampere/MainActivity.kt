package com.example.batteryampere

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
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowWidthSizeClass
import com.example.batteryampere.ui.theme.BatteryAmpereTheme
import kotlin.math.abs

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
    val viewModel: BatteryViewModel = viewModel()
    val batteryState by viewModel.batteryState.collectAsStateWithLifecycle()
    BatteryContent(batteryState = batteryState, modifier = modifier)
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun BatteryContent(batteryState: BatteryState, modifier: Modifier = Modifier) {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isExpanded = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isExpanded) {
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
                        isExpanded = true,
                        modifier = Modifier.size(400.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    AppTitle(style = MaterialTheme.typography.displayMedium)
                    ChargingStatusCard(batteryState)
                    MetricsGrid(batteryState)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                AppTitle(style = MaterialTheme.typography.headlineMedium)

                AmperageGauge(
                    currentMA = batteryState.currentMA,
                    isCharging = batteryState.isCharging,
                    isExpanded = false,
                    modifier = Modifier.size(280.dp)
                )

                ChargingStatusCard(batteryState)
                MetricsGrid(batteryState)
            }
        }
    }
}

@Composable
private fun AppTitle(style: TextStyle, modifier: Modifier = Modifier) {
    Text(
        text = "Battery Ampere",
        style = style,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
fun AmperageGauge(
    currentMA: Int,
    isCharging: Boolean,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val primaryColor = if (isCharging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val trackColor = if (isCharging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer

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
                color = trackColor.copy(alpha = 0.3f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = primaryColor,
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
                    fontSize = if (isExpanded) 80.sp else 56.sp
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
    val isPluggedIn = state.pluggedSource != "Battery"
    val displayStatus = when {
        state.isCharging -> if (state.level >= 100) "Full" else "Charging"
        isPluggedIn -> "Not Charging"
        else -> "Discharging"
    }
    val displaySubtext = when {
        state.isCharging || isPluggedIn -> "Source: ${state.pluggedSource}"
        else -> "On Battery Power"
    }
    val containerColor = if (state.isCharging || isPluggedIn)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (state.isCharging || isPluggedIn)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

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
                    contentDescription = displayStatus,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = displayStatus,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = displaySubtext,
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
    val wattsW = state.voltageV * abs(state.currentMA) / 1000f

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
                iconTint = temperatureColor(state.temperature),
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
        MetricCard(
            label = "Power",
            value = "${"%.1f".format(wattsW)} W",
            icon = Icons.Rounded.Bolt,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun temperatureColor(temp: Float): Color = when {
    temp >= 45f -> MaterialTheme.colorScheme.error
    temp >= 38f -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.Unspecified
) {
    val resolvedTint = if (iconTint == Color.Unspecified) MaterialTheme.colorScheme.primary else iconTint
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = resolvedTint,
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
