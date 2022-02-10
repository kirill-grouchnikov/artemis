# Artemis

Artemis is a playground for exploring canvas APIs in Compose Desktop and the underlying Skia.

## Seasonal spiral

<img src="https://raw.githubusercontent.com/kirill-grouchnikov/artemis/main/docs/seasonal-spiral.png" border=0>

This demo loads daily visits data to a specific Wikipedia page (either remotely with Retrofit and Moshi, or from a local JSON file), and then displays that data as a seasonal spiral based on the visuals from [this article](https://observablehq.com/@yurivish/seasonal-spirals).

It also has a `DrawScope` extension function that draws a string using the specified paint along the specified path, bringing Android's `Canvas.drawTextOnPath` API to the desktop version of Compose.
