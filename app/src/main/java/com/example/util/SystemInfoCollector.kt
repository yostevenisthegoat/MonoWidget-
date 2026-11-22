package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import java.util.Locale

data class SystemTelemetry(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val batteryTempC: Float,
    val storageUsedGb: Float,
    val storageTotalGb: Float,
    val storageUsedPercent: Int,
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val ramUsedPercent: Int,
    val deviceModel: String,
    val androidVersion: String,
    val apiLevel: Int,
    val uptimeLabel: String,
    val isOnline: Boolean,
    val transportLabel: String,
    val isMetered: Boolean,
    val wifiConnected: Boolean,
    val wifiLinkMbps: Int,
    val wifiSignalPercent: Int,
    val wifiFrequencyMhz: Int,
    val downBandwidthMbps: Int,
    val upBandwidthMbps: Int,
    val sessionRxMb: Float,
    val sessionTxMb: Float
)

data class InstalledAppEntry(
    val label: String,
    val packageName: String
)

object SystemInfoCollector {

    fun collect(context: Context): SystemTelemetry {
        // Battery
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 85
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280
        val batteryTempC = tempTenths / 10f

        // Storage
        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalBytes = statFs.totalBytes
        val availBytes = statFs.availableBytes
        val usedBytes = (totalBytes - availBytes).coerceAtLeast(0L)
        val totalGb = totalBytes / (1024f * 1024f * 1024f)
        val usedGb = usedBytes / (1024f * 1024f * 1024f)
        val storagePct = if (totalBytes > 0L) ((usedBytes * 100) / totalBytes).toInt() else 0

        // RAM
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        val ramTotalMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(1L)
        val ramAvailMb = memInfo.availMem / (1024L * 1024L)
        val ramUsedMb = (ramTotalMb - ramAvailMb).coerceAtLeast(0L)
        val ramPct = ((ramUsedMb * 100) / ramTotalMb).toInt().coerceIn(0, 100)

        // Device
        val manufacturer = Build.MANUFACTURER.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        val model = if (Build.MODEL.startsWith(manufacturer, ignoreCase = true)) {
            Build.MODEL
        } else {
            "$manufacturer ${Build.MODEL}"
        }
        val uptimeMs = SystemClock.elapsedRealtime()
        val uptimeHrs = uptimeMs / (1000 * 60 * 60)
        val uptimeMins = (uptimeMs / (1000 * 60)) % 60
        val uptimeLabel = "${uptimeHrs}h ${uptimeMins}m"

        // Connectivity & Network Speed
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val isOnline = caps != null &&
            (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        val transportLabel = when {
            caps == null -> "Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "Connected"
        }
        val isMetered = cm?.isActiveNetworkMetered ?: false
        val downMbps = ((caps?.linkDownstreamBandwidthKbps ?: 0) / 1000).coerceAtLeast(0)
        val upMbps = ((caps?.linkUpstreamBandwidthKbps ?: 0) / 1000).coerceAtLeast(0)

        // Wi-Fi
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        @Suppress("DEPRECATION")
        val wifiInfo = wm?.connectionInfo
        val wifiConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val wifiLinkMbps = if (wifiConnected) (wifiInfo?.linkSpeed ?: 0).coerceAtLeast(0) else 0
        val rssi = wifiInfo?.rssi ?: -100
        @Suppress("DEPRECATION")
        val wifiSignalPct = if (wifiConnected) {
            WifiManager.calculateSignalLevel(rssi, 100).coerceIn(0, 100)
        } else 0
        val wifiFreq = if (wifiConnected) wifiInfo?.frequency ?: 0 else 0

        // TrafficStats
        val totalRx = TrafficStats.getTotalRxBytes()
        val totalTx = TrafficStats.getTotalTxBytes()
        val rxMb = if (totalRx != TrafficStats.UNSUPPORTED.toLong()) totalRx / (1024f * 1024f) else 0f
        val txMb = if (totalTx != TrafficStats.UNSUPPORTED.toLong()) totalTx / (1024f * 1024f) else 0f

        return SystemTelemetry(
            batteryPercent = batteryPct,
            isCharging = isCharging,
            batteryTempC = batteryTempC,
            storageUsedGb = usedGb,
            storageTotalGb = totalGb,
            storageUsedPercent = storagePct,
            ramUsedMb = ramUsedMb,
            ramTotalMb = ramTotalMb,
            ramUsedPercent = ramPct,
            deviceModel = model,
            androidVersion = Build.VERSION.RELEASE ?: "14",
            apiLevel = Build.VERSION.SDK_INT,
            uptimeLabel = uptimeLabel,
            isOnline = isOnline,
            transportLabel = transportLabel,
            isMetered = isMetered,
            wifiConnected = wifiConnected,
            wifiLinkMbps = wifiLinkMbps,
            wifiSignalPercent = wifiSignalPct,
            wifiFrequencyMhz = wifiFreq,
            downBandwidthMbps = downMbps,
            upBandwidthMbps = upMbps,
            sessionRxMb = rxMb,
            sessionTxMb = txMb
        )
    }

    fun getLaunchableApps(context: Context): List<InstalledAppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = pm.queryIntentActivities(intent, 0)
        return resolved.mapNotNull { info ->
            val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
            if (pkg == context.packageName) return@mapNotNull null
            val label = info.loadLabel(pm)?.toString() ?: pkg
            InstalledAppEntry(label = label, packageName = pkg)
        }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }
}
