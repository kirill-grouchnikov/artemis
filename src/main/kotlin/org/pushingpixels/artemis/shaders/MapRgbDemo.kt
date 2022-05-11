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
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import java.awt.Point
import java.awt.event.MouseEvent

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Map RGB Displacement",
        state = rememberWindowState(
            placement = WindowPlacement.Floating,
            position = WindowPosition.Aligned(Alignment.Center),
            size = DpSize(670.dp, 470.dp)
        )
    ) {
        val mapSize = remember { mutableStateOf(IntSize(0, 0)) }
        val intensityAmount = remember { Animatable(0.0f) }
        val clickPoint = remember { mutableStateOf(Point()) }

        val density = LocalDensity.current.density

        @Language("GLSL")
        val displaceSksl = """
            uniform shader content;
            uniform float width;
            uniform float height;
            uniform float intensityAmount;
            uniform vec2 clickPoint;
    
            vec4 main(vec2 coord) {
                vec4 c = content.eval(coord);
                if (intensityAmount == 0.0) {
                    return c;
                }

                
                // How far are we from the focal point?
                vec2 focalPoint = clickPoint;
                float relativeToFocal = 
                    min(length(coord - focalPoint) / length(vec2(width, height) / 2.0), 0.4);

                // Emphasize the effect towards the corners, pushing it away from the focal point a bit
                float weighedDistance = intensityAmount * pow(relativeToFocal, 1.2);
                
                // Green channel is from this coordinate
                float luminanceHere = dot(c.rgb, vec3(0.2126, 0.7152, 0.0722));
                float green = luminanceHere * c.a;
                
                // Blue channel is taken from a pixel further from the focal point
                vec2 coordForBlue = coord + weighedDistance * 0.1 * (coord - focalPoint);
                vec4 colorForBlue = content.eval(coordForBlue);
                float luminanceForBlue = dot(colorForBlue.rgb, vec3(0.2126, 0.7152, 0.0722));
                float blue = luminanceForBlue * colorForBlue.a;
                
                // Red channel is taken from a pixel closer to the focal point
                vec2 coordForRed = coord - weighedDistance * 0.1 * (coord - focalPoint);
                vec4 colorForRed = content.eval(coordForRed);
                float luminanceForRed = dot(colorForRed.rgb, vec3(0.2126, 0.7152, 0.0722));
                float red = luminanceForRed * colorForRed.a;
                    
                return vec4(red, green, blue, c.a);
            }
        """

        val displaceRuntimeEffect = RuntimeEffect.makeForShader(displaceSksl)
        val displaceShaderBuilder = RuntimeShaderBuilder(displaceRuntimeEffect)
        displaceShaderBuilder.uniform("width", mapSize.value.width.toFloat())
        displaceShaderBuilder.uniform("height", mapSize.value.height.toFloat())
        displaceShaderBuilder.uniform("intensityAmount", intensityAmount.value)
        displaceShaderBuilder.uniform("clickPoint", clickPoint.value.x * density,
            clickPoint.value.y * density)

        val coroutineScope = rememberCoroutineScope()
        Image(
            painter = painterResource("/map/worldmap-small.png"),
            contentDescription = "Sample",
            modifier = Modifier.fillMaxSize()
                .onSizeChanged { mapSize.value = it }
                .graphicsLayer(
                    renderEffect = ImageFilter.makeRuntimeShader(
                        runtimeShaderBuilder = displaceShaderBuilder,
                        shaderName = "content",
                        input = null
                    ).asComposeRenderEffect(),
                    clip = true
                )
                .onPointerEvent(eventType = PointerEventType.Press) {
                    val mouseEvent = it.nativeEvent as MouseEvent
                    clickPoint.value = mouseEvent.point
                    coroutineScope.launch {
                        intensityAmount.animateTo(targetValue = 0.0f,
                            animationSpec = keyframes {
                                durationMillis = 200
                                0.0f at 0 with FastOutSlowInEasing
                                1.0f at 150
                            }
                        )
                    }
                }
        )
    }
}
