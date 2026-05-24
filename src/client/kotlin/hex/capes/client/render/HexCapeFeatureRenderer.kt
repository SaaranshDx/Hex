package hex.capes.client.render

import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayers
import net.minecraft.client.render.command.OrderedRenderCommandQueue
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

    private val capeModel = PlayerCapeModel(rendererContext.entityModels.getModelPart(EntityModelLayers.PLAYER_CAPE))
    private val equipmentModelLoader: EquipmentModelLoader = rendererContext.equipmentModelLoader

    override fun render(
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        state: PlayerEntityRenderState,
        limbAngle: Float,
        limbDistance: Float
    ) {
        if (state.invisible || !state.capeVisible) {
            return
        }

        val textureId = HexCapeTexture.getTextureId(resolveUsername(state)) ?: return
        if (hasCustomModelForLayer(state.equippedChestStack, EquipmentModel.LayerType.WINGS)) {
            return
        }

        matrices.push()
        if (hasCustomModelForLayer(state.equippedChestStack, EquipmentModel.LayerType.HUMANOID)) {
            matrices.translate(0.0f, -0.053125f, 0.06875f)
        }

        queue.submitModel(
            capeModel,
            state,
            matrices,
            RenderLayers.entitySolid(textureId),
            light,
            OverlayTexture.DEFAULT_UV,
            state.outlineColor,
            null
        )
        matrices.pop()
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
