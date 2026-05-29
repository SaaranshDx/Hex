package hex.capes

import com.google.gson.JsonParser
import net.minecraft.util.Util
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
object HexServers {

    //var httpsServer: String = ""
    var clientServer: String = ""
    var versionApi: String = ""
    //var clientVersion: String = ""
    //var updateRequired: Boolean = false
    var catalogurl: String = ""

    fun fetchServerConfig() {
        try {
            val client = HttpClient.newHttpClient()

            val request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:3000/server.json"))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build()

            val response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            )

            val json = JsonParser.parseString(response.body()).asJsonObject

            //httpsServer = json["httpsserver"]?.asString ?: ""
            clientServer = json["serverHost"]?.asString ?: ""
            versionApi = json["version-api"]?.asString ?: ""
            //updateRequired = json["updaterequired"]?.asBoolean ?: false
            catalogurl = json["catalogurl"]?.asString ?: ""
        } catch (e: Exception) {
            println("Failed to fetch remote config: ${e.message}")
        }

        if (clientServer.isEmpty()) {
            clientServer = "http://localhost:8000"
        }
    }

    var playerRegistrationState: Boolean = false

    fun reloadAll() {
        fetchServerConfig()
        println("Reloaded all server configs and cache.")
    }

    fun fetchPlayerRegistrationState(playerName: String) {

        try {

            val serverUrl = clientServer.takeIf { it.isNotEmpty() } ?: "http://localhost:8000"

            val request = HttpRequest.newBuilder()
                .uri(
                    URI.create(
                        "$serverUrl/registration-state/${Hex.encodePathSegment(playerName)}"
                    )
                )
                .GET()
                .build()

            val response = Hex.httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            )

            playerRegistrationState =
                response.body().toBoolean()

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    fun openCatalogue() {
        try {
            Util.getOperatingSystem().open(
                "$catalogurl"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var clientVersion = "1"

    fun isUpdateRequired(): Boolean {
        return versionApi > clientVersion
    }

    var updateRequiredstatus: Boolean = isUpdateRequired()

}