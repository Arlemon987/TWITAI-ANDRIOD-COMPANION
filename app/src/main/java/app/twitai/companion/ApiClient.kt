package app.twitai.companion

import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class GenerateRequest(
    val tweet: String,
    val replyCount: Int = 5,
    val minWords: Int = 10,
    val maxWords: Int = 18,
    val tone: String = "balanced",
    val tag: String = "",
    val language: String = "auto"
)

data class GenerateResponse(val replies: List<String>)

class ApiClient {
    private val gson = Gson()

    suspend fun generate(request: GenerateRequest): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                    ?: return@withContext Result.failure(Exception("Please sign in to Twit AI first."))

                val token = user.getIdToken(true).await().token
                    ?: return@withContext Result.failure(Exception("Could not get Firebase session."))

                val connection =
                    URL("${Config.API_BASE}/api/generate").openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 60000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $token")

                val body = gson.toJson(request)
                connection.outputStream.use {
                    it.write(body.toByteArray(StandardCharsets.UTF_8))
                }

                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream
                else connection.errorStream

                val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

                if (code !in 200..299) {
                    val message = try {
                        JsonParser.parseString(responseText)
                            .asJsonObject["error"]?.asString
                    } catch (_: Exception) {
                        null
                    }
                    return@withContext Result.failure(
                        Exception(message ?: "Twit AI request failed ($code)")
                    )
                }

                val json = JsonParser.parseString(responseText).asJsonObject
                val replies = json["replies"]?.asJsonArray
                    ?.mapNotNull { if (it.isJsonPrimitive) it.asString else null }
                    ?: emptyList()

                if (replies.isEmpty()) {
                    Result.failure(Exception("No replies returned by Twit AI."))
                } else {
                    Result.success(replies)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
