/*
 * Copyright (c) 2021-22 Artemis, Kirill Grouchnikov. All Rights Reserved.
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
package org.pushingpixels.artemis.shaders

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.pushingpixels.aurora.component.model.Command
import org.pushingpixels.aurora.component.projection.CommandButtonProjection
import org.pushingpixels.aurora.theming.mistSilverSkin
import org.pushingpixels.aurora.window.AuroraWindow
import org.pushingpixels.aurora.window.auroraApplication

fun main() = auroraApplication {
    val state = rememberWindowState(
        placement = WindowPlacement.Floating,
        position = WindowPosition.Aligned(Alignment.Center),
        size = DpSize(300.dp, 300.dp)
    )

    val skin = mutableStateOf(mistSilverSkin())

    val redSksl = """
        uniform shader content;
        vec4 main(vec2 coord) {
            vec4 c = content.eval(coord);
            return vec4(1.0 * c.a, c.g * c.a, c.b * c.a, c.a);
        }
        """

    val redRuntimeEffect = RuntimeEffect.makeForShader(redSksl)
    val redShaderBuilder = RuntimeShaderBuilder(redRuntimeEffect)

    val compositeSksl = """
        uniform shader content;
        uniform shader blurred;
        uniform float cutoff;
        vec4 main(vec2 coord) {
            vec4 c = content.eval(coord);
            vec4 b = blurred.eval(coord);
            if (coord.x > cutoff) {
                return vec4(1.0 * c.a, c.g * c.a, c.b * c.a, c.a);
            } else {
                return b;
            }
        }
        """

    val compositeRuntimeEffect = RuntimeEffect.makeForShader(compositeSksl)
    val compositeShaderBuilder = RuntimeShaderBuilder(compositeRuntimeEffect)
    compositeShaderBuilder.uniform("cutoff", 100.0f)
    val blurImageFilter = ImageFilter.makeBlur(sigmaX = 2.0f, sigmaY = 2.0f, mode = FilterTileMode.DECAL)

    AuroraWindow(
        skin = skin,
        title = "Filter Demo",
        state = state,
        undecorated = true,
        onCloseRequest = ::exitApplication,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CommandButtonProjection(
                contentModel = Command(text = "Top button!", action = { println("Clicked!") })
            ).project(modifier = Modifier.align(Alignment.TopCenter))

            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.Center
            ) {
                CommandButtonProjection(
                    contentModel = Command(text = "Click me!", action = { println("Clicked!") })
                ).project(
                    modifier = Modifier.graphicsLayer(
                        renderEffect = ImageFilter.makeRuntimeShader(
                            runtimeShaderBuilder = redShaderBuilder,
                            shaderName = "content",
                            input = null
                        ).asComposeRenderEffect(),
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                CommandButtonProjection(
                    contentModel = Command(text = "Click me 2!", action = { println("Clicked!") })
                ).project(
                    modifier = Modifier.graphicsLayer(
                        renderEffect = ImageFilter.makeRuntimeShader(
                            runtimeShaderBuilder = compositeShaderBuilder,
                            shaderNames = arrayOf("content", "blurred"),
                            inputs = arrayOf(null, blurImageFilter)
                        ).asComposeRenderEffect(),
                    )
                )
            }

            CommandButtonProjection(
                contentModel = Command(text = "Bottom button!", action = { println("Clicked!") })
            ).project(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

