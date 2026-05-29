package hex.capes

import com.google.gson.JsonParser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import net.minecraft.util.Util
object HexServers {

    var httpsServer: String = ""
    var clientServer: String = ""
    var versionApi: String = ""
    var clientVersion: String = ""
    var updateRequired: Boolean = false
    var catalogurl: String = ""

    fun fetchServerConfig() {

        try {

            val client = HttpClient.newHttpClient()

            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://raw.githubusercontent.com/SaaranshDx/Hex/refs/heads/main/server.json"))
                .GET()
                .build()

            val response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            )

            val json = JsonParser
                .parseString(response.body())
                .asJsonObject

            httpsServer = json["httpsserver"].asString
            clientServer = json["clientserver"].asString
            versionApi = json["version-api"].asString
            clientVersion = json["client-version"].asString
            updateRequired = json["updaterequired"].asBoolean
            catalogurl = json["catalogurl"].asString

            println("HTTPS Server: $httpsServer")
            println("Client Server: $clientServer")
            println("Version API: $versionApi")
            println("Client Version: $clientVersion")
            println("Update Required: $updateRequired")
            println("Catalog URL: $catalogurl")
        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    var playerRegistrationState: Boolean = false

    fun reloadAll() {
        fetchServerConfig()
        println("Reloaded all server configs and cache.")
    }

    fun fetchPlayerRegistrationState(playerName: String) {

        try {

            val client = HttpClient.newHttpClient()

            val request = HttpRequest.newBuilder()
                .uri(
                    URI.create(
                        "$clientServer/registration-state/$playerName"
                    )
                )
                .GET()
                .build()

            val response = client.send(
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

    fun isUpdateRequired(): Boolean {
        return versionApi > clientVersion
    }

    var updateRequiredstatus: Boolean = isupdateRequired()

}