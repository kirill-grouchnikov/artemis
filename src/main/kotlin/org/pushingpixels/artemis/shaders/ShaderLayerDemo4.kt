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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.launch
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.pushingpixels.aurora.component.model.Command
import org.pushingpixels.aurora.component.model.CommandActionPreview
import org.pushingpixels.aurora.component.model.SliderContentModel
import org.pushingpixels.aurora.component.projection.CommandButtonProjection
import org.pushingpixels.aurora.component.projection.SliderProjection
import org.pushingpixels.aurora.theming.AuroraSkin
import org.pushingpixels.aurora.theming.businessSkin
import org.pushingpixels.aurora.window.AuroraWindow
import org.pushingpixels.aurora.window.auroraApplication

fun main() = auroraApplication {
    val state = rememberWindowState(
        placement = WindowPlacement.Floating,
        position = WindowPosition.Aligned(Alignment.Center),
        size = DpSize(300.dp, 200.dp)
    )

    val skin = mutableStateOf(businessSkin())

    val compositeSksl = """
        uniform shader content;
        uniform shader blurred;

        uniform float width;
        uniform float cutoffPercent;
        uniform float redness;

        vec4 main(vec2 coord) {
            vec4 c = content.eval(coord);
            vec4 b = blurred.eval(coord);
            if (coord.x > width * cutoffPercent) {
                return vec4((c.r + (1.0 - c.r) * redness) * c.a, c.g * c.a, c.b * c.a, c.a);
            } else {
                return b;
            }
        }
        """

    val compositeRuntimeEffect = RuntimeEffect.makeForShader(compositeSksl)

    AuroraWindow(
        skin = skin,
        title = "Filter Demo",
        state = state,
        undecorated = true,
        onCloseRequest = ::exitApplication,
    ) {
        val tweenSpec = tween<Float>(durationMillis = AuroraSkin.animationConfig.regular, easing = FastOutSlowInEasing)
        val hoverAmount = remember { Animatable(0.0f) }

        // The cutoff line between the blurred part and the colorized part is dynamic
        // based on the button width
        val buttonWidth = remember { mutableStateOf(0.0f) }
        val cutoffPercent = remember { mutableStateOf(0.5f) }

        // The amount of "redness" is the inverse of the hover amount (full red
        // when there is no hover, no red on hover)
        val redness = derivedStateOf { 1.0f - hoverAmount.value }

        val compositeShaderBuilder = RuntimeShaderBuilder(compositeRuntimeEffect)
        compositeShaderBuilder.uniform("width", buttonWidth.value)
        compositeShaderBuilder.uniform("cutoffPercent", cutoffPercent.value)
        compositeShaderBuilder.uniform("redness", redness.value)

        // The amount of blur is the inverse of the hover amount (full blur when there
        // is no hover, no blue on hover). Note that hover amount 0.0f cause null pointer
        // to be thrown from Skia, so for now no blur is actually 0.1f blur.
        val blurAmount = derivedStateOf { (1.0f - hoverAmount.value) * 2.0f + 0.1f }
        val blurImageFilter: ImageFilter = ImageFilter.makeBlur(
            sigmaX = blurAmount.value,
            sigmaY = blurAmount.value,
            mode = FilterTileMode.DECAL
        )
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier.fillMaxSize().padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.Center
            ) {
                CommandButtonProjection(
                    contentModel = Command(text = "Click me 2!", action = { println("Clicked!") },
                        actionPreview = object : CommandActionPreview {
                            override fun onCommandPreviewActivated(command: Command) {
                                coroutineScope.launch {
                                    hoverAmount.animateTo(targetValue = 1.0f, animationSpec = tweenSpec)
                                }
                            }

                            override fun onCommandPreviewCanceled(command: Command) {
                                coroutineScope.launch {
                                    hoverAmount.animateTo(targetValue = 0.0f, animationSpec = tweenSpec)
                                }
                            }
                        })
                ).project(
                    modifier = Modifier.graphicsLayer(
                        renderEffect = ImageFilter.makeRuntimeShader(
                            runtimeShaderBuilder = compositeShaderBuilder,
                            shaderNames = arrayOf("content", "blurred"),
                            inputs = arrayOf(null, blurImageFilter)
                        ).asComposeRenderEffect()
                    ).onGloballyPositioned {
                        buttonWidth.value = it.size.width.toFloat()
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            SliderProjection(
                contentModel = SliderContentModel(
                    value = cutoffPercent.value,
                    valueRange = 0.0f.rangeTo(1.0f),
                    onTriggerValueChange = {
                        cutoffPercent.value = it
                    }
                )
            ).project()
        }
    }
}

