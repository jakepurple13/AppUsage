package com.programmersbox.appusage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedApps(
    selectBlockedApps: Boolean,
    onDismiss: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    appList: List<AppInfo>,
    blockedApps: List<AppInfo>
) {
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState()
    if (selectBlockedApps) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    bottomSheetState.hide()
                }.invokeOnCompletion { onDismiss() }
            },
            sheetState = bottomSheetState
        ) {
            BlockedApps(
                onDismiss = {
                    scope.launch {
                        bottomSheetState.hide()
                    }.invokeOnCompletion { onDismiss() }
                },
                onAppClick = onAppClick,
                appList = appList,
                blockedApps = blockedApps
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockedApps(
    onDismiss: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    appList: List<AppInfo>,
    blockedApps: List<AppInfo>
) {
    CenterAlignedTopAppBar(
        title = { Text("Blocked Apps") },
        actions = {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        },
        windowInsets = WindowInsets(0.dp),
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(appList) { app ->
            OutlinedCard(
                onClick = { onAppClick(app) },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (blockedApps.any { b -> b.usageStats.packageName == app.usageStats.packageName })
                        Color.Red
                    else
                        MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .heightIn(min = 150.dp)
                    .animateItem()
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                        .heightIn(min = 150.dp)
                ) {
                    GradientAppIcon(app)

                    Text(
                        app.appName,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}