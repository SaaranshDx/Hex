package hex.capes.client.render

import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.equipment.EquipmentModel
import net.minecraft.client.render.entity.equipment.EquipmentModelLoader
import net.minecraft.client.render.entity.feature.FeatureRenderer
import net.minecraft.client.render.entity.feature.FeatureRendererContext
import net.minecraft.client.render.entity.model.EntityModelLayers
import net.minecraft.client.render.entity.model.PlayerCapeModel
import net.minecraft.client.render.entity.model.PlayerEntityModel
import net.minecraft.client.render.entity.state.PlayerEntityRenderState
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.component.DataComponentTypes
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack

class HexCapeFeatureRenderer(
    context: FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>,
    rendererContext: EntityRendererFactory.Context
) : FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel>(context) {

    private val capeModel = PlayerCapeModel<PlayerEntityRenderState>(rendererContext.entityModels.getModelPart(EntityModelLayers.PLAYER_CAPE))
    private val equipmentModelLoader: EquipmentModelLoader = rendererContext.equipmentModelLoader
    
    @Volatile
    private var lastSyncTime: Long = 0L
    private val SYNC_INTERVAL_MS = 10_000L // Sync every 10 seconds

    override fun render(
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        state: PlayerEntityRenderState,
        limbAngle: Float,
        limbDistance: Float
    ) {
        if (state.invisible || !state.capeVisible) {
            return
        }

        // Periodically sync with server to get cape URLs for all players
        syncWithServerIfNeeded()

        val textureId = HexCapeTexture.getTextureId(resolveUsername(state)) ?: return
        if (hasCustomModelForLayer(state.equippedChestStack, EquipmentModel.LayerType.WINGS)) {
            return
        }

        matrices.push()
        if (hasCustomModelForLayer(state.equippedChestStack, EquipmentModel.LayerType.HUMANOID)) {
            matrices.translate(0.0f, -0.053125f, 0.06875f)
        }

        render(
            capeModel,
            textureId,
            matrices,
            vertexConsumers,
            light,
            state,
            -1
        )
        matrices.pop()
    }

    private fun syncWithServerIfNeeded() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSyncTime < SYNC_INTERVAL_MS) {
            return
        }
        
        lastSyncTime = currentTime
        val client = MinecraftClient.getInstance()
        HexCapeTexture.syncCapesWithServer(client)
    }

    private fun resolveUsername(state: PlayerEntityRenderState): String? {
        val player = MinecraftClient.getInstance().world?.getEntityById(state.id) as? PlayerEntity
        return player?.gameProfile?.name ?: state.playerName?.string?.trim()?.takeIf(String::isNotEmpty)
    }

    private fun hasCustomModelForLayer(stack: ItemStack, layerType: EquipmentModel.LayerType): Boolean {
        val equippable = stack.get(DataComponentTypes.EQUIPPABLE) ?: return false
        val assetId = equippable.assetId().orElse(null) ?: return false
        val equipmentModel = equipmentModelLoader.get(assetId)
        return equipmentModel.getLayers(layerType).isNotEmpty()
    }
}
