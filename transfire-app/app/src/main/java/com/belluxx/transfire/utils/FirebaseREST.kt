package com.belluxx.transfire.utils

import android.util.Log
import androidx.compose.runtime.Composable
import com.belluxx.transfire.data.Message
import com.belluxx.transfire.data.MessageAuthor
import com.belluxx.transfire.data.chatToJSON
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import java.lang.Exception
import org.json.JSONObject
import kotlinx.serialization.json.Json

enum class FirebaseError {
    DATABASE_NOT_FOUND,
    NO_INTERNET_CONNECTION,
    UNAUTHORIZED,
    WRONG_ENCRYPTION_KEY,
    UNKNOWN
}

fun codeToError(code: Int): FirebaseError {
    return when (code) {
        401 -> FirebaseError.UNAUTHORIZED
        404 -> FirebaseError.DATABASE_NOT_FOUND
        0 -> FirebaseError.NO_INTERNET_CONNECTION
        else -> FirebaseError.UNKNOWN
    }
}

class FirebaseREST(val apiKey: String, val baseUrl: String, private val password: String) {
    private val client = OkHttpClient()
    private val KEY_CLIENT_MAILBOX = "mailbox_client"
    private val KEY_SERVER_MAILBOX = "mailbox_server"
    private val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()

    fun sendResponseRequest(chat: List<Message>, model: String, onFailure: (error: String, fbError: FirebaseError) -> Unit) {
        val path = KEY_CLIENT_MAILBOX

        val payload = chatToJSON(chat, model)
        val encryptedPayload = encryptPayload(payload)!!

        // Delete eventual old responses
        deleteAsync(KEY_SERVER_MAILBOX,
            onSuccess = { Log.d("checkResponseAvailable", "Server mailbox cleared pre-emptively") },
            onFailure = { error, code ->
                Log.e("checkResponseAvailable", "Unable to empty server mailbox: $error")
                onFailure(error, codeToError(code))
            }
        )

        // Delete eventual old sent messages
        deleteAsync(KEY_CLIENT_MAILBOX,
            onSuccess = {
                Log.d("checkResponseAvailable", "Client mailbox cleared pre-emptively")

                // Send new message
                postAsync(path, encryptedPayload,
                    onSuccess = { Log.d("sendResponseRequest", "User message sent successfully") },
                    onFailure = { error, code ->
                        Log.e("sendResponseRequest", "Failed to send user message: $error")
                        onFailure(error, codeToError(code))
                    }
                )
            },
            onFailure = { error, code ->
                Log.e("checkResponseAvailable", "Unable to empty client mailbox: $error")
                onFailure(error, codeToError(code))
            }
        )
    }

    fun checkResponseAvailable(onMessageFound: (message: Message) -> Unit, onNoMessage: () -> Unit, onFailure: (error: String, fbError: FirebaseError) -> Unit) {
        val path = KEY_SERVER_MAILBOX

        getAsync(path,
            onSuccess = { data ->
                if (data != "null") {
                    var decryptedData: String
                    try {
                        decryptedData = decryptResponse(extractPushedString(data))!!
                    } catch (e: kotlin.Exception) {
                        onFailure(e.toString(), FirebaseError.WRONG_ENCRYPTION_KEY)
                        return@getAsync
                    }

                    val serverResponse = JSONObject(decryptedData)

                    val choices = serverResponse.getJSONArray("choices")
                    val firstChoice = choices.getJSONObject(0)
                    val message = firstChoice.getJSONObject("message")
                    val content = message.getString("content")

                    // Delete the message after reading
                    deleteAsync(KEY_SERVER_MAILBOX,
                        onSuccess = { Log.d("checkResponseAvailable", "Server mailbox cleared") },
                        onFailure = { error, code ->
                            Log.e("checkResponseAvailable", "Unable to empty server mailbox: $error")
                            onFailure(error, codeToError(code))
                        }
                    )

                    onMessageFound(Message(MessageAuthor.ASSISTANT, content))
                } else { onNoMessage() }
            },
            onFailure = { error, code ->
                Log.e("checkResponseAvailable", "Failed to read assistant message: $error")
                onFailure(error, codeToError(code))
            }
        )
    }

    fun isConfigurationValid(): Boolean {
        return apiKey.isNotBlank() && baseUrl.isNotBlank() && password.isNotBlank()
    }

    private fun getAsync(path: String, onSuccess: (data: String) -> Unit, onFailure: (error: String, code: Int) -> Unit) {
        Log.d("getAsync", "GET: $path")

        try {
            val request = Request.Builder().url(getUrl(path)).build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    onFailure(e.toString(), 0)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            onFailure("Unexpected code $response", response.code)
                            return
                        }

                        val result = response.body.string()
                        onSuccess(result)

                        Log.d("getAsync", "Got: $result")
                    }
                }
            })
        } catch (e: Exception) {
            onFailure(e.toString(), 0)
        }

    }

    private fun postAsync(path: String, payload: String, onFailure: (error: String, code: Int) -> Unit, onSuccess: (data: String) -> Unit) {
        Log.d("postAsync", "POST: $path")

        val request = Request.Builder()
            .url(getUrl(path))
            .post(payload.toRequestBody(MEDIA_TYPE_JSON))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                onFailure(e.message!!, 0)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        onFailure("Unexpected code $response", response.code)
                        return
                    }

                    val result = response.body.string()
                    onSuccess(result)

                    Log.d("getAsync", "Posted: $payload")
                }
            }
        })
    }

    private fun deleteAsync(path: String, onSuccess: () -> Unit, onFailure: (error: String, code: Int) -> Unit) {
        Log.d("deleteAsync", "DELETE: $path")

        try {
            val request = Request.Builder().url(getUrl(path)).delete().build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    onFailure(e.toString(), 0)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            onFailure("Unexpected code $response", response.code)
                            return
                        }

                        onSuccess()
                        
                        Log.d("deleteAsync", "Deleted: $path")
                    }
                }
            })
        } catch (e: Exception) {
            onFailure(e.toString(), 0)
        }
    }

    private fun encryptPayload(payload: String): String? {
        return try {
            val (ciphertext, iv) = Crypt.encryptString(password, payload)
            val encryptedData = JSONObject().apply {
                put("data", ciphertext)
                put("iv", iv)
            }
            Json.encodeToString(encryptedData.toString())
        } catch (e: Exception) {
            Log.e("encryptPayload", "Failed to encrypt payload: ${e.message}")
            return null
        }
    }

    private fun decryptResponse(response: String): String? {
        return try {
            if (response == "null") {
                return "null"
            }

            val responseJson = JSONObject(response)
            val ciphertext = responseJson.getString("data")
            val iv = responseJson.getString("iv")
            Crypt.decryptString(password, ciphertext, iv)
        } catch (e: Exception) {
            Log.e("decryptResponse", "Failed to decrypt response: ${e.message}")
            return null
        }
    }

    private fun getUrl(path: String): String {
        return "$baseUrl/$path.json?auth=${apiKey}"
    }

    private fun extractPushedString(data: String): String {
        val jsonData = JSONObject(data)
        val pushKey = jsonData.keys().next()
        val pushValue: String = jsonData.get(pushKey) as String

        return pushValue
    }
}