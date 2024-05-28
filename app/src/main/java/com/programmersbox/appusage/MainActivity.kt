package com.programmersbox.appusage

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.icu.text.DateFormat
import android.icu.text.DecimalFormat
import android.icu.text.SimpleDateFormat
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.programmersbox.appusage.ui.theme.AppUsageTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class MainActivity : ComponentActivity() {
    private val appUsage = AppUsage()
    private val mDateFormat = SimpleDateFormat.getDateInstance()

    private var beginTime by mutableLongStateOf(
        mDateFormat.let { justDay -> justDay.parse(justDay.format(Date())) }.time
    )
    private var endTime by mutableLongStateOf(System.currentTimeMillis())

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppUsageTheme {
                val scope = rememberCoroutineScope()
                val bottomAppBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()

                var showBlockedApps by remember { mutableStateOf(false) }

                BlockedApps(
                    selectBlockedApps = showBlockedApps,
                    onDismiss = { showBlockedApps = false },
                    onAppClick = { appUsage.blockedApps.toggle(it) },
                    appList = appUsage.appList,
                    blockedApps = appUsage.blockedApps
                )

                var showDatePicker by remember { mutableStateOf(false) }
                val datePickerState = rememberDateRangePickerState(
                    initialSelectedStartDateMillis = beginTime,
                    initialSelectedEndDateMillis = endTime
                )

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDatePicker = false
                                    datePickerState.selectedStartDateMillis
                                        ?.let { beginTime = it + 1.days.inWholeMilliseconds }
                                    datePickerState.selectedEndDateMillis
                                        ?.let { endTime = it + 1.days.inWholeMilliseconds }

                                    scope.launch {
                                        appUsage.reloadData(
                                            context = this@MainActivity,
                                            minRange = beginTime,
                                            maxRange = endTime
                                        )
                                    }
                                }
                            ) { Text("Save") }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showDatePicker = false }
                            ) { Text("Cancel") }
                        }
                    ) {
                        DateRangePicker(
                            datePickerState,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }

                Scaffold(
                    topBar = {
                        Surface {
                            val breakdown = getDurationBreakdown(appUsage.totalTime.animate())
                            val totalNetwork = appUsage.totalNetworkStats
                                .animate().formatBytes()
                            val wifiSent = appUsage.totalWifiSent
                                .animate().formatBytes()
                            val wifiReceived = appUsage.totalWifiReceived
                                .animate().formatBytes()
                            val mobileSent = appUsage.totalMobileSent
                                .animate().formatBytes()
                            val mobileReceived = appUsage.totalMobileReceived
                                .animate().formatBytes()

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CenterAlignedTopAppBar(
                                    navigationIcon = {
                                        IconButton(onClick = { showBlockedApps = true }) {
                                            Icon(Icons.Default.AppBlocking, null)
                                        }
                                    },
                                    title = {
                                        Text(
                                            "Total App Usage Time:\n$breakdown",
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    actions = {
                                        var showMenu by remember { mutableStateOf(false) }
                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false }
                                        ) {
                                            DropdownMenuItems(
                                                datePickerState = datePickerState,
                                                mDateFormat = mDateFormat,
                                                onSelected = { start, end ->
                                                    beginTime = start
                                                    endTime = end
                                                    scope.launch {
                                                        appUsage.reloadData(
                                                            context = this@MainActivity,
                                                            minRange = beginTime,
                                                            maxRange = endTime
                                                        )
                                                    }
                                                },
                                                onDismiss = { showMenu = false }
                                            )
                                        }
                                        IconButton(
                                            onClick = { showMenu = true }
                                        ) { Icon(Icons.Default.Menu, null) }
                                    }
                                )

                                Text(
                                    "Total Network: $totalNetwork",
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Wifi Sent: $wifiSent")
                                        Text("Wifi Received: $wifiReceived")
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Mobile Sent: $mobileSent")
                                        Text("Mobile Received: $mobileReceived")
                                    }
                                }
                            }
                        }
                    },
                    bottomBar = {
                        BottomAppBar(
                            scrollBehavior = bottomAppBarScrollBehavior,
                            floatingActionButton = {
                                FloatingActionButton(
                                    onClick = {
                                        showDatePicker = true
                                    }
                                ) { Icon(Icons.Default.DateRange, null) }
                            },
                            actions = {
                                Text(
                                    mDateFormat.format(beginTime.animate())
                                            + " - "
                                            + mDateFormat.format(endTime.animate()),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        )
                    },
                    modifier = Modifier
                        .nestedScroll(bottomAppBarScrollBehavior.nestedScrollConnection)
                        .fillMaxSize()
                ) { innerPadding ->
                    CustomPullToRefreshBox(
                        isRefreshing = appUsage.isLoading,
                        paddingValues = innerPadding,
                        onRefresh = {
                            scope.launch {
                                appUsage.reloadData(
                                    context = this@MainActivity,
                                    minRange = beginTime,
                                    maxRange = endTime
                                )
                            }
                        },
                        enabled = { !appUsage.isLoading }
                    ) {
                        LazyColumn(
                            contentPadding = innerPadding,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                AnimatedVisibility(appUsage.isLoading) {
                                    CircularProgressIndicator()
                                }
                            }

                            if (appUsage.allowedAppList.isNotEmpty()) {
                                item {
                                    Chart(appUsage.allowedAppList)
                                }
                            }

                            if (appUsage.appList.isEmpty()) {
                                item {
                                    ElevatedCard(
                                        onClick = {
                                            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                        }
                                    ) {
                                        Text(
                                            text = "Apps Usage May Not Be Enabled. Please Enable It",
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.titleLarge,
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .align(Alignment.CenterHorizontally)
                                        )
                                    }
                                }
                            }

                            items(
                                appUsage.allowedAppList,
                                key = { it.usageStats.packageName }
                            ) {
                                AppItem(
                                    appInfo = it,
                                    modifier = Modifier.animateItem()
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
        lifecycleScope.launch(Dispatchers.IO) {
            appUsage.reloadData(
                context = this@MainActivity,
                minRange = beginTime,
                maxRange = endTime
            )
        }
    }
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownMenuItems(
    datePickerState: DateRangePickerState,
    mDateFormat: DateFormat,
    onSelected: (Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    fun datePresetSelected(
        start: Long,
        end: Long
    ) {
        datePickerState.setSelection(
            startDateMillis = start,
            endDateMillis = end
        )
        onSelected(start, end)
        onDismiss()
    }

    DropdownMenuItem(
        onClick = {
            val start = mDateFormat.let { justDay ->
                justDay.parse(justDay.format(Date()))
            }.time
            val end = System.currentTimeMillis()
            datePresetSelected(start, end)
        },
        text = { Text("Today") }
    )

    DropdownMenuItem(
        onClick = {
            val start = mDateFormat.let { justDay ->
                justDay.parse(justDay.format(Date()))
            }.time - 7.days.inWholeMilliseconds
            val end = System.currentTimeMillis()
            datePresetSelected(start, end)
        },
        text = { Text("Week") }
    )

    DropdownMenuItem(
        onClick = {
            val start = mDateFormat.let { justDay ->
                justDay.parse(justDay.format(Date().apply { month -= 1 }))
            }.time
            val end = System.currentTimeMillis()
            datePresetSelected(start, end)
        },
        text = { Text("Month") }
    )

    DropdownMenuItem(
        onClick = {
            val start = mDateFormat.let { justDay ->
                justDay.parse(justDay.format(Date().apply { year -= 1 }))
            }.time
            val end = System.currentTimeMillis()
            datePresetSelected(start, end)
        },
        text = { Text("Year") }
    )
}

@Composable
private fun AppItem(appInfo: AppInfo, modifier: Modifier = Modifier) {
    OutlinedCard(
        modifier = modifier
    ) {
        ListItem(
            headlineContent = { Text(appInfo.appName) },
            leadingContent = { GradientAppIcon(appInfo) },
            overlineContent = { Text(appInfo.usageStats.packageName) },
            trailingContent = { Text("${appInfo.usagePercentage}%") },
            supportingContent = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { appInfo.usagePercentage.toFloat() / 100 },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(appInfo.usageDuration)
                    Text("Last Time Used: " + appInfo.lastUsed)
                    Column {
                        Text("Wifi")
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sent: ${appInfo.networkInfo.wifiSent.formatBytes()}")
                            Text("Received: ${appInfo.networkInfo.wifiReceived.formatBytes()}")
                        }
                    }
                    Column {
                        Text("Mobile Data")
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sent: ${appInfo.networkInfo.mobileSent.formatBytes()}")
                            Text("Received: ${appInfo.networkInfo.mobileReceived.formatBytes()}")
                        }
                    }
                    Text("Times opened: ${appInfo.timesOpened}")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(categoryColor(appInfo.category.id)),
                        )
                        Text("Category: ${appInfo.category.text}")
                    }
                }
            }
        )
    }
}

class AppUsage {
    var isLoading by mutableStateOf(true)
    var totalTime by mutableLongStateOf(0)
    val blockedApps = mutableStateListOf<AppInfo>()
    val appList = mutableStateListOf<AppInfo>()

    val allowedAppList by derivedStateOf {
        appList.filterNot {
            blockedApps.any { b -> b.usageStats.packageName == it.usageStats.packageName }
        }
    }

    var totalMobileSent by mutableLongStateOf(0)
    var totalMobileReceived by mutableLongStateOf(0)

    var totalWifiSent by mutableLongStateOf(0)
    var totalWifiReceived by mutableLongStateOf(0)

    val totalNetworkStats by derivedStateOf {
        totalMobileSent + totalMobileReceived + totalWifiSent + totalWifiReceived
    }

    private val mDateFormat = SimpleDateFormat.getDateTimeInstance()

    suspend fun reloadData(
        context: Context,
        minRange: Long = System.currentTimeMillis() - 1000 * 3600 * 24,
        maxRange: Long = System.currentTimeMillis()
    ) {
        isLoading = true
        totalWifiSent = 0
        totalWifiReceived = 0
        totalMobileSent = 0
        totalMobileReceived = 0
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val n = context.getSystemService(NetworkStatsManager::class.java)

        val names = usm.timesOpened(minRange, maxRange)

        val appList = usm.queryAndAggregateUsageStats(
            minRange,
            maxRange
        ).filter { it.value.totalTimeInForeground > 0 }

        totalTime = appList.values.sumOf { obj: UsageStats -> obj.totalTimeInForeground }

        val pm = context.packageManager

        appList
            .values
            .sortedByDescending { it.totalTimeInForeground }
            .mapNotNull {
                runCatching {
                    val ai = pm.getApplicationInfo(it.packageName, 0)
                    val icon = pm.getApplicationIcon(ai)
                    val name = pm.getApplicationLabel(ai).toString()
                    val usageDuration = getDurationBreakdown(it.totalTimeInForeground)
                    val usagePercentage = (it.totalTimeInForeground * 100 / totalTime)
                    val networkInfo = n.reloadNetworkStats(ai.uid)
                    val category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ai.category
                    } else {
                        -1
                    }
                    AppInfo(
                        appName = name,
                        icon = icon,
                        usageDuration = usageDuration,
                        usagePercentage = usagePercentage,
                        lastUsed = mDateFormat.format(it.lastTimeUsed),
                        networkInfo = networkInfo,
                        timesOpened = names[it.packageName] ?: 0,
                        usageStats = it,
                        category = CategoryInformation(
                            id = category,
                            text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                ApplicationInfo.getCategoryTitle(context, category)
                                    ?.toString()
                                    ?: "Undefined"
                            } else {
                                "Undefined"
                            }
                        ),
                    )
                }
                    .onFailure { it.printStackTrace() }
                    .getOrNull()
            }
            .let {
                this.appList.clear()
                this.appList.addAll(it)
            }
        isLoading = false
    }

    @Suppress("DEPRECATION")
    private suspend fun NetworkStatsManager.reloadNetworkStats(
        uid: Int,
        minRange: Long = System.currentTimeMillis() - 1000 * 3600 * 24,
        maxRange: Long = System.currentTimeMillis()
    ): NetworkInfo {
        val mobileSentReceived = networkBucket(
            minRange = minRange,
            maxRange = maxRange,
            uid = uid,
            connectionType = ConnectivityManager.TYPE_MOBILE,
            totalAdditionReceived = { totalMobileReceived += it },
            totalAdditionSent = { totalMobileSent += it }
        )

        val wifiSentReceived = networkBucket(
            minRange = minRange,
            maxRange = maxRange,
            uid = uid,
            connectionType = ConnectivityManager.TYPE_WIFI,
            totalAdditionReceived = { totalWifiReceived += it },
            totalAdditionSent = { totalWifiSent += it }
        )

        return NetworkInfo(
            wifiSent = wifiSentReceived.sent,
            wifiReceived = wifiSentReceived.received,
            mobileSent = mobileSentReceived.sent,
            mobileReceived = mobileSentReceived.received
        )
    }

    private suspend fun NetworkStatsManager.networkBucket(
        minRange: Long = System.currentTimeMillis() - 1000 * 3600 * 24,
        maxRange: Long = System.currentTimeMillis(),
        uid: Int,
        connectionType: Int,
        totalAdditionReceived: (Long) -> Unit,
        totalAdditionSent: (Long) -> Unit
    ): NetworkSentReceive {
        val sentReceived = NetworkSentReceive(0, 0)
        val stats = withContext(Dispatchers.IO) {
            queryDetailsForUid(
                connectionType,
                null,
                minRange,
                maxRange,
                uid
            )
        }

        val bucket = NetworkStats.Bucket()
        while (stats.hasNextBucket()) {
            stats.getNextBucket(bucket)
            totalAdditionReceived(bucket.rxBytes)
            totalAdditionSent(bucket.txBytes)
            sentReceived.sent += bucket.txBytes
            sentReceived.received += bucket.rxBytes
        }
        return sentReceived
    }

    /**
     * This does technically work, it just doesn't show it in real time.
     * Only on next open.
     */
    private fun UsageStatsManager.timesOpened(
        minRange: Long = System.currentTimeMillis() - 1000 * 3600 * 24,
        maxRange: Long = System.currentTimeMillis()
    ): Map<String, Int> {
        val names = mutableMapOf<String, Int>()
        val events = queryEvents(minRange, maxRange)
        val ev = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            if (ev.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                if (names.containsKey(ev.packageName)) {
                    names[ev.packageName] = names[ev.packageName]!! + 1
                } else {
                    names[ev.packageName] = 1
                }
            }
        }
        return names
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
    val timesOpened: Int,
    val networkInfo: NetworkInfo,
    val usageStats: UsageStats,
    val category: CategoryInformation
)

data class CategoryInformation(
    val id: Int,
    val text: String,
)

data class NetworkInfo(
    val wifiSent: Long,
    val wifiReceived: Long,
    val mobileSent: Long,
    val mobileReceived: Long
)

data class NetworkSentReceive(
    var sent: Long,
    var received: Long
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

@Composable
fun Long.animate() = animateValueAsState(
    this,
    TwoWayConverter({ AnimationVector1D(it.toFloat()) }, { it.value.toLong() }),
    label = ""
).value

fun <T> MutableList<T>.toggle(item: T) = if (item in this) remove(item) else add(item)