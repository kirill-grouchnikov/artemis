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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.launch
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.pushingpixels.aurora.component.model.Command
import org.pushingpixels.aurora.component.model.CommandActionPreview
import org.pushingpixels.aurora.component.projection.CommandButtonProjection
import org.pushingpixels.aurora.theming.AuroraSkin
import org.pushingpixels.aurora.theming.ColorSchemeAssociationKind
import org.pushingpixels.aurora.theming.ComponentState
import org.pushingpixels.aurora.theming.graphiteGoldSkin
import org.pushingpixels.aurora.window.AuroraWindow
import org.pushingpixels.aurora.window.auroraApplication

fun main() = auroraApplication {
    val state = rememberWindowState(
        placement = WindowPlacement.Floating,
        position = WindowPosition.Aligned(Alignment.Center),
        size = DpSize(300.dp, 120.dp)
    )

    val skin = mutableStateOf(graphiteGoldSkin())

    val glowSksl = """
        uniform shader content;
        uniform float width;
        uniform float height;
        uniform float radius;

        uniform float maxGlowAmount;
        uniform float glowCutoffDistance;
        
        uniform float glowRed;
        uniform float glowGreen;
        uniform float glowBlue;

        vec4 main(vec2 coord) {
            vec4 c = content.eval(coord);
            if (c.a > 0.0) {
                // We're inside the content
                return c;
            }
            
            float distanceToClosestEdge = 0.0;
            
            if ((coord.x < radius) && (coord.y < radius)) {
                // Top-left corner
                float dx = radius - coord.x;
                float dy = radius - coord.y;
                distanceToClosestEdge = sqrt(dx*dx + dy*dy) - radius;
            } else if ((coord.x > (width - radius)) && (coord.y < radius)) {
                // Top-right corner
                float dx = coord.x - (width - radius);
                float dy = radius - coord.y;
                distanceToClosestEdge = sqrt(dx*dx + dy*dy) - radius;
            } else if ((coord.x > (width - radius)) && (coord.y > (height - radius))) {
                // Bottom-right corner
                float dx = coord.x - (width - radius);
                float dy = coord.y - (height - radius);
                distanceToClosestEdge = sqrt(dx*dx + dy*dy) - radius;
            } else if ((coord.x < radius) && (coord.y > (height - radius))) {
                // Bottom-left corner
                float dx = radius - coord.x;
                float dy = coord.y - (height - radius);
                distanceToClosestEdge = sqrt(dx*dx + dy*dy) - radius;
            } else if (coord.x < 0.0) {
                // Left edge
                distanceToClosestEdge = -coord.x;
            } else if (coord.y < 0.0) {
                // Top edge
                distanceToClosestEdge = -coord.y;
            } else if (coord.x > width) {
                // Right edge
                distanceToClosestEdge = coord.x - width;
            } else if (coord.y > height) {
                // Bottom edge
                distanceToClosestEdge = coord.y - height;
            }
            
            // How much glow do we want?
            float distanceFraction = max(1.0 - distanceToClosestEdge / glowCutoffDistance, 0.0);
            // Force quicker dissipation of the glow
            distanceFraction = pow(distanceFraction, 1.8);
            
            float glowAmount = maxGlowAmount * distanceFraction;
            
            return vec4(glowRed * glowAmount, glowGreen * glowAmount, glowBlue * glowAmount, glowAmount);
        }
        """

    val glowRuntimeEffect = RuntimeEffect.makeForShader(glowSksl)

    AuroraWindow(
        skin = skin,
        title = "Filter Demo",
        state = state,
        undecorated = true,
        onCloseRequest = ::exitApplication,
    ) {
        val tweenSpec = tween<Float>(durationMillis = AuroraSkin.animationConfig.regular, easing = FastOutSlowInEasing)

        val hoverAmount = remember { Animatable(0.0f) }
        val buttonWidth = remember { mutableStateOf(0.0f) }
        val buttonHeight = remember { mutableStateOf(0.0f) }

        val glowShaderBuilder = RuntimeShaderBuilder(glowRuntimeEffect)
        glowShaderBuilder.uniform("width", buttonWidth.value)
        glowShaderBuilder.uniform("height", buttonHeight.value)
        glowShaderBuilder.uniform("radius", 3.0f * LocalDensity.current.density)

        // The amount of "glow" is derived from the hover amount
        val glow = derivedStateOf { 0.8f * hoverAmount.value }
        glowShaderBuilder.uniform("maxGlowAmount", glow.value)

        glowShaderBuilder.uniform("glowCutoffDistance", 20.0f)

        val coroutineScope = rememberCoroutineScope()

        Box(modifier = Modifier.fillMaxSize()) {
            val decorationAreaType = AuroraSkin.decorationAreaType
            val rolloverFill = AuroraSkin.colors.getColorScheme(
                decorationAreaType = decorationAreaType,
                associationKind = ColorSchemeAssociationKind.Fill,
                componentState = ComponentState.RolloverUnselected
            ).backgroundFillColor
            glowShaderBuilder.uniform("glowRed", rolloverFill.red)
            glowShaderBuilder.uniform("glowGreen", rolloverFill.green)
            glowShaderBuilder.uniform("glowBlue", rolloverFill.blue)

            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.Center
            ) {
                CommandButtonProjection(
                    contentModel = Command(text = "Regular", action = { println("Clicked!") })
                ).project()

                Spacer(Modifier.width(40.dp))

                CommandButtonProjection(
                    contentModel = Command(text = "With glow", action = { println("Clicked!") },
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
                            runtimeShaderBuilder = glowShaderBuilder,
                            shaderName = "content",
                            input = null
                        ).asComposeRenderEffect(),
                        clip = true,
                        shape = object : Shape {
                            override fun createOutline(
                                size: Size,
                                layoutDirection: LayoutDirection,
                                density: Density
                            ): Outline {
                                return Outline.Rectangle(
                                    rect = Rect(
                                        left = -20.0f,
                                        top = -20.0f,
                                        right = size.width + 20.0f,
                                        bottom = size.height + 20.0f
                                    )
                                )
                            }
                        }
                    ).onGloballyPositioned {
                        buttonWidth.value = it.size.width.toFloat()
                        buttonHeight.value = it.size.height.toFloat()
                    }
                )
            }
        }
    }
}

