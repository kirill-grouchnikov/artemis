/*
 * Copyright (c) 2021-24 Artemis, Kirill Grouchnikov. All Rights Reserved.
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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import org.intellij.lang.annotations.Language
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

@Language("GLSL")
private val flutedGlassSksl = """
    uniform shader content;
    uniform shader blur1;
    uniform shader blur2;
    uniform float width;
    uniform float height;

    vec4 main(vec2 coord) {
        vec4 c = content.eval(coord);
        vec4 b1 = blur1.eval(coord);
        vec4 b2 = blur2.eval(coord);
        float intensifiedBlackBlurAlpha = 1.0 - pow(1.0 - b1.a, 5.0);
        vec4 intensifiedBlackBlur = vec4(b1.r, b1.g, b1.b, intensifiedBlackBlurAlpha); 
        vec4 originalWithBlur = (c.a == 1.0) ? c : intensifiedBlackBlur;

        if (fract(coord.x / 16.0) <= 0.25) {
            return mix(vec4(0.88, 0.88, 0.88, 1.0), vec4(0.38, 0.38, 0.38, 1.0), originalWithBlur.a);
        }
        
        bool isLeft = (fract(coord.x / 16.0) <= 0.5);
        bool isMiddle = !isLeft && (fract(coord.x / 16.0) <= 0.75);
        bool isRight = !isLeft && !isMiddle;
        
        
        vec3 purple = vec3(122.0, 103.0, 223.0);
        vec3 brown = vec3(152.0, 75.0, 45.0);
        
        float mixAmount = coord.x / width;
        float mixRed = mix(purple.r, brown.r, mixAmount) / 255.0;
        float mixGreen = mix(purple.g, brown.g, mixAmount) / 255.0;
        float mixBlue = mix(purple.b, brown.b, mixAmount) / 255.0;
        float intensifiedMixAlpha = 1.0 - pow(1.0 - b2.a, 3.0);
        vec4 mixed = vec4(mixRed, mixGreen, mixBlue, intensifiedMixAlpha);
        
        float red = min(c.x, b1.x);
        float green = min(c.y, b1.y);
        float blue = min(c.z, b1.z);
        float alpha = max(c.a, b1.a);

        vec4 allMixed = mix(mixed, originalWithBlur, originalWithBlur.a);
        // Mixed color blur blends into the original + blur
        float blendAmount = min(allMixed.a + intensifiedBlackBlurAlpha, 1.0);
        vec4 finalMixed = mix(mixed, allMixed, originalWithBlur.a);

        if (isLeft) {
            float alpha = pow(intensifiedBlackBlur.a, 1.9);
            return vec4(finalMixed.r, finalMixed.g, finalMixed.b, alpha);
        }
        if (isRight) {
            float alpha = pow(intensifiedBlackBlur.a, 1.3);//
            return vec4(finalMixed.r, finalMixed.g, finalMixed.b, alpha);
        }
        
        // Middle
        return finalMixed;
    }
"""

private val flutedGlassBuilder = RuntimeShaderBuilder(RuntimeEffect.makeForShader(flutedGlassSksl))

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Fluted Glass",
        state = rememberWindowState(
            placement = WindowPlacement.Floating,
            position = WindowPosition.Aligned(Alignment.Center),
            size = DpSize(800.dp, 800.dp)
        )
    ) {
        val imageSize = remember { mutableStateOf(IntSize(0, 0)) }

        flutedGlassBuilder.uniform("width", imageSize.value.width.toFloat())
        flutedGlassBuilder.uniform("height", imageSize.value.height.toFloat())

        Image(
            painter = painterResource("/icon/blur-icon.png"),
            contentDescription = "Icon",
            modifier = Modifier.fillMaxSize()
                .background(Color.LightGray)
                .onSizeChanged { imageSize.value = it }
                .graphicsLayer(
                    renderEffect = ImageFilter.makeRuntimeShader(
                        runtimeShaderBuilder = flutedGlassBuilder,
                        shaderNames = arrayOf("content", "blur1", "blur2"),
                        inputs = arrayOf(
                            null,
                            ImageFilter.makeBlur(
                                sigmaX = 24.0f, sigmaY = 24.0f, mode = FilterTileMode.DECAL
                            ),
                            ImageFilter.makeBlur(
                                sigmaX = 160.0f, sigmaY = 64.0f, mode = FilterTileMode.CLAMP
                            ),

                        )
                    ).asComposeRenderEffect(),
                    clip = true
                )
        )
    }
}
