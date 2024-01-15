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
    uniform shader mainBlur;
    uniform shader haloBlur;
    uniform float width;
    uniform float height;

    vec4 main(vec2 coord) {
        vec4 blurColor = mainBlur.eval(coord);
        vec4 haloBlurColor = haloBlur.eval(coord);
        float intensifiedMainBlurAlpha = 1.0 - pow(1.0 - blurColor.a, 5.0);
        vec4 intensifiedMainBlur = vec4(blurColor.r, blurColor.g, blurColor.b, intensifiedMainBlurAlpha); 

        if (fract(coord.x / 16.0) <= 0.25) {
            return mix(vec4(0.88, 0.88, 0.88, 1.0), vec4(0.28, 0.28, 0.28, 1.0), intensifiedMainBlur.a);
        }
        
        float offsetFromCenter = abs(0.625 - fract(coord.x / 16.0));
        bool isLeft = (fract(coord.x / 16.0) <= 0.5);
        bool isMiddle = (offsetFromCenter <= 0.125);
        bool isRight = !isLeft && !isMiddle;
        
        vec3 purple = vec3(122.0, 103.0, 223.0);
        vec3 brown = vec3(152.0, 75.0, 45.0);
        
        float haloMixAmount = coord.x / width;
        float haloRed = mix(purple.r, brown.r, haloMixAmount) / 255.0;
        float haloGreen = mix(purple.g, brown.g, haloMixAmount) / 255.0;
        float haloBlue = mix(purple.b, brown.b, haloMixAmount) / 255.0;
        float intensifiedHaloAlpha = 1.0 - pow(1.0 - haloBlurColor.a, 3.0);
        vec4 haloColor = vec4(haloRed, haloGreen, haloBlue, intensifiedHaloAlpha);
        
        if (isMiddle) {
            // Porter-Duff SrcOver, source being the intensified main blue, and destination
            // being the colored halo
            vec3 middleColor = intensifiedMainBlur.rgb + (1 - intensifiedMainBlurAlpha) * haloColor.rgb;
            float middleAlpha = intensifiedMainBlurAlpha + (1 - intensifiedMainBlurAlpha) * haloColor.a;
            return vec4(middleColor.r, middleColor.g, middleColor.b, middleAlpha);
        }
        // Sides, extend the original blur based on the X offset from the band center
        vec4 allMixed = mix(haloColor, intensifiedMainBlur, intensifiedMainBlur.a);
        // Halo color blur blends into the main blur
        float blendAmount = min(allMixed.a + intensifiedMainBlurAlpha, 1.0);
        vec4 finalMixed = mix(haloColor, allMixed, intensifiedMainBlur.a);
        // Tweak the alpha. The sides fade away as they get further from the main blur,
        // creating an extended "rib" in the center.
        float alpha = pow(intensifiedMainBlur.a, 0.8 + offsetFromCenter);
        return vec4(finalMixed.r, finalMixed.g, finalMixed.b, alpha);
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
            painter = painterResource("/icon/reduce_capacity_200.png"),
            contentDescription = "Icon",
            modifier = Modifier.fillMaxSize()
                .background(Color.LightGray)
                .onSizeChanged { imageSize.value = it }
                .graphicsLayer(
                    renderEffect = ImageFilter.makeRuntimeShader(
                        runtimeShaderBuilder = flutedGlassBuilder,
                        shaderNames = arrayOf("content", "mainBlur", "haloBlur"),
                        inputs = arrayOf(
                            null,
                            ImageFilter.makeBlur(
                                sigmaX = 20.0f, sigmaY = 20.0f, mode = FilterTileMode.DECAL
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
