package com.example.network

import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.roundToInt

data class FlashSolveApiResult(
    val answerBracketed: String,
    val latencyMs: Long,
    val isSuccess: Boolean,
    val rawError: String? = null
)

object GeminiFlashClient {

    // Using the supported alias for Gemini Flash per gemini-api skill
    private const val MODEL_NAME = "gemini-flash-latest"
    private const val BASE_ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Dedicated OkHttpClient with warm ConnectionPool and 60s timeout per skill guidelines
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Compresses and downscales the captured screen Bitmap on Dispatchers.Default
     * so base64 encoding takes <15ms and network upload is ultra-fast (<1.5s total).
     */
    suspend fun encodeScreenBitmapFast(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        val maxDimension = 640
        val width = bitmap.width.coerceAtLeast(1)
        val height = bitmap.height.coerceAtLeast(1)
        val largestSide = max(width, height)

        val scaledBitmap = if (largestSide > maxDimension) {
            val ratio = maxDimension.toFloat() / largestSide.toFloat()
            val targetW = (width * ratio).roundToInt().coerceAtLeast(1)
            val targetH = (height * ratio).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream(32 * 1024)
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 65, outputStream)
        if (scaledBitmap !== bitmap) {
            scaledBitmap.recycle()
        }
        Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Sends the captured screen content via an asynchronous OkHttp POST request
     * off the main thread using Kotlin Coroutines to the Gemini Flash REST API.
     * Enforces minimal immediate text output (e.g., "[B]") for sub-1.5s latency.
     */
    suspend fun solveScreenAsync(
        screenBitmap: Bitmap,
        apiKey: String
    ): FlashSolveApiResult = withContext(Dispatchers.IO) {
        val startElapsed = SystemClock.elapsedRealtime()
        val trimmedKey = apiKey.trim()

        if (trimmedKey.isEmpty() || trimmedKey == "MY_GEMINI_API_KEY" || trimmedKey == "null") {
            return@withContext FlashSolveApiResult(
                answerBracketed = "[KEY?]",
                latencyMs = SystemClock.elapsedRealtime() - startElapsed,
                isSuccess = false,
                rawError = "Missing Gemini API Key. Enter key in the secure field or AI Studio Secrets."
            )
        }

        val base64Jpeg = try {
            encodeScreenBitmapFast(screenBitmap)
        } catch (e: Exception) {
            return@withContext FlashSolveApiResult(
                answerBracketed = "[ERR]",
                latencyMs = SystemClock.elapsedRealtime() - startElapsed,
                isSuccess = false,
                rawError = "Bitmap encoding error: ${e.message}"
            )
        }

        val payloadJson = buildUltraFastPayloadJson(base64Jpeg)
        val request = Request.Builder()
            .url("$BASE_ENDPOINT?key=$trimmedKey")
            .post(payloadJson.toRequestBody(jsonMediaType))
            .header("Accept", "application/json")
            .build()

        try {
            val responseBodyString = executeAsyncCall(request)
            val elapsed = SystemClock.elapsedRealtime() - startElapsed
            val parsedAnswer = parseMinimalBracketedAnswer(responseBodyString)
            FlashSolveApiResult(
                answerBracketed = parsedAnswer,
                latencyMs = elapsed,
                isSuccess = true
            )
        } catch (e: ApiHttpException) {
            val elapsed = SystemClock.elapsedRealtime() - startElapsed
            FlashSolveApiResult(
                answerBracketed = "[ERR]",
                latencyMs = elapsed,
                isSuccess = false,
                rawError = "HTTP ${e.code}: ${e.details}"
            )
        } catch (e: Exception) {
            val elapsed = SystemClock.elapsedRealtime() - startElapsed
            FlashSolveApiResult(
                answerBracketed = "[ERR]",
                latencyMs = elapsed,
                isSuccess = false,
                rawError = e.localizedMessage ?: "Network error"
            )
        }
    }

    /**
     * Constructs the JSON payload enforcing minimal token generation and zero explanation
     * so Gemini Flash responds in < 1.5 seconds with immediate bracketed text like "[B]".
     */
    internal fun buildUltraFastPayloadJson(base64Jpeg: String): String {
        val systemInstruction = JSONObject().apply {
            put(
                "parts",
                JSONArray().put(
                    JSONObject().put(
                        "text",
                        "You are FlashSolve AI, an ultra-fast visual question solver. " +
                            "Read the question on the screen image and output ONLY the final answer inside square brackets, " +
                            "for example: [A], [B], [C], [D], [True], or [42]. " +
                            "Do NOT write any explanation, steps, or extra words."
                    )
                )
            )
        }

        val userParts = JSONArray().apply {
            put(
                JSONObject().put(
                    "inlineData",
                    JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Jpeg)
                    }
                )
            )
            put(
                JSONObject().put(
                    "text",
                    "Solve the visible question immediately. Output ONLY the bracketed answer (e.g. [B]):"
                )
            )
        }

        val contents = JSONArray().put(
            JSONObject().apply {
                put("role", "user")
                put("parts", userParts)
            }
        )

        val generationConfig = JSONObject().apply {
            put("temperature", 0.0)
            put("topP", 0.1)
            put("topK", 1)
            put("maxOutputTokens", 12)
        }

        return JSONObject().apply {
            put("systemInstruction", systemInstruction)
            put("contents", contents)
            put("generationConfig", generationConfig)
        }.toString()
    }

    /**
     * Executes OkHttp Call asynchronously using enqueue() bridged to Kotlin Coroutines.
     */
    private suspend fun executeAsyncCall(request: Request): String =
        suspendCancellableCoroutine { continuation ->
            val call = okHttpClient.newCall(request)
            continuation.invokeOnCancellation {
                call.cancel()
            }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWith(Result.failure(e))
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { resp ->
                        val bodyStr = resp.body?.string().orEmpty()
                        if (!resp.isSuccessful) {
                            val cleanErr = extractApiErrorMessage(bodyStr).ifEmpty { resp.message }
                            if (continuation.isActive) {
                                continuation.resumeWith(
                                    Result.failure(ApiHttpException(resp.code, cleanErr))
                                )
                            }
                            return
                        }
                        if (continuation.isActive) {
                            continuation.resume(bodyStr)
                        }
                    }
                }
            })
        }

    internal fun parseMinimalBracketedAnswer(rawJson: String): String {
        return try {
            val root = JSONObject(rawJson)
            val candidates = root.optJSONArray("candidates") ?: return "[?]"
            if (candidates.length() == 0) return "[?]"
            val firstCandidate = candidates.optJSONObject(0) ?: return "[?]"
            val content = firstCandidate.optJSONObject("content") ?: return "[?]"
            val parts = content.optJSONArray("parts") ?: return "[?]"
            if (parts.length() == 0) return "[?]"
            val rawText = parts.optJSONObject(0)?.optString("text", "")?.trim().orEmpty()
            formatAsBracketedToken(rawText)
        } catch (e: Exception) {
            "[?]"
        }
    }

    internal fun formatAsBracketedToken(rawText: String): String {
        if (rawText.isBlank()) return "[?]"
        // Look for an existing [X] pattern first
        val bracketRegex = Regex("""\[([^\[\]\n]{1,18})]""")
        val match = bracketRegex.find(rawText)
        if (match != null) {
            return "[${match.groupValues[1].trim()}]"
        }
        // Otherwise strip markdown/punctuation and wrap concise token in brackets
        val cleaned = rawText
            .replace("*", "")
            .replace("`", "")
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.take(14)
            ?: "?"
        return "[$cleaned]"
    }

    private fun extractApiErrorMessage(errorJson: String): String {
        return try {
            val obj = JSONObject(errorJson)
            obj.optJSONObject("error")?.optString("message", "").orEmpty()
        } catch (_: Exception) {
            errorJson.take(120)
        }
    }

    private class ApiHttpException(val code: Int, val details: String) :
        IOException("HTTP $code: $details")
}
