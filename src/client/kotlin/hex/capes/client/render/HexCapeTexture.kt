package hex.capes.client.render

import hex.capes.Hex
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object HexCapeTexture {

    private const val VANILLA_CAPE_WIDTH = 64
    private const val VANILLA_CAPE_HEIGHT = 32
    private const val REFRESH_INTERVAL_MS = 600_000L // Only refresh every 10 mins

    private val logger = LoggerFactory.getLogger("hex")
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "hex-cape-cache").apply { isDaemon = true }
    }
    private val entries = ConcurrentHashMap<String, CapeEntry>()
    private val trackedUsers = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    private var client: MinecraftClient? = null

    @Volatile
    private var cacheDirectory: Path? = null

    @Volatile
    private var trackedUsersFile: Path? = null

    fun initialize(client: MinecraftClient) {
        this.client = client

        val baseDirectory = client.runDirectory.toPath().resolve("hex").resolve("capes")
        val trackedFile = baseDirectory.resolve("tracked-users.txt")

        Files.createDirectories(baseDirectory)
        cacheDirectory = baseDirectory
        trackedUsersFile = trackedFile

        loadTrackedUsers().forEach { username ->
            trackUser(username)
            loadFromDisk(username)
            queueRefresh(username)
        }

        client.session?.username?.takeIf { it.isNotBlank() }?.let { username ->
            trackUser(username)
            loadFromDisk(username)
            queueRefresh(username)
        }
    }

    fun shutdown() {
        executor.shutdownNow()
    }

    fun primeKnownPlayers(client: MinecraftClient, usernames: Collection<String>) {
        initialize(client)

        usernames
            .map(String::trim)
            .filter(String::isNotEmpty)
            .forEach { username ->
                trackUser(username)
                loadFromDisk(username)
                queueRefresh(username)
            }
    }

    fun reloadAll() {
        val client = client ?: return
        initialize(client)

        val usernames = linkedSetOf<String>()
        usernames.addAll(trackedUsers)
        client.session?.username?.takeIf { it.isNotBlank() }?.let(usernames::add)
        client.networkHandler?.playerList
            ?.mapNotNull { it.profile?.name }
            ?.filter { it.isNotBlank() }
            ?.forEach(usernames::add)

        usernames.forEach { username ->
            trackUser(username)
            loadFromDisk(username)
            queueRefresh(username)
        }
    }

    fun getTextureId(username: String?): Identifier? {
        val normalizedName = username?.trim()?.takeIf { it.isNotEmpty() } ?: return null

        trackUser(normalizedName)
        loadFromDisk(normalizedName)
        queueRefresh(normalizedName)

        val entry = entries[cacheKey(normalizedName)] ?: return null
        return if (entry.registered) entry.textureId else null
    }

    private fun loadTrackedUsers(): List<String> {
        val trackedFile = trackedUsersFile ?: return emptyList()
        if (!Files.exists(trackedFile)) {
            return emptyList()
        }

        return try {
            Files.readAllLines(trackedFile)
                .map(String::trim)
                .filter(String::isNotEmpty)
        } catch (exception: Exception) {
            logger.warn("Failed to read tracked users cache", exception)
            emptyList()
        }
    }

    private fun trackUser(username: String) {
        trackedUsers += username
        persistTrackedUsers()
        entries.computeIfAbsent(cacheKey(username)) { createEntry(username) }
    }

    private fun persistTrackedUsers() {
        val trackedFile = trackedUsersFile ?: return

        try {
            Files.createDirectories(trackedFile.parent)
            Files.write(
                trackedFile,
                trackedUsers
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .sorted()
            )
        } catch (exception: Exception) {
            logger.warn("Failed to persist tracked users cache", exception)
        }
    }

    private fun loadFromDisk(username: String) {
        val client = client ?: return
        val entry = entries.computeIfAbsent(cacheKey(username)) { createEntry(username) }
        if (!entry.diskLoadPending.compareAndSet(true, false)) {
            return
        }

        val cacheFile = cacheFile(username)
        if (!Files.exists(cacheFile)) {
            return
        }

        try {
            val imageBytes = Files.readAllBytes(cacheFile)
            registerTexture(client, entry, imageBytes)
        } catch (exception: Exception) {
            logger.warn("Failed to load cached cape for {}", username, exception)
        }
    }

    private fun queueRefresh(username: String) {
        val entry = entries.computeIfAbsent(cacheKey(username)) { createEntry(username) }
        if (!entry.refreshInFlight.compareAndSet(false, true)) {
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - entry.lastRefreshTime < REFRESH_INTERVAL_MS) {
            entry.refreshInFlight.set(false)
            return
        }

        executor.execute {
            try {
                refreshCape(username, entry)
            } finally {
                entry.refreshInFlight.set(false)
                entry.lastRefreshTime = System.currentTimeMillis()
            }
        }
    }

    private fun refreshCape(username: String, entry: CapeEntry) {
        val client = client ?: return
        val profile = Hex.fetchUserdata(username)

        val capeUrl = profile?.cape?.trim().orEmpty()
        if (capeUrl.isEmpty()) {
            clearTexture(client, entry, username)
            return
        }

        val imageBytes = Hex.downloadBytes(capeUrl)
            ?: readCachedTexture(username)
            ?: return

        writeCache(username, imageBytes)
        registerTexture(client, entry, imageBytes)
    }

    private fun registerTexture(client: MinecraftClient, entry: CapeEntry, imageBytes: ByteArray) {
        val bakedImage = NativeImage.read(imageBytes).use(::bakeCapeProviderTexture)

        client.execute {
            client.textureManager.destroyTexture(entry.textureId)
            client.textureManager.registerTexture(
                entry.textureId,
                NativeImageBackedTexture({ "hex_cape_${entry.cacheKey}" }, bakedImage)
            )
            entry.registered = true
        }
    }

    private fun clearTexture(client: MinecraftClient, entry: CapeEntry, username: String) {
        try {
            Files.deleteIfExists(cacheFile(username))
        } catch (exception: Exception) {
            logger.warn("Failed to delete cached cape for {}", username, exception)
        }

        client.execute {
            client.textureManager.destroyTexture(entry.textureId)
            entry.registered = false
            entry.diskLoadPending.set(true)
        }
    }

    private fun readCachedTexture(username: String): ByteArray? {
        val cacheFile = cacheFile(username)
        if (!Files.exists(cacheFile)) {
            return null
        }

        return try {
            Files.readAllBytes(cacheFile)
        } catch (exception: Exception) {
            logger.warn("Failed to read cached cape bytes for {}", username, exception)
            null
        }
    }

    private fun writeCache(username: String, imageBytes: ByteArray) {
        val cacheFile = cacheFile(username)
        val tempFile = cacheFile.resolveSibling("${cacheFile.fileName}.tmp")

        try {
            Files.createDirectories(cacheFile.parent)
            Files.write(tempFile, imageBytes)
            Files.move(
                tempFile,
                cacheFile,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (exception: Exception) {
            logger.warn("Failed to write cape cache for {}", username, exception)
            try {
                Files.deleteIfExists(tempFile)
            } catch (_: Exception) {
            }
        }
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

        return bakedImage
    }

    private fun cacheFile(username: String): Path {
        val directory = cacheDirectory
            ?: error("Cape cache used before client initialization")
        return directory.resolve("${sanitizePath(cacheKey(username))}.png")
    }

    private fun createEntry(username: String): CapeEntry {
        val cacheKey = cacheKey(username)
        return CapeEntry(
            cacheKey = cacheKey,
            textureId = Identifier.of("hex", "textures/cape/runtime/${sanitizePath(cacheKey)}")
        )
    }

    private fun cacheKey(username: String): String {
        return username.trim().lowercase(Locale.ROOT)
    }

    private fun sanitizePath(value: String): String {
        return buildString(value.length) {
            value.forEach { character ->
                when {
                    character in 'a'..'z' || character in '0'..'9' || character == '_' || character == '-' ->
                        append(character)
                    else -> append('_')
                }
            }
        }
    }

    private class CapeEntry(
        val cacheKey: String,
        val textureId: Identifier,
        val diskLoadPending: AtomicBoolean = AtomicBoolean(true),
        val refreshInFlight: AtomicBoolean = AtomicBoolean(false),
        @Volatile var registered: Boolean = false,
        @Volatile var lastRefreshTime: Long = 0L
    )
}
