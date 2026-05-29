package hex.capes.client

import hex.capes.client.render.HexCapeFeatureRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.render.entity.PlayerEntityRenderer
import net.minecraft.client.render.entity.model.PlayerEntityModel

object HexClient : ClientModInitializer {

    override fun onInitializeClient() {

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
            LivingEntityFeatureRendererRegistrationCallback { entityType, entityRenderer, registrationHelper ->

                if (entityRenderer is PlayerEntityRenderer) {
                    registrationHelper.register(
                        HexCapeFeatureRenderer(
                            entityRenderer as net.minecraft.client.render.entity.feature.FeatureRendererContext<
                                    AbstractClientPlayerEntity,
                                    PlayerEntityModel<AbstractClientPlayerEntity>
                                    >
                        )
                    )
                }
            }
        )

        println("Hex client initialized.")
    }
}