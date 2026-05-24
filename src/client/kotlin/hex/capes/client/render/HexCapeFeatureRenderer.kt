package hex.capes.client.render

import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayers
import net.minecraft.client.render.command.OrderedRenderCommandQueue
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.feature.FeatureRenderer
import net.minecraft.client.render.entity.feature.FeatureRendererContext
import net.minecraft.client.render.entity.model.EntityModelLayers
import net.minecraft.client.render.entity.model.PlayerCapeModel
import net.minecraft.client.render.entity.model.PlayerEntityModel
import net.minecraft.client.render.entity.state.PlayerEntityRenderState
import net.minecraft.client.util.math.MatrixStack

class HexCapeFeatureRenderer(
    context: FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>,
    rendererContext: EntityRendererFactory.Context
) : FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel>(context) {

    private val capeModel = PlayerCapeModel(rendererContext.getPart(EntityModelLayers.PLAYER_CAPE))

    override fun render(
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        state: PlayerEntityRenderState,
        limbAngle: Float,
        limbDistance: Float
    ) {
        if (state.invisible) {
            return
        }

        matrices.push()
        queue.submitModel(
            capeModel,
            state,
            matrices,
            RenderLayers.armorCutoutNoCull(HexCapeTexture.TEXTURE_ID),
            light,
            OverlayTexture.DEFAULT_UV,
            state.outlineColor,
            null
        )
        matrices.pop()
    }
}
