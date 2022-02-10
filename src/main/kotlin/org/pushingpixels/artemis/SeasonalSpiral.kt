/*
 * Copyright (c) 2022 Artemis, Kirill Grouchnikov. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  o Neither the name of the copyright holder nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.pushingpixels.artemis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okio.Okio
import org.jetbrains.skia.Color4f
import org.jetbrains.skia.Font
import org.jetbrains.skia.Typeface
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin

private interface WikipediaService {
    @GET("/api/rest_v1/metrics/pageviews/per-article/en.wikipedia/all-access/all-agents/{query}/daily/{from}/{to}")
    fun getDailyStats(
        @Path("query") query: String,
        @Path("from") from: String,
        @Path("to") to: String,
    ): Call<Stats>

    companion object {
        const val API_URL = "https://wikimedia.org/"
    }
}

data class Stats(
    @field:Json(name = "items") val items: List<QueryItem>
)

data class QueryItem(
    @field:Json(name = "project") val project: String,
    @field:Json(name = "article") val article: String,
    @field:Json(name = "granularity") var granularity: String,
    @field:Json(name = "timestamp") val timestamp: String,
    @field:Json(name = "access") val access: String,
    @field:Json(name = "agent") val agent: String,
    @field:Json(name = "views") val views: Int
)

fun Float.toRad() = this * Math.PI.toFloat() / 180.0f

class Gradients(vararg val colorStops: Pair<Float, Color>) {
    fun getColor(fraction: Float): Color {
        for (index in 0 until colorStops.size - 1) {
            if ((fraction >= colorStops[index].first) && (fraction <= colorStops[index + 1].first)) {
                val start = colorStops[index].second
                val end = colorStops[index + 1].second
                return lerp(
                    start, end, (fraction - colorStops[index].first) /
                            (colorStops[index + 1].first - colorStops[index].first)
                )
            }
        }
        throw IllegalArgumentException("Out of bounds")
    }

    fun getMidPoint(start: Int, end: Int): Int {
        val stops = colorStops.size
        return if (stops % 2 == 1) {
            val fraction = colorStops[stops / 2].first
            (start + (end - start) * fraction).toInt()
        } else {
            val fractionStart = colorStops[stops / 2 - 1].first
            val fractionEnd = colorStops[stops / 2].first
            (start + (end - start) * (fractionEnd + fractionStart) / 2.0f).toInt()
        }
    }
}


data class DataSet(
    val name: String, val localSource: String,
    val remoteQuery: String, val gradients: Gradients
)

val colorLow = Color(254, 254, 230)
val colorMid1 = Color(247, 206, 167)
val colorMid2 = Color(183, 64, 138)
val colorHigh = Color(67, 20, 119)

val MRNA = DataSet(
    name = "MRNA",
    localSource = "/mrna.json",
    remoteQuery = "Messenger_RNA",
    gradients = Gradients(
        0.0f to colorLow,
        0.1f to colorMid1,
        0.5f to colorMid2,
        1.0f to colorHigh,
    )
)
val SOURDOUGH = DataSet(
    name = "Sourdough",
    localSource = "/sourdough.json",
    remoteQuery = "Sourdough",
    gradients = Gradients(
        0.0f to colorLow,
        0.05f to colorMid1,
        0.2f to colorMid2,
        1.0f to colorHigh,
    )
)
val REBECCA_BLACK = DataSet(
    name = "Rebecca Black",
    localSource = "/rebecca_black.json",
    remoteQuery = "Rebecca_Black",
    gradients = Gradients(
        0.0f to colorLow,
        0.03f to colorMid1,
        0.08f to colorMid2,
        1.0f to colorHigh,
    )
)
val PRIME_NUMBER = DataSet(
    name = "Prime Number",
    localSource = "/prime_number.json",
    remoteQuery = "Prime_number",
    gradients = Gradients(
        0.0f to colorLow,
        0.03f to colorMid1,
        0.08f to colorMid2,
        1.0f to colorHigh,
    )
)

// To run this, change the dataSet to point to one of the DataSet vals.
// Setting useLocal to true will require a local copy of the JSON response to the matching query:
// 1. Open https://wikimedia.org/api/rest_v1/metrics/pageviews/per-article/en.wikipedia/all-access/all-agents/Prime_numbers/daily/2017010100/2022010100
//    in the browser
// 2. Save response as raw data
// 3. Put it as the matching *.json file under the /resources folder
fun main() = application {
    val dataSet = SOURDOUGH
    val useLocal = false

    val moshi: Moshi = Moshi.Builder().build()
    val jsonAdapter: JsonAdapter<Stats> = moshi.adapter(Stats::class.java)

    val dailyStats = if (useLocal) {
        println("Loading local data")
        val input = WikipediaService::class.java.getResourceAsStream(dataSet.localSource)
        val source = Okio.buffer(Okio.source(input))
        jsonAdapter.fromJson(source)
    } else {
        println("Fetching remote data from Wikimedia")
        // Note that this really should be running on a worker thread and then updating the UI
        // when the data arrives (LaunchEffect or something similar). That, however, is not the
        // main point of this demo, so we're doing a blocking call here and wait for the data
        // to load.
        val retrofit = Retrofit.Builder()
            .baseUrl(WikipediaService.API_URL)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        val service = retrofit.create(WikipediaService::class.java)

        val dailyStatsCall = service.getDailyStats(
            query = dataSet.remoteQuery,
            from = "2017010100",
            to = "2022010100"
        )
        val dailyStatsResponse = dailyStatsCall.execute()
        dailyStatsResponse.body()
    }

    val mapped = dailyStats!!.items.associate { it.timestamp to it.views }
    val lowestHitCount = mapped.values.minOf { it }
    val highestHitCount = mapped.values.maxOf { it }

    Window(
        onCloseRequest = ::exitApplication, title = "Artemis ${dataSet.name}",
        state = rememberWindowState(
            placement = WindowPlacement.Floating,
            position = WindowPosition.Aligned(Alignment.Center),
            size = DpSize(500.dp, 500.dp)
        )
    ) {

        Canvas(Modifier.fillMaxSize()) {
            // The visuals of the spiral are from
            // https://observablehq.com/@yurivish/seasonal-spirals
            val fillColor = Color(255, 254, 240)
            val shadowColor = Color(10, 10, 18)

            drawRect(color = fillColor)

            val gap = 4.dp.toPx()
            val centerX = size.center.x
            val centerY = size.center.y
            var radiusStart = 40.dp.toPx()
            var thicknessStart = 20.dp.toPx()
            val thicknessIncrement = 5.dp.toPx()

            var date = LocalDate.of(2018, 1, 1)
            for (year in 2018 until 2022) {
                val thicknessEnd = thicknessStart + thicknessIncrement

                val radiusInnerStart = radiusStart
                val radiusInnerEnd = radiusInnerStart + thicknessStart + gap
                val radiusOuterStart = radiusStart + thicknessStart
                val radiusOuterEnd = radiusInnerEnd + thicknessEnd


                val segments = 52
                for (segment in 0 until segments) {
                    val startAngle = segment * 360.0f / segments
                    val endAngle = startAngle + 360.0f / segments

                    val radiusA =
                        radiusInnerStart + (radiusInnerEnd - radiusInnerStart) * startAngle / 360.0f
                    val radiusB =
                        radiusOuterStart + (radiusOuterEnd - radiusOuterStart) * startAngle / 360.0f
                    val radiusC =
                        radiusOuterStart + (radiusOuterEnd - radiusOuterStart) * endAngle / 360.0f
                    val radiusD =
                        radiusInnerStart + (radiusInnerEnd - radiusInnerStart) * endAngle / 360.0f

                    for (weekday in 0 until 7) {
                        val radiusDayA = radiusA + (radiusB - radiusA) * weekday / 7.0f
                        val radiusDayB = radiusDayA + (radiusB - radiusA) / 7.0f
                        val radiusDayD = radiusD + (radiusC - radiusD) * weekday / 7.0f
                        val radiusDayC = radiusDayD + (radiusC - radiusD) / 7.0f

                        val A = Offset(
                            x = centerX + radiusDayA * sin(startAngle.toRad()),
                            y = centerY - radiusDayA * cos(startAngle.toRad())
                        )
                        val B = Offset(
                            x = centerX + radiusDayB * sin(startAngle.toRad()),
                            y = centerY - radiusDayB * cos(startAngle.toRad())
                        )
                        val C = Offset(
                            x = centerX + radiusDayC * sin(endAngle.toRad()),
                            y = centerY - radiusDayC * cos(endAngle.toRad())
                        )
                        val D = Offset(
                            x = centerX + radiusDayD * sin(endAngle.toRad()),
                            y = centerY - radiusDayD * cos(endAngle.toRad())
                        )

                        val path = androidx.compose.ui.graphics.Path()
                        path.moveTo(A.x, A.y)
                        path.lineTo(B.x, B.y)
                        path.lineTo(C.x, C.y)
                        path.lineTo(D.x, D.y)
                        path.close()

                        // Convert the current date to timestamp that matches our source data
                        val dateKey = "${date.year}${
                            date.monthValue.toString().padStart(2, '0')
                        }${date.dayOfMonth.toString().padStart(2, '0')}00"
                        val hits = mapped[dateKey]!!.toFloat()

                        // Compute where the number of hits that corresponds to the current date
                        // falls within the min-max range of the whole data set
                        val percentage =
                            (hits - lowestHitCount) / (highestHitCount - lowestHitCount)

                        // And get the matching color for filling the chart segment that corresponds
                        // to the current date
                        val color = dataSet.gradients.getColor(percentage)

                        drawPath(
                            path = path,
                            color = color,
                            style = Fill
                        )

                        // Go to the next day. Note that 52 weeks is 364 days, so technically
                        // speaking, we're accumulating one-day off every year. But since we're
                        // only displaying data for 3-4 years in this graph, the total
                        // accumulated deviation doesn't really matter
                        date = date.plusDays(1)
                    }
                }

                val yearString = year.toString()
                val radiusLetter = (radiusInnerStart + radiusOuterStart) / 2.0f - 4.dp.toPx()

                val yearPath = Path()
                yearPath.addArc(
                    oval = Rect(
                        left = centerX - radiusLetter * 1.2f,
                        top = centerY - radiusLetter,
                        right = centerX + radiusLetter * 1.2f,
                        bottom = centerY + radiusLetter
                    ),
                    startAngleDegrees = -90.0f,
                    sweepAngleDegrees = 90.0f
                )
                this.drawTextOnPath(
                    text = yearString,
                    textSize = 12.dp,
                    path = yearPath,
                    offset = Offset(2.dp.toPx(), 0.0f),
                    textAlign = TextAlign.Left,
                    paint = Paint().also {
                        it.color = fillColor
                        it.style = PaintingStyle.Fill
                    },
                    shadow = Shadow(
                        color = shadowColor,
                        offset = Offset.Zero,
                        blurRadius = 2.5f
                    )
                )

                thicknessStart = thicknessEnd
                radiusStart = radiusInnerEnd
            }

            // Color legend
            val colorLegendHeight = 80.dp.toPx()
            val colorLegendWidth = 12.dp.toPx()

            var colorLegendBottom = size.height - 40.dp.toPx()
            val colorLegendLeft = 20.dp.toPx()
            val spanHeight = colorLegendHeight / (dataSet.gradients.colorStops.size - 1)

            for (index in 0 until dataSet.gradients.colorStops.size - 1) {
                val spanBottom = dataSet.gradients.colorStops[index]
                val spanTop = dataSet.gradients.colorStops[index + 1]

                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to spanTop.second, 1.0f to spanBottom.second,
                        startY = colorLegendBottom - spanHeight, endY = colorLegendBottom
                    ),
                    topLeft = Offset(x = colorLegendLeft, y = colorLegendBottom - spanHeight),
                    size = Size(width = colorLegendWidth, height = spanHeight),
                    style = Fill
                )

                colorLegendBottom -= (spanHeight - 1)
            }

            this.drawIntoCanvas {
                val nativeCanvas = it.nativeCanvas

                val textPaint = org.jetbrains.skia.Paint()
                textPaint.color4f = Color4f(0.2f, 0.2f, 0.2f, 1.0f)
                val font = Font(Typeface.makeDefault(), 24.0f)

                val legendTextX = colorLegendLeft + colorLegendWidth + 4.dp.toPx()
                nativeCanvas.drawString(
                    (lowestHitCount / 1000).toString() + "K",
                    legendTextX,
                    size.height - 36.dp.toPx(),
                    font,
                    textPaint
                )
                nativeCanvas.drawString(
                    (dataSet.gradients.getMidPoint(
                        lowestHitCount,
                        highestHitCount
                    ) / 1000).toString() + "K",
                    legendTextX,
                    size.height - 36.dp.toPx() - colorLegendHeight / 2.0f,
                    font,
                    textPaint
                )
                nativeCanvas.drawString(
                    (highestHitCount / 1000).toString() + "K",
                    legendTextX,
                    size.height - 36.dp.toPx() - colorLegendHeight,
                    font,
                    textPaint
                )
            }

            // Months
            val months = listOf(
                "jan",
                "feb",
                "mar",
                "apr",
                "may",
                "jun",
                "jul",
                "aug",
                "sep",
                "oct",
                "nov",
                "dec"
            )

            for ((index, month) in months.withIndex()) {
                val monthName = month.uppercase()
                val radius = radiusStart + thicknessStart * index / 12.0f + 6.dp.toPx()
                val monthPath = Path()
                monthPath.addArc(
                    oval = Rect(center = Offset(x = centerX, y = centerY), radius = radius),
                    startAngleDegrees = 30.0f * index - 90.0f,
                    sweepAngleDegrees = 30.0f
                )
                this.drawTextOnPath(
                    text = monthName,
                    textSize = 12.dp,
                    path = monthPath,
                    offset = Offset.Zero,
                    textAlign = TextAlign.Center,
                    paint = Paint().also {
                        it.color = fillColor
                        it.style = PaintingStyle.Fill
                    },
                    shadow = Shadow(
                        color = shadowColor,
                        offset = Offset.Zero,
                        blurRadius = 2.5f
                    )
                )
            }
        }
    }
}
