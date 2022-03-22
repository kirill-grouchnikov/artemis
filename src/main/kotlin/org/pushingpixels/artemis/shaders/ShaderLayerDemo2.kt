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

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asSkiaColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.skia.*
import org.pushingpixels.aurora.component.model.Command
import org.pushingpixels.aurora.component.model.CommandActionPreview
import org.pushingpixels.aurora.component.projection.CommandButtonProjection
import org.pushingpixels.aurora.theming.businessSkin
import org.pushingpixels.aurora.theming.colorscheme.OrangeColorScheme
import org.pushingpixels.aurora.theming.utils.getColorSchemeFilter
import org.pushingpixels.aurora.window.AuroraWindow
import org.pushingpixels.aurora.window.auroraApplication
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun main() = auroraApplication {
    val state = rememberWindowState(
        placement = WindowPlacement.Floating,
        position = WindowPosition.Aligned(Alignment.Center),
        size = DpSize(300.dp, 120.dp)
    )

    val skin = mutableStateOf(businessSkin())

    AuroraWindow(
        skin = skin,
        title = "Filter Demo",
        state = state,
        undecorated = true,
        onCloseRequest = ::exitApplication,
    ) {

        val blurAmount = remember { Animatable(3.0f) }
        val blurFilter: ImageFilter = ImageFilter.makeBlur(
            sigmaX = blurAmount.value,
            sigmaY = blurAmount.value,
            mode = FilterTileMode.DECAL
        )
        val blurEffect = blurFilter.asComposeRenderEffect()
        val coroutineScope = rememberCoroutineScope()

        Box(modifier = Modifier.fillMaxSize()) {
            CommandButtonProjection(
                contentModel = Command(text = "Click me!", action = { println("Clicked!") },
                    actionPreview = object : CommandActionPreview {
                        override fun onCommandPreviewActivated(command: Command) {
                            coroutineScope.launch {
                                blurAmount.animateTo(
                                    targetValue = 0.1f,
                                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                )
                            }
                        }

                        override fun onCommandPreviewCanceled(command: Command) {
                            coroutineScope.launch {
                                blurAmount.animateTo(
                                    targetValue = 3.0f,
                                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                )
                            }
                        }
                    })
            ).project(
                modifier = Modifier.graphicsLayer(renderEffect = blurEffect).align(Alignment.Center)
            )
        }
    }
}

