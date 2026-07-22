package com.resumestudio.data

import com.resumestudio.model.AICareerCoachReply
import com.resumestudio.model.CareerCoachContext
import com.resumestudio.model.CareerCoachMessage
import com.resumestudio.model.CareerCoachMessageRole
import com.resumestudio.model.ResumeAIAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to the same `/v1/ai` endpoint the iOS app uses.
 *
 * There is one backend and it does not know which client is asking, so the
 * request shape here is iOS's — action, client id, entitlement, payload — and
 * nothing about it is Android's to redesign.
 *
 * Auth is supplied rather than fetched. Firebase is not wired on Android yet,
 * and a client that silently sent unauthenticated requests would look like it
 * worked right up until the server started refusing them. [Tokens] makes the
 * absence explicit and [Result.Unauthenticated] says so out loud.
 */
class ResumeAIClient(
    private val baseUrl: String,
    private val clientId: String,
    private val tokens: suspend () -> Tokens,
) {

    /**
     * @param idToken Firebase Auth ID token, or null when signed out.
     * @param appCheck App Check (Play Integrity) token, or null when unavailable.
     */
    data class Tokens(val idToken: String?, val appCheck: String?)

    sealed interface Result<out T> {
        data class Success<T>(val value: T) : Result<T>

        /** The server answered, and said no. [code] is its own machine-readable one. */
        data class Failed(val message: String, val code: String? = null) : Result<Nothing>

        /** No usable credentials, so the request was never sent. */
        data object Unauthenticated : Result<Nothing>

        data class Offline(val message: String) : Result<Nothing>
    }

    suspend fun careerCoach(
        context: CareerCoachContext,
        messages: List<CareerCoachMessage>,
    ): Result<AICareerCoachReply> {
        // The last dozen turns, and never the opening welcome: that message is
        // written on device and personalised with the user's own name, so
        // sending it would put their name into a request that did not need it.
        val conversation = messages
            .takeLast(12)
            .dropWhile { it.role == CareerCoachMessageRole.ASSISTANT }

        return post(
            action = ResumeAIAction.CAREER_COACH,
            payload = json.encodeToJsonElement(
                CoachPayload.serializer(),
                CoachPayload(context, conversation),
            ),
            deserialize = { json.decodeFromJsonElement(AICareerCoachReply.serializer(), it) },
        )
    }

    private suspend fun <T> post(
        action: ResumeAIAction,
        payload: JsonElement,
        deserialize: (JsonElement) -> T,
    ): Result<T> = withContext(Dispatchers.IO) {
        val credentials = tokens()
        if (credentials.idToken == null && credentials.appCheck == null) {
            return@withContext Result.Unauthenticated
        }

        val body = json.encodeToString(
            Envelope.serializer(),
            Envelope(action = action, clientID = clientId, payload = payload),
        )

        runCatching {
            val connection = (URL("${baseUrl.trimEnd('/')}/v1/ai").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 45_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-ResumeStudio-Client", "ResumeStudio-Android/1")
                credentials.appCheck?.let { setRequestProperty("X-Firebase-AppCheck", it) }
                credentials.idToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            }
            connection.outputStream.use { it.write(body.toByteArray()) }

            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            connection.disconnect()
            status to text
        }.fold(
            onSuccess = { (status, text) ->
                if (status in 200..299) {
                    runCatching {
                        val envelope = json.decodeFromString(JsonObject.serializer(), text)
                        val result = envelope["result"]
                            ?: return@runCatching Result.Failed("The server sent no result.")
                        Result.Success(deserialize(result))
                    }.getOrElse { Result.Failed("The server's reply could not be read.") }
                } else {
                    val error = runCatching {
                        json.decodeFromString(ErrorEnvelope.serializer(), text)
                    }.getOrNull()
                    Result.Failed(
                        error?.error ?: "The coach is unavailable right now.",
                        error?.code,
                    )
                }
            },
            onFailure = { Result.Offline(it.message ?: "No connection.") },
        )
    }

    @Serializable
    private data class Envelope(
        val action: ResumeAIAction,
        @SerialName("clientID") val clientID: String,
        val payload: JsonElement,
    )

    @Serializable
    private data class ErrorEnvelope(val error: String = "", val code: String? = null)

    @Serializable
    private data class CoachPayload(
        val context: CareerCoachContext,
        val messages: List<CareerCoachMessage>,
    )

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }
    }
}
