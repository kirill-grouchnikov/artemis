/*
 * Copyright (c) 2021-22 Artemis, Kirill Grouchnikov. All Rights Reserved.
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

package org.pushingpixels.artemis

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import org.jetbrains.skia.Data
import org.jetbrains.skia.RuntimeEffect
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun horizontalSrgbGradient(
    width: Float,
    colorStart: Color,
    colorEnd: Color
): Brush {
    val sksl = """
            // https://bottosson.github.io/posts/colorwrong/#what-can-we-do%3F
            vec3 linearSrgbToSrgb(vec3 x) {
                vec3 xlo = 12.92*x;
                vec3 xhi = 1.055 * pow(x, vec3(1.0/2.4)) - 0.055;
                return mix(xlo, xhi, step(vec3(0.0031308), x));
            
            }
            
            vec3 srgbToLinearSrgb(vec3 x) {
                vec3 xlo = x / 12.92;
                vec3 xhi = pow((x + 0.055)/(1.055), vec3(2.4));
                return mix(xlo, xhi, step(vec3(0.04045), x));
            }
            
            uniform vec4 start;
            uniform vec4 end;
            uniform float width;

            half4 main(vec2 fragcoord) {
               // Implicit assumption in here that colors are full opacity
               float fraction = fragcoord.x / width;
               // Convert start and end colors to linear SRGB
               vec3 linearStart = srgbToLinearSrgb(start.xyz);
               vec3 linearEnd = srgbToLinearSrgb(end.xyz);
               // Interpolate in linear SRGB space
               vec3 linearInterpolated = mix(linearStart, linearEnd, fraction);
               // And convert back to SRGB
               return vec4(linearSrgbToSrgb(linearInterpolated), 1.0);
            }
        """

    val dataBuffer = ByteBuffer.allocate(36).order(ByteOrder.LITTLE_ENDIAN)
    // RGBA colorLight
    dataBuffer.putFloat(0, colorStart.red)
    dataBuffer.putFloat(4, colorStart.green)
    dataBuffer.putFloat(8, colorStart.blue)
    dataBuffer.putFloat(12, colorStart.alpha)
    // RGBA colorDark
    dataBuffer.putFloat(16, colorEnd.red)
    dataBuffer.putFloat(20, colorEnd.green)
    dataBuffer.putFloat(24, colorEnd.blue)
    dataBuffer.putFloat(28, colorEnd.alpha)
    // Width
    dataBuffer.putFloat(32, width)

    val effect = RuntimeEffect.makeForShader(sksl)
    val shader = effect.makeShader(
        uniforms = Data.makeFromBytes(dataBuffer.array()),
        children = null,
        localMatrix = null,
        isOpaque = false
    )

    return ShaderBrush(shader)
}