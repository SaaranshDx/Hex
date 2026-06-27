package hex.capes.client.mixin

import com.mojang.authlib.GameProfile
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.client.texture.PlayerSkinTextureDownloader
import net.minecraft.client.texture.TextureManager
import net.minecraft.entity.player.SkinTextures
import net.minecraft.util.AssetInfo.TextureAssetInfo
import net.minecraft.util.Identifier
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.net.Proxy
import java.util.concurrent.ConcurrentHashMap

@Mixin(PlayerListEntry::class)
object HexCapesSkinMixin {

    private val skinCache = ConcurrentHashMap<String, Identifier?>()

    private val pending = ConcurrentHashMap.newKeySet<String>()

    @Inject(
        method = ["getSkinTextures"],
        at = [At("RETURN")],
        cancellable = true
    )
    private fun onGetSkinTextures(
        cir: CallbackInfoReturnable<SkinTextures>
    ) {
        @Suppress("CAST_NEVER_SUCCEEDS")
        val entry = this as PlayerListEntry
        val profile: GameProfile = entry.profile
        val username: String = profile.name ?: return

        when {
            skinCache.containsKey(username) -> {
                val customId = skinCache[username] ?: return
                val original = cir.returnValue ?: return
                cir.returnValue = SkinTextures(
                    TextureAssetInfo(customId, customId),
                    original.cape(),
                    original.elytra(),
                    original.model(),
                    original.secure()
                )
            }

            !pending.contains(username) -> {
                pending.add(username)
                fetchAndCacheSkin(username)
            }
        }
    }

    private fun fetchAndCacheSkin(username: String) {
        val client = MinecraftClient.getInstance()
        val textureManager: TextureManager = client.textureManager

        val id = Identifier.of("hexcapes", "skin/${username.lowercase()}")
        val url = "https://api.hexcapes.qzz.io/assets/skins/${username}.png"

        val downloader = PlayerSkinTextureDownloader(
            Proxy.NO_PROXY,
            textureManager,
            client::execute
        )

        downloader.downloadAndRegisterTexture(id, null, url, false).whenComplete { _, throwable ->
            if (throwable != null) {
                skinCache[username] = null
                pending.remove(username)
            } else {
                skinCache[username] = id
                pending.remove(username)
            }
        }
    }
}
