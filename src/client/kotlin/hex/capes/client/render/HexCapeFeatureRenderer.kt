package hex.capes.client.render

import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.feature.FeatureRenderer
import net.minecraft.client.render.entity.feature.FeatureRendererContext
import net.minecraft.client.render.entity.model.PlayerEntityModel
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerModelPart
import net.minecraft.item.Items
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.RotationAxis

class HexCapeFeatureRenderer(
    context: FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>>,
    @Suppress("UNUSED_PARAMETER") rendererContext: EntityRendererFactory.Context
) : FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>>(context) {

    @Volatile
    private var lastSyncTime: Long = 0L
    private val SYNC_INTERVAL_MS = 10_000L // Sync every 10 seconds

    override fun render(
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        player: AbstractClientPlayerEntity,
        limbAngle: Float,
        limbDistance: Float,
        tickDelta: Float,
        animationProgress: Float,
        headYaw: Float,
        headPitch: Float
    ) {
        if (player.isInvisible || !player.isPartVisible(PlayerModelPart.CAPE)) {
            return
        }

        // Periodically sync with server to get cape URLs for all players
        syncWithServerIfNeeded()

        val textureId = HexCapeTexture.getTextureId(player.gameProfile.name) ?: return
        if (player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
            return
        }

        matrices.push()
        matrices.translate(0.0f, 0.0f, 0.125f)

        val capeX = MathHelper.lerp(tickDelta.toDouble(), player.prevCapeX, player.capeX) -
            MathHelper.lerp(tickDelta.toDouble(), player.prevX, player.x)
        val capeY = MathHelper.lerp(tickDelta.toDouble(), player.prevCapeY, player.capeY) -
            MathHelper.lerp(tickDelta.toDouble(), player.prevY, player.y)
        val capeZ = MathHelper.lerp(tickDelta.toDouble(), player.prevCapeZ, player.capeZ) -
            MathHelper.lerp(tickDelta.toDouble(), player.prevZ, player.z)
        val bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw)
        val yawSin = MathHelper.sin(bodyYaw * (Math.PI.toFloat() / 180.0f)).toDouble()
        val yawCos = -MathHelper.cos(bodyYaw * (Math.PI.toFloat() / 180.0f)).toDouble()

        var capePitch = (capeY.toFloat() * 10.0f).coerceIn(-6.0f, 32.0f)
        var capeForward = ((capeX * yawSin + capeZ * yawCos).toFloat() * 100.0f).coerceIn(0.0f, 150.0f)
        val capeSide = ((capeX * yawCos - capeZ * yawSin).toFloat() * 100.0f).coerceIn(-20.0f, 20.0f)
        if (capeForward < 0.0f) {
            capeForward = 0.0f
        }

        val strideDistance = MathHelper.lerp(tickDelta, player.prevStrideDistance, player.strideDistance)
        capePitch += MathHelper.sin(MathHelper.lerp(tickDelta, player.prevHorizontalSpeed, player.horizontalSpeed) * 6.0f) *
            32.0f * strideDistance

        if (player.isInSneakingPose) {
            capePitch += 25.0f
        }

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6.0f + capeForward / 2.0f + capePitch))
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(capeSide / 2.0f))
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - capeSide / 2.0f))

        val vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntitySolid(textureId))
        contextModel.renderCape(
            matrices,
            vertexConsumer,
            light,
            OverlayTexture.DEFAULT_UV
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
}
