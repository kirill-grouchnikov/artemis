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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.pushingpixels.aurora.component.model.Command
import org.pushingpixels.aurora.component.model.CommandActionPreview
import org.pushingpixels.aurora.component.projection.CommandButtonProjection
import org.pushingpixels.aurora.theming.*
import org.pushingpixels.aurora.window.AuroraWindow
import org.pushingpixels.aurora.window.AuroraWindowTitlePaneConfigurations
import org.pushingpixels.aurora.window.auroraApplication

fun main() = auroraApplication {
    val state = rememberWindowState(
        placement = WindowPlacement.Floating,
        position = WindowPosition.Aligned(Alignment.Center),
        size = DpSize(300.dp, 120.dp)
    )

    @Language("GLSL")
    val glowSksl = """
        uniform shader content;
        uniform float width;
        uniform float height;
        uniform float radius;

        uniform float maxGlowAmount;
        uniform float glowCutoffDistance;
        
        uniform vec4 glowColor;

        // Simplified version of SDF (signed distance function) for a rounded box
        // from https://www.iquilezles.org/www/articles/distfunctions2d/distfunctions2d.htm
        float roundedRectangleSDF(vec2 position, vec2 box, float radius) {
            vec2 q = abs(position) - box + vec2(radius);
            return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;   
        }

        vec4 main(vec2 coord) {
            vec2 rectangle = vec2(width, height) / 2.0;
            float distanceToClosestEdge = roundedRectangleSDF(
                coord - rectangle, rectangle, radius);

            vec4 c = content.eval(coord);
            if (distanceToClosestEdge <= 0.0) {
                // We're inside the content
                return c;
            }
            
            // How much glow do we want?
            float distanceFraction = max(1.0 - distanceToClosestEdge / glowCutoffDistance, 0.0);
            // Force quicker dissipation of the glow
            distanceFraction = pow(distanceFraction, 1.8);
            
            float glowAmount = maxGlowAmount * distanceFraction * glowColor.a;
            
            return vec4(glowColor.r * glowAmount, glowColor.g * glowAmount, glowColor.b * glowAmount, glowAmount);
        }
        """

    val glowRuntimeEffect = RuntimeEffect.makeForShader(glowSksl)

    AuroraWindow(
        skin = graphiteGoldSkin(),
        title = "Filter Demo",
        state = state,
        windowTitlePaneConfiguration = AuroraWindowTitlePaneConfigurations.AuroraPlain(),
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
            glowShaderBuilder.uniform(
                "glowColor", rolloverFill.red, rolloverFill.green,
                rolloverFill.blue, rolloverFill.alpha
            )

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
                    ).onSizeChanged {
                        buttonWidth.value = it.width.toFloat()
                        buttonHeight.value = it.height.toFloat()
                    }
                )
            }
        }
    }
}

