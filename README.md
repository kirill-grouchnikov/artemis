# Artemis

Artemis is a playground for exploring canvas APIs in Compose Desktop and the underlying Skia.

## Seasonal spiral

<img src="https://raw.githubusercontent.com/kirill-grouchnikov/artemis/main/docs/seasonal-spiral.png" border=0>

[This demo](https://github.com/kirill-grouchnikov/artemis/blob/woodland/src/main/kotlin/org/pushingpixels/artemis/charts/SeasonalSpiral.kt) loads daily visits data to a specific Wikipedia page (either remotely with Retrofit and Moshi, or from a local JSON file), and then displays that data as a seasonal spiral based on the visuals from [this article](https://observablehq.com/@yurivish/seasonal-spirals).

It also has a `DrawScope` extension function that draws a string using the specified paint along the specified path, bringing Android's `Canvas.drawTextOnPath` API to the desktop version of Compose.

## Glassmorphic

<img src="https://raw.githubusercontent.com/kirill-grouchnikov/artemis/main/docs/glassmorphic.png" border=0>

[This demo](https://github.com/kirill-grouchnikov/artemis/blob/woodland/src/main/kotlin/org/pushingpixels/artemis/Glassmorphic.kt) showcases shader-based render effects that are applied on render node content, combining multiple effects (blur, drop shadow, gradients, noise) in a single shader, based on the visuals from [this article](https://uxmisfit.com/2021/01/13/how-to-create-glassmorphic-card-ui-design/).
