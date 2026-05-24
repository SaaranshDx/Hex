package hex.capes.client.render

import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier

object HexCapeTexture {

    private const val VANILLA_CAPE_WIDTH = 64
    private const val VANILLA_CAPE_HEIGHT = 32

    private val SOURCE_TEXTURE = Identifier.of("hex", "textures/cape/legacy_source.png")
    val TEXTURE_ID: Identifier = Identifier.of("hex", "textures/cape/legacy_runtime.png")

    private var initialized = false

    fun initialize(client: MinecraftClient) {
        if (initialized) {
            return
        }

        val bakedTexture = client.resourceManager.getResourceOrThrow(SOURCE_TEXTURE).inputStream.use { stream ->
            val sourceImage = NativeImage.read(stream)
            bakeCapeProviderTexture(sourceImage)
        }

        client.textureManager.registerTexture(
            TEXTURE_ID,
            NativeImageBackedTexture({ "hex_legacy_cape" }, bakedTexture)
        )
        initialized = true
    }

    private fun bakeCapeProviderTexture(sourceImage: NativeImage): NativeImage {
        val sourceWidth = sourceImage.width
        val sourceHeight = sourceImage.height

        var imageWidth = VANILLA_CAPE_WIDTH
        var imageHeight = VANILLA_CAPE_HEIGHT
        while (imageWidth < sourceWidth || imageHeight < sourceHeight) {
            imageWidth *= 2
            imageHeight *= 2
        }

        val bakedImage = NativeImage(imageWidth, imageHeight, true)
        for (x in 0 until sourceWidth) {
            for (y in 0 until sourceHeight) {
                bakedImage.setColorArgb(x, y, sourceImage.getColorArgb(x, y))
            }
        }
        sourceImage.close()

        return bakedImage
    }
}
