package org.pushingpixels.artemis.shaders

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import java.io.File
import javax.imageio.ImageIO

fun main() {
    val imageBitmap = ImageBitmap(600, 600)
    val bitmapCanvas = Canvas(imageBitmap)
    bitmapCanvas.saveLayer(
        Rect(Offset.Zero, Size(600.0f, 600.0f)),
        Paint().apply {
            asFrameworkPaint().imageFilter = ImageFilter.makeBlur(
                sigmaX = 5.0f,
                sigmaY = 5.0f,
                mode = FilterTileMode.DECAL
            )
        }
    )

    bitmapCanvas.drawOval(left = 50.0f, top = 50.0f, right = 550.0f, bottom = 550.0f,
        paint = Paint().apply {
            color = Color.Red
        }
    )

    bitmapCanvas.restore()

    ImageIO.write(imageBitmap.toAwtImage(), "png", File(System.getProperty("user.home"), "bitmap.png"))
}