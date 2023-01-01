/*
 * Copyright (c) 2021-23 Artemis, Kirill Grouchnikov. All Rights Reserved.
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

package org.pushingpixels.artemis.samples

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import org.pushingpixels.artemis.drawTextOnPath

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication, title = "Drawing text on a path",
        state = rememberWindowState(
            placement = WindowPlacement.Floating,
            position = WindowPosition.Aligned(Alignment.Center),
            size = DpSize(500.dp, 400.dp)
        )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val lightYellow = Color(255, 254, 240)
            val darkGrey = Color(10, 10, 18)

            drawRect(color = lightYellow)

            // A path with three quad Bezier segments
            val path = androidx.compose.ui.graphics.Path()
            path.moveTo(30.dp.toPx(), 30.dp.toPx())
            path.quadraticBezierTo(80.dp.toPx(), 40.dp.toPx(), 95.dp.toPx(), 100.dp.toPx())
            path.quadraticBezierTo(110.dp.toPx(), 170.dp.toPx(), 160.dp.toPx(), 170.dp.toPx())
            path.quadraticBezierTo(220.dp.toPx(), 175.dp.toPx(), 225.dp.toPx(), 70.dp.toPx())

            drawPath(path = path, color = Color.Red, style = Stroke())
            drawTextOnPath(
                text = "abcdefghijklmnopqrstuvwxyz0123456789",
                textSize = 14.dp,
                path = path,
                offset = Offset(10.dp.toPx(), 0.0f),
                textAlign = TextAlign.Start,
                paint = Paint().also {
                    it.color = Color.Blue
                    it.style = PaintingStyle.Fill
                }
            )

            // A circular arc
            val path2 = androidx.compose.ui.graphics.Path()
            path2.moveTo(250.dp.toPx(), 50.dp.toPx())
            path2.arcTo(
                rect = Rect(center = Offset(250.dp.toPx(), 250.dp.toPx()), radius = 200.dp.toPx()),
                startAngleDegrees = -90.0f, sweepAngleDegrees = 90.0f, forceMoveTo = false
            )
            drawPath(path = path2, color = darkGrey, style = Stroke())
            drawTextOnPath(
                text = "abcdefghijklmnopqrstuvwxyz0123456789",
                textSize = 14.dp,
                path = path2,
                offset = Offset(0.0f, -5.dp.toPx()),
                paint = Paint().also {
                    it.color = lightYellow
                    it.style = PaintingStyle.Fill
                },
                shadow = Shadow(
                    color = darkGrey,
                    offset = Offset(x = 0.0f, y = 0.0f),
                    blurRadius = 2.5f
                )
            )

            // Same arc, but "going" the other way
            val path3 = androidx.compose.ui.graphics.Path()
            path3.moveTo(450.dp.toPx(), 250.dp.toPx())
            path3.arcTo(
                rect = Rect(center = Offset(250.dp.toPx(), 250.dp.toPx()), radius = 200.dp.toPx()),
                startAngleDegrees = 0.0f, sweepAngleDegrees = -90.0f, forceMoveTo = false
            )
            drawTextOnPath(
                text = "abcdefghijklmnopqrstuvwxyz0123456789",
                textSize = 14.dp,
                path = path3,
                offset = Offset(0.0f, -5.dp.toPx()),
                paint = Paint().also {
                    it.color = lightYellow
                    it.style = PaintingStyle.Fill
                },
                shadow = Shadow(
                    color = darkGrey,
                    offset = Offset(x = 0.0f, y = 0.0f),
                    blurRadius = 2.5f
                )
            )

            // One more path
            val path4 = androidx.compose.ui.graphics.Path()
            path4.moveTo(94.dp.toPx(), 335.dp.toPx())
            path4.lineTo(125.dp.toPx(), 275.dp.toPx())
            path4.quadraticBezierTo(158.dp.toPx(), 202.dp.toPx(), 300.dp.toPx(), 200.dp.toPx())
            path4.lineTo(400.dp.toPx(), 200.dp.toPx())

            drawPath(path = path4, color = darkGrey, style = Stroke())
            drawTextOnPath(
                text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
                textSize = 14.dp,
                path = path4,
                offset = Offset(0.0f, -5.dp.toPx()),
                paint = Paint().also {
                    it.color = Color(0, 90, 90)
                    it.style = PaintingStyle.Fill
                }
            )
        }
    }
}
