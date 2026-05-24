package hex.capes

import com.google.gson.JsonParser
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

object Hex : ModInitializer {

	private val logger = LoggerFactory.getLogger("hex")
	private val httpClient = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(10))
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build()

	override fun onInitialize() {
		logger.info("Hex initialized")
	}

	fun fetchUserdata(username: String): ProfileResponse? {
		try {
			val request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:8000/profile/${encodePathSegment(username)}"))
				.GET()
				.timeout(Duration.ofSeconds(10))
				.build()

			val response = httpClient.send(
				request,
				HttpResponse.BodyHandlers.ofString()
			)
			if (response.statusCode() !in 200..299) {
				logger.warn("Failed to fetch profile for {}: HTTP {}", username, response.statusCode())
				return null
			}

			val root = JsonParser.parseString(response.body()).asJsonObject
			val cape = root.get("cape")?.takeUnless { it.isJsonNull }?.asString
			return ProfileResponse(cape)
		} catch (e: Exception) {
			logger.warn("Failed to fetch profile for {}", username, e)
			return null
		}
	}

	fun downloadBytes(url: String): ByteArray? {
		return try {
			val request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.GET()
				.timeout(Duration.ofSeconds(15))
				.build()
			val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
			if (response.statusCode() !in 200..299) {
				logger.warn("Failed to download texture from {}: HTTP {}", url, response.statusCode())
				return null
			}

			response.body()
		} catch (e: Exception) {
			logger.warn("Failed to download texture from {}", url, e)
			null
		}
	}

	private fun encodePathSegment(value: String): String {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
	}

	data class ProfileResponse(val cape: String?)
}
