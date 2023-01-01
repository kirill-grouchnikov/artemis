/*
 * Copyright (c) 2021-23 Artemis, Kirill Grouchnikov. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pushingpixels.artemis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import org.intellij.lang.annotations.Language
import org.jetbrains.skia.*
import org.jetbrains.skia.Shader

fun main() = application {
    val state = rememberWindowState(
        placement = WindowPlacement.Floating,
        position = WindowPosition.Aligned(Alignment.Center),
        size = DpSize(510.dp, 370.dp)
    )

    Window(
        title = "Compose / Skia shader demo",
        state = state,
        undecorated = true,
        onCloseRequest = ::exitApplication,
    ) {
        Box(modifier = Modifier.fillMaxSize(1.0f).background(Color(0xFF03080D))) {
            // Recreate visuals from https://uxmisfit.com/2021/01/13/how-to-create-glassmorphic-card-ui-design/
            @Language("GLSL")
            val compositeSksl = """
                uniform shader content;
                uniform shader blur;
                uniform shader noise;
                
                uniform vec4 rectangle;
                uniform float radius;
                
                uniform float dropShadowSize;
                
                // Simplified version of SDF (signed distance function) for a rounded box
                // from https://www.iquilezles.org/www/articles/distfunctions2d/distfunctions2d.htm
                float roundedRectangleSDF(vec2 position, vec2 box, float radius) {
                    vec2 q = abs(position) - box + vec2(radius);
                    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;   
                }
                
                vec4 main(vec2 coord) {
                    vec2 shiftRect = (rectangle.zw - rectangle.xy) / 2.0;
                    vec2 shiftCoord = coord - rectangle.xy;
                    float distanceToClosestEdge = roundedRectangleSDF(
                        shiftCoord - shiftRect, shiftRect, radius);
        
                    vec4 c = content.eval(coord);
                    if (distanceToClosestEdge > 0.0) {
                        // We're outside of the filtered area
                        if (distanceToClosestEdge < dropShadowSize) {
                            // Emulate drop shadow around the filtered area
                            float darkenFactor = (dropShadowSize - distanceToClosestEdge) / dropShadowSize;
                            // Use exponential drop shadow decay for more pleasant visuals
                            darkenFactor = pow(darkenFactor, 1.6);
                            // Shift towards black, by 10% around the edge, dissipating to 0% further away
                            return c * (0.9 + (1.0 - darkenFactor) / 10.0);
                        }
                        return c;
                    }
                    
                    vec4 b = blur.eval(coord);
                    vec4 n = noise.eval(coord);
                    // How far are we from the top-left corner?
                    float lightenFactor = min(1.0, length(coord - rectangle.xy) / (0.85 * length(rectangle.zw - rectangle.xy)));
                    // Add some noise for extra texture
                    float noiseLuminance = dot(n.rgb, vec3(0.2126, 0.7152, 0.0722));
                    lightenFactor = min(1.0, lightenFactor + noiseLuminance);
                    // Shift towards white, by 35% in top left corner, down to 10% in bottom right corner
                    return b + (vec4(1.0) - b) * (0.35 - 0.25 * lightenFactor);
                }
            """

            val compositeRuntimeEffect = RuntimeEffect.makeForShader(compositeSksl)
            val compositeShaderBuilder = RuntimeShaderBuilder(compositeRuntimeEffect)

            val density = LocalDensity.current.density
            compositeShaderBuilder.uniform(
                "rectangle",
                85.0f * density, 110.0f * density, 405.0f * density, 290.0f * density
            )
            compositeShaderBuilder.uniform("radius", 20.0f * density)
            compositeShaderBuilder.child(
                "noise", Shader.makeFractalNoise(
                    baseFrequencyX = 0.45f,
                    baseFrequencyY = 0.45f,
                    numOctaves = 4,
                    seed = 2.0f
                )
            )
            compositeShaderBuilder.uniform("dropShadowSize", 15.0f * density)

            Canvas(
                modifier = Modifier.fillMaxSize(1.0f)
                    .graphicsLayer(
                        renderEffect = ImageFilter.makeRuntimeShader(
                            runtimeShaderBuilder = compositeShaderBuilder,
                            shaderNames = arrayOf("content", "blur"),
                            inputs = arrayOf(
                                null, ImageFilter.makeBlur(
                                    sigmaX = 20.0f,
                                    sigmaY = 20.0f,
                                    mode = FilterTileMode.DECAL
                                )
                            )
                        ).asComposeRenderEffect(),
                    )
            ) {
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF7A26D9), Color(0xFFE444E1)),
                        start = Offset(450.dp.toPx(), 60.dp.toPx()),
                        end = Offset(290.dp.toPx(), 190.dp.toPx()),
                        tileMode = TileMode.Clamp
                    ),
                    center = Offset(375.dp.toPx(), 125.dp.toPx()),
                    radius = 100.dp.toPx()
                )
                drawCircle(
                    color = Color(0xFFEA357C),
                    center = Offset(100.dp.toPx(), 265.dp.toPx()),
                    radius = 55.dp.toPx()
                )
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFEA334C), Color(0xFFEC6051)),
                        start = Offset(180.dp.toPx(), 125.dp.toPx()),
                        end = Offset(230.dp.toPx(), 125.dp.toPx()),
                        tileMode = TileMode.Clamp
                    ),
                    center = Offset(205.dp.toPx(), 125.dp.toPx()),
                    radius = 25.dp.toPx()
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize(1.0f)) {
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0x80FFFFFF), Color(0x00FFFFFF), Color(0x00FF48DB), Color(0x80FF48DB)),
                    start = Offset(120.dp.toPx(), 110.dp.toPx()),
                    end = Offset(405.dp.toPx(), 290.dp.toPx()),
                    tileMode = TileMode.Clamp
                ),
                topLeft = Offset(86.dp.toPx(), 111.dp.toPx()),
                size = Size(318.dp.toPx(), 178.dp.toPx()),
                cornerRadius = CornerRadius(20.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )

            drawTextOnPath(
                text = "MEMBERSHIP",
                textSize = 14.dp,
                isEmboldened = true,
                path = Path().also { path ->
                    path.moveTo(100.dp.toPx(), 140.dp.toPx())
                    path.lineTo(400.dp.toPx(), 140.dp.toPx())
                },
                offset = Offset(2.dp.toPx(), 0.0f),
                textAlign = TextAlign.Left,
                paint = Paint().also {
                    it.color = Color(0x80FFFFFF)
                    it.style = PaintingStyle.Fill
                },
            )

            drawTextOnPath(
                text = "JAMES APPLESEED",
                textSize = 18.dp,
                isEmboldened = true,
                path = Path().also { path ->
                    path.moveTo(100.dp.toPx(), 240.dp.toPx())
                    path.lineTo(400.dp.toPx(), 240.dp.toPx())
                },
                offset = Offset(2.dp.toPx(), 0.0f),
                textAlign = TextAlign.Left,
                paint = Paint().also {
                    it.color = Color(0x80FFFFFF)
                    it.style = PaintingStyle.Fill
                },
            )

            drawTextOnPath(
                text = "PUSHING-PIXELS",
                textSize = 13.dp,
                isEmboldened = true,
                path = Path().also { path ->
                    path.moveTo(100.dp.toPx(), 265.dp.toPx())
                    path.lineTo(400.dp.toPx(), 265.dp.toPx())
                },
                offset = Offset(2.dp.toPx(), 0.0f),
                textAlign = TextAlign.Left,
                paint = Paint().also {
                    it.color = Color(0x80FFFFFF)
                    it.style = PaintingStyle.Fill
                },
            )
        }
    }
}

