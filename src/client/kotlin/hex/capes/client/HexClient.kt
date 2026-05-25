package hex.capes.client

import hex.capes.client.render.HexCapeFeatureRenderer
import hex.capes.client.render.HexCapeTexture
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback
import net.minecraft.client.render.entity.PlayerEntityRenderer
import net.minecraft.client.render.entity.feature.FeatureRendererContext
import net.minecraft.client.render.entity.model.PlayerEntityModel
import net.minecraft.client.render.entity.state.PlayerEntityRenderState
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object HexClient : ClientModInitializer {

	override fun onInitializeClient() {
		ClientLifecycleEvents.CLIENT_STARTED.register(HexCapeTexture::initialize)
		ClientLifecycleEvents.CLIENT_STOPPING.register { HexCapeTexture.shutdown() }

		ClientPlayConnectionEvents.JOIN.register { handler, _, client ->
			HexCapeTexture.primeKnownPlayers(client, handler.playerList.mapNotNull { it.profile?.name })
		}

// update checks

		ClientPlayConnectionEvents.JOIN.register { handler, _, client ->

			HexCapeTexture.primeKnownPlayers(
				client,
				handler.playerList.mapNotNull { it.profile?.name }
			)

			HexServers.fetchServerConfig()

			if (HexServers.updateRequired) {


				client.player?.sendMessage(
					Text.literal("A new Hex update expect broken features or consider updating")
						.styled { it.withColor(Formatting.RED) },
					false
				)
			}
		}

		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			dispatcher.register(
				ClientCommandManager.literal("reloadcosmetics")
					.executes { context ->
						HexCapeTexture.reloadAll()
						context.source.sendFeedback(Text.literal("Reloading cosmetics cache."))
						1
					}
			)
		}

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
