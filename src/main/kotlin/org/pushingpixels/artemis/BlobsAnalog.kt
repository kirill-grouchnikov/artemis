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

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import org.intellij.lang.annotations.Language
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

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
        @Language("GLSL")
        val compositeSksl = """
                uniform shader content;
                uniform shader blur;

                float colorDodge(float dst, float src) {
                    return (src == 1.0) ? src : min(dst/(1.0 - src), 1.0);
                }
                
                vec3 colorDodge(vec3 dst, vec3 src) {
                    return vec3(colorDodge(dst.r, src.r), colorDodge(dst.g, src.g), colorDodge(dst.b, src.b));
                }
                
                float colorBurn(float dst, float src) {
                    return (src == 0.0) ? src : max((1.0 - ((1.0 - dst)/src)), 0.0);
                }
                
                vec3 colorBurn(vec3 dst, vec3 src) {
                    return vec3(colorBurn(dst.r, src.r), colorBurn(dst.g, src.g), colorBurn(dst.b, src.b));
                }

                vec4 main(vec2 coord) {
                    vec4 b = blur.eval(coord);
                    vec3 dodge = colorDodge(vec3(0.5, 0.5, 0.5), b.rgb);
                    vec3 burn = colorBurn(b.rgb, dodge);
                    return vec4(burn.x, burn.y, burn.z, 1.0);
                }
            """

        val compositeRuntimeEffect = RuntimeEffect.makeForShader(compositeSksl)
        val compositeShaderBuilder = RuntimeShaderBuilder(compositeRuntimeEffect)

        val infiniteTransition = rememberInfiniteTransition()
        val x2 by infiniteTransition.animateFloat(
            initialValue = 100.0f,
            targetValue = 230.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        val r1 by infiniteTransition.animateFloat(
            initialValue = 100.0f,
            targetValue = 120.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )

        Box(modifier = Modifier.fillMaxSize(1.0f).background(Color(0xFFFFFFFF))) {
            Canvas(
                modifier = Modifier.fillMaxSize(1.0f).graphicsLayer(
                    renderEffect = ImageFilter.makeRuntimeShader(
                        runtimeShaderBuilder = compositeShaderBuilder,
                        shaderNames = arrayOf("content", "blur"),
                        inputs = arrayOf(
                            null, ImageFilter.makeBlur(
                                sigmaX = 40.0f,
                                sigmaY = 40.0f,
                                mode = FilterTileMode.MIRROR
                            )
                        )
                    ).asComposeRenderEffect(),
                )
            ) {
                drawRect(Color.White)
                drawCircle(
                    color = Color.Black,
                    center = Offset(355.dp.toPx(), 165.dp.toPx()),
                    radius = r1.dp.toPx()
                )
                drawCircle(
                    color = Color.Black,
                    center = Offset(x2.dp.toPx(), 220.dp.toPx()),
                    radius = 60.dp.toPx()
                )
            }
        }
    }
}

