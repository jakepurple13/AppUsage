package com.programmersbox.appusage

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

private const val DEFAULT_ROW_HEIGHT = 100
private const val ROW_HEIGHT_400 = 400 - DEFAULT_ROW_HEIGHT

@Composable
fun Chart(
    appList: List<AppInfo>,
    modifier: Modifier = Modifier
) {
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