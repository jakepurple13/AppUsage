package com.programmersbox.appusage

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.icu.text.DecimalFormat
import android.icu.text.SimpleDateFormat
import android.net.ConnectivityManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.programmersbox.appusage.ui.theme.AppUsageTheme
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes


class MainActivity : ComponentActivity() {
    private val appUsage = AppUsage()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppUsageTheme {

                var showDatePicker by remember { mutableStateOf(false) }
                val datePickerState = rememberDatePickerState()

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDatePicker = false
                                }
                            ) {
                                Text("OK")
                            }
                        }
                    ) {
                        DatePicker(datePickerState)
                    }
                }

                Scaffold(
                    topBar = {
                        Surface {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CenterAlignedTopAppBar(
                                    title = {
                                        Text(
                                            "Total App Usage Time:\n${getDurationBreakdown(appUsage.totalTime)}",
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    /*actions = {
                                        IconButton(
                                            onClick = { showDatePicker = true }
                                        ) {
                                            Icon(Icons.Default.DateRange, null)
                                        }
                                    }*/
                                )
                                Text(
                                    "Total Network: ${appUsage.totalNetworkStats.formatBytes()}",
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Wifi Sent: ${appUsage.totalWifiSent.formatBytes()}")
                                        Text("Wifi Received: ${appUsage.totalWifiReceived.formatBytes()}")
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Mobile Sent: ${appUsage.totalMobileSent.formatBytes()}")
                                        Text("Mobile Received: ${appUsage.totalMobileReceived.formatBytes()}")
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    LazyColumn(
                        contentPadding = innerPadding,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (appUsage.appList.isEmpty()) {
                            item {
                                Card(
                                    onClick = {
                                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                    }
                                ) {
                                    Text(
                                        text = "Apps Usage May Not Be Enabled. Please Enable It",
                                        textAlign = TextAlign.Center,
                                        fontSize = 16.sp,
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .align(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        }

                        items(appUsage.appList) {
                            OutlinedCard {
                                ListItem(
                                    headlineContent = { Text(it.appName) },
                                    leadingContent = {
                                        Image(
                                            rememberDrawablePainter(it.icon),
                                            null
                                        )
                                    },
                                    overlineContent = { Text("Last Time Used: " + it.lastUsed) },
                                    trailingContent = { Text("${it.usagePercentage}%") },
                                    supportingContent = {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            LinearProgressIndicator(
                                                progress = { it.usagePercentage.toFloat() / 100 },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Text(it.usageDuration)
                                            Text("Wifi Sent: ${it.networkInfo.wifiSent.formatBytes()}")
                                            Text("Wifi Received: ${it.networkInfo.wifiReceived.formatBytes()}")
                                            Text("Mobile Sent: ${it.networkInfo.mobileSent.formatBytes()}")
                                            Text("Mobile Received: ${it.networkInfo.mobileReceived.formatBytes()}")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appUsage.reloadData(
            context = this,
            minRange = System.currentTimeMillis() - 7.days.inWholeMilliseconds
        )
    }
}

class AppUsage {
    var totalTime by mutableLongStateOf(0)
    val appList = mutableStateListOf<AppInfo>()

    var totalMobileSent by mutableLongStateOf(0)
    var totalMobileReceived by mutableLongStateOf(0)

    var totalWifiSent by mutableLongStateOf(0)
    var totalWifiReceived by mutableLongStateOf(0)

    val totalNetworkStats by derivedStateOf {
        totalMobileSent + totalMobileReceived + totalWifiSent + totalWifiReceived
    }

    private val mDateFormat = SimpleDateFormat.getDateTimeInstance()

    fun reloadData(
        context: Context,
        minRange: Long = System.currentTimeMillis() - 1000 * 3600 * 24,
        maxRange: Long = System.currentTimeMillis()
    ) {
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val n = context.getSystemService(NetworkStatsManager::class.java)
        val appList = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            minRange,
            maxRange
        ).filter { it.totalTimeInForeground > 0 }

        totalTime = appList.sumOf { obj: UsageStats -> obj.totalTimeInForeground }

        val pm = context.packageManager

        this.appList.clear()

        appList
            .sortedByDescending { it.totalTimeInForeground }
            .mapNotNull {
                runCatching {
                    val ai = pm.getApplicationInfo(it.packageName, 0)
                    val icon = pm.getApplicationIcon(ai)
                    val name = pm.getApplicationLabel(ai).toString()
                    val usageDuration = getDurationBreakdown(it.totalTimeInForeground)
                    val usagePercentage = (it.totalTimeInForeground * 100 / totalTime)
                    val networkInfo = n.reloadNetworkStats(ai.uid)
                    AppInfo(
                        appName = name,
                        icon = icon,
                        usageDuration = usageDuration,
                        usagePercentage = usagePercentage,
                        lastUsed = mDateFormat.format(it.lastTimeUsed),
                        networkInfo = networkInfo,
                        usageStats = it
                    )
                }
                    .onFailure { it.printStackTrace() }
                    .getOrNull()
            }
            .let { this.appList.addAll(it) }
    }

    private fun NetworkStatsManager.reloadNetworkStats(
        uid: Int,
        minRange: Long = System.currentTimeMillis() - 1000 * 3600 * 24,
        maxRange: Long = System.currentTimeMillis()
    ): NetworkInfo {
        var mobileSentReceived: Pair<Long, Long>? = null
        val mobile = queryDetailsForUid(
            ConnectivityManager.TYPE_MOBILE,
            null,
            minRange,
            maxRange,
            uid
        )

        val bucket = NetworkStats.Bucket()
        while (mobile.hasNextBucket()) {
            mobile.getNextBucket(bucket)
            totalMobileReceived += bucket.rxBytes
            totalMobileSent += bucket.txBytes
            mobileSentReceived = bucket.rxBytes to bucket.txBytes
        }

        var wifiSentReceived: Pair<Long, Long>? = null
        val wifi = queryDetailsForUid(
            ConnectivityManager.TYPE_WIFI,
            null,
            minRange,
            maxRange,
            uid
        )

        val wifiBucket = NetworkStats.Bucket()
        while (wifi.hasNextBucket()) {
            wifi.getNextBucket(wifiBucket)
            totalWifiReceived += wifiBucket.rxBytes
            totalWifiSent += wifiBucket.txBytes
            wifiSentReceived = wifiBucket.rxBytes to wifiBucket.txBytes
        }

        if (wifiSentReceived == null) wifiSentReceived = 0L to 0L
        if (mobileSentReceived == null) mobileSentReceived = 0L to 0L

        return NetworkInfo(
            wifiSent = wifiSentReceived.first,
            wifiReceived = wifiSentReceived.second,
            mobileSent = mobileSentReceived.first,
            mobileReceived = mobileSentReceived.second
        )
    }
}

private fun getDurationBreakdown(millis: Long): String {
    require(millis >= 0) { "Duration must be greater than zero!" }

    var m = millis

    val hours: Long = m.milliseconds.inWholeHours
    m -= hours.hours.inWholeMilliseconds
    val minutes: Long = m.milliseconds.inWholeMinutes
    m -= minutes.minutes.inWholeMilliseconds
    val seconds: Long = m.milliseconds.inWholeSeconds

    return ("$hours h $minutes m $seconds s")
}

data class AppInfo(
    val appName: String,
    val icon: Drawable,
    val usageDuration: String,
    val usagePercentage: Long,
    val lastUsed: String,
    val networkInfo: NetworkInfo,
    val usageStats: UsageStats
)

data class NetworkInfo(
    val wifiSent: Long,
    val wifiReceived: Long,
    val mobileSent: Long,
    val mobileReceived: Long
)

fun Long.formatBytes(): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val decimalFormat = DecimalFormat("#,##0.#")
    var bytesRemaining = this
    var unitIndex = 0

    while (bytesRemaining >= 1024 && unitIndex < units.size - 1) {
        bytesRemaining /= 1024
        unitIndex++
    }

    return decimalFormat.format(bytesRemaining) + " " + units[unitIndex]
}