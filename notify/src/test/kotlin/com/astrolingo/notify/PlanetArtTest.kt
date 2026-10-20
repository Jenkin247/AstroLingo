package com.astrolingo.notify

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PlanetArtTest {
    @Test fun `every planet has a wide picture and a round icon`() {
        Planet.entries.forEach { planet ->
            val scene = PlanetArt.scene(planet)
            assertThat(scene.width).isEqualTo(2 * scene.height)
            assertThat(PlanetArt.icon(planet).width).isEqualTo(PlanetArt.ICON_SIZE)
        }
    }

    @Test fun `the sky is dark and calm, the planet lit`() {
        val scene = PlanetArt.scene(Planet.VENUS)
        // Top-left corner is night sky; the planet's centre is bright.
        val corner = scene.getPixel(2, 2)
        val centre = scene.getPixel((PlanetArt.SCENE_WIDTH * 0.62f).toInt(), PlanetArt.SCENE_HEIGHT / 2)
        assertThat(brightness(corner)).isLessThan(70)
        assertThat(brightness(centre)).isGreaterThan(150)
    }

    private fun brightness(pixel: Int) = ((pixel shr 16 and 0xFF) + (pixel shr 8 and 0xFF) + (pixel and 0xFF)) / 3
}
