package hex.capes.client

import hex.capes.client.render.HexCapeFeatureRenderer
import hex.capes.client.render.HexCapeTexture
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback
import net.minecraft.client.render.entity.PlayerEntityRenderer
import net.minecraft.client.render.entity.feature.FeatureRendererContext
import net.minecraft.client.render.entity.model.PlayerEntityModel
import net.minecraft.client.render.entity.state.PlayerEntityRenderState

object HexClient : ClientModInitializer {

	override fun onInitializeClient() {
		ClientLifecycleEvents.CLIENT_STARTED.register(HexCapeTexture::initialize)

		LivingEntityFeatureRendererRegistrationCallback.EVENT.register { _, entityRenderer, registrationHelper, context ->
			if (entityRenderer is PlayerEntityRenderer<*>) {
				@Suppress("UNCHECKED_CAST")
				val playerRenderer =
					entityRenderer as FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>

				registrationHelper.register(HexCapeFeatureRenderer(playerRenderer, context))
			}
		}

		println("Hex client initialized.")
	}
}
