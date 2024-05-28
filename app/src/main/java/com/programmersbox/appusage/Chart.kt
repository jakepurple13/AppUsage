package com.programmersbox.appusage

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.ehsannarmani.compose_charts.RowChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.IndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.PopupProperties
import kotlinx.coroutines.launch
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

private const val DEFAULT_ROW_HEIGHT = 100
private const val ROW_HEIGHT_400 = 400 - DEFAULT_ROW_HEIGHT

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Chart(
    appList: List<AppInfo>,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val appsList by remember {
        derivedStateOf {
            appList
                .groupBy { it.category }
                .map {
                    Bars(
                        label = it.key.text,
                        values = listOf(
                            Bars.Data(
                                label = it.key.text,
                                value = it
                                    .value
                                    .sumOf { a -> a.usageStats.totalTimeInForeground }
                                    .toDouble(),
                                color = SolidColor(categoryColor(it.key.id))
                            )
                        ),
                    )
                }
                .sortedByDescending { it.values.sumOf { v -> v.value } }
        }
    }

    if (showSheet) {
        ChartSheet(
            appList = appList,
            sheetState = sheetState,
            onDismiss = {
                scope.launch { sheetState.hide() }
                    .invokeOnCompletion { showSheet = false }
            }
        )
    }

    ElevatedCard(
        modifier = modifier
    ) {
        RowChart(
            data = appsList,
            barProperties = BarProperties(
                cornerRadius = Bars.Data.Radius.Rectangle(
                    topRight = 6.dp,
                    bottomRight = 6.dp
                ),
                spacing = 2.dp,
                thickness = 20.dp
            ),
            labelHelperProperties = LabelHelperProperties(
                false,
                textStyle = MaterialTheme.typography.bodyMedium
                    .copy(fontSize = 12.sp)
                    .copy(color = MaterialTheme.colorScheme.onSurface)
            ),
            labelProperties = LabelProperties(
                true,
                textStyle = MaterialTheme.typography.bodyMedium
                    .copy(fontSize = 12.sp)
                    .copy(color = MaterialTheme.colorScheme.onSurface)
            ),
            indicatorProperties = IndicatorProperties(
                true,
                textStyle = MaterialTheme.typography.bodyMedium
                    .copy(fontSize = 12.sp)
                    .copy(color = MaterialTheme.colorScheme.onSurface)
            ) { getDurationBreakdown(it.roundToLong()) },
            popupProperties = PopupProperties(
                false,
                textStyle = MaterialTheme.typography.bodyMedium
                    .copy(fontSize = 12.sp)
                    .copy(color = MaterialTheme.colorScheme.onSurface),
                contentBuilder = { getDurationBreakdown(it.roundToLong()) }
            ),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            animationMode = AnimationMode.Together { it * 100L },
            animationDelay = 300,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height((DEFAULT_ROW_HEIGHT + ((ROW_HEIGHT_400 / 9) * appsList.size)).dp)
        )

        TextButton(
            onClick = { showSheet = true },
            modifier = Modifier.align(Alignment.End)
        ) { Text("View More") }
    }
}

fun categoryColor(category: Int) = when (category) {
    0 -> Color.Red
    1 -> Color.Blue
    2 -> Color(0xFFeb3b5a)
    3 -> Color.Yellow
    4 -> Color.Magenta
    5 -> Color.Cyan
    6 -> Color(0xFF0fb9b1)
    7 -> Color(0xFFf7b731)
    8 -> Color.Green
    else -> Color(0xFF20bf6b)
}

private fun getDurationBreakdown(millis: Long): String {
    require(millis >= 0) { "Duration must be greater than zero!" }

    var m = millis

    val hours: Long = m.milliseconds.inWholeHours
    m -= hours.hours.inWholeMilliseconds
    val minutes: Long = m.milliseconds.inWholeMinutes
    m -= minutes.minutes.inWholeMilliseconds

    return ("${hours}h ${minutes}m")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChartSheet(
    appList: List<AppInfo>,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        CenterAlignedTopAppBar(
            title = { Text("Categories") },
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

        val categoryList by remember {
            derivedStateOf {
                appList.groupBy { it.category }
            }
        }

        val filteredCategories = remember {
            mutableStateListOf<String>()
        }

        val appsList by remember {
            derivedStateOf {
                categoryList
                    .filter { if (filteredCategories.isEmpty()) true else it.key.text in filteredCategories }
                    .map {
                        Bars(
                            label = it.key.text,
                            values = listOf(
                                Bars.Data(
                                    label = it.key.text,
                                    value = it
                                        .value
                                        .sumOf { a -> a.usageStats.totalTimeInForeground }
                                        .toDouble(),
                                    color = SolidColor(categoryColor(it.key.id))
                                )
                            ),
                        )
                    }
                    .sortedByDescending { it.values.sumOf { v -> v.value } }
            }
        }

        val appShowingList by remember {
            derivedStateOf {
                appList
                    .filter { if (filteredCategories.isEmpty()) true else it.category.text in filteredCategories }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item(
                span = { GridItemSpan(maxLineSpan) }
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(
                        4.dp,
                        Alignment.CenterHorizontally
                    ),
                ) {
                    categoryList.forEach {
                        FilterChip(
                            onClick = { filteredCategories.toggle(it.key.text) },
                            label = { Text(it.key.text) },
                            leadingIcon = {
                                Box(
                                    Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(categoryColor(it.key.id)),
                                )
                            },
                            selected = it.key.text in filteredCategories,
                        )
                    }
                }
            }

            item(
                span = { GridItemSpan(maxLineSpan) }
            ) {
                RowChart(
                    data = appsList,
                    barProperties = BarProperties(
                        cornerRadius = Bars.Data.Radius.Rectangle(
                            topRight = 6.dp,
                            bottomRight = 6.dp
                        ),
                        spacing = 2.dp,
                        thickness = 20.dp
                    ),
                    labelHelperProperties = LabelHelperProperties(
                        false,
                        textStyle = MaterialTheme.typography.bodyMedium
                            .copy(fontSize = 12.sp)
                            .copy(color = MaterialTheme.colorScheme.onSurface)
                    ),
                    labelProperties = LabelProperties(
                        true,
                        textStyle = MaterialTheme.typography.bodyMedium
                            .copy(fontSize = 12.sp)
                            .copy(color = MaterialTheme.colorScheme.onSurface)
                    ),
                    indicatorProperties = IndicatorProperties(
                        true,
                        textStyle = MaterialTheme.typography.bodyMedium
                            .copy(fontSize = 12.sp)
                            .copy(color = MaterialTheme.colorScheme.onSurface)
                    ) { getDurationBreakdown(it.roundToLong()) },
                    popupProperties = PopupProperties(
                        false,
                        textStyle = MaterialTheme.typography.bodyMedium
                            .copy(fontSize = 12.sp)
                            .copy(color = MaterialTheme.colorScheme.onSurface),
                        contentBuilder = { getDurationBreakdown(it.roundToLong()) }
                    ),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    animationMode = AnimationMode.Together { it * 100L },
                    animationDelay = 300,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height((DEFAULT_ROW_HEIGHT + ((ROW_HEIGHT_400 / 9) * appsList.size)).dp)
                )
            }

            items(appShowingList) { app ->
                OutlinedCard(
                    border = BorderStroke(
                        width = 1.dp,
                        color = categoryColor(app.category.id)
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
}