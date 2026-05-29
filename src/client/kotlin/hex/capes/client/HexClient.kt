package hex.capes.client

import hex.capes.client.render.HexCapeFeatureRenderer
import hex.capes.client.render.HexCapeTexture
import hex.capes.HexServers
import java.net.URI
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
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object HexClient : ClientModInitializer {

	override fun onInitializeClient() {
		ClientLifecycleEvents.CLIENT_STARTED.register(HexCapeTexture::initialize)
		ClientLifecycleEvents.CLIENT_STOPPING.register { HexCapeTexture.shutdown() }

		ClientPlayConnectionEvents.JOIN.register { handler, _, client ->

			HexCapeTexture.primeKnownPlayers(
				client,
				handler.playerList.mapNotNull { it.profile?.name }
			)

			HexServers.fetchServerConfig()

			val playerName = client.player?.name?.string ?: return@register

			// Check player registration state
			HexServers.fetchPlayerRegistrationState(playerName)

			// If registered, fetch profile and render cape
			if (HexServers.playerRegistrationState) {
				HexCapeTexture.queueRefresh(playerName)
			}

			if (HexServers.updateRequiredstatus) {
				client.player?.sendMessage(
					Text.literal("A new Hex update is available. Please update to the latest version to continue using Hex features. Update at ")
						.append(
							Text.literal(HexServers.catalogurl)
								.styled { it.withColor(Formatting.RED)
									.withClickEvent(ClickEvent.OpenUrl(URI.create("${HexServers.catalogurl}/update")))
									.withHoverEvent(HoverEvent.ShowText(Text.literal("Click to open link"))) }
						),
					false
				)
			}

			if (!HexServers.playerRegistrationState) {
				client.player?.sendMessage(
					Text.literal("Your account is not registered on the Hex servers any of the cosmetic features won't work register at ")
						.append(
							Text.literal(HexServers.catalogurl)
								.styled { it.withColor(Formatting.YELLOW)
									.withClickEvent(ClickEvent.OpenUrl(URI.create(HexServers.catalogurl)))
									.withHoverEvent(HoverEvent.ShowText(Text.literal("Click to open link"))) }
						),
					false
				)
			}
		}

// reload cache cmd

		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			dispatcher.register(
				ClientCommandManager.literal("reloadcache")
					.executes { context ->
						HexServers.reloadAll()
						val player = context.source.player
						if (player != null) {
							val name = player.name.string
							HexServers.fetchPlayerRegistrationState(name)
							if (HexServers.playerRegistrationState) {
								HexCapeTexture.queueRefresh(name)
							}
							HexCapeTexture.reloadAll()
						}
						context.source.sendFeedback(Text.literal("Reloaded cache."))
						1
					}
			)
		}

		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->

			dispatcher.register(
				ClientCommandManager.literal("catalogue")
					.executes { context ->

						try {

							HexServers.openCatalogue()

							context.source.sendFeedback(
								Text.literal("Opening cosmetics catalogue.")
							)

						} catch (e: Exception) {

							context.source.sendFeedback(
								Text.literal("Failed to open cosmetics catalogue.")
							)

							e.printStackTrace()
						}

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
