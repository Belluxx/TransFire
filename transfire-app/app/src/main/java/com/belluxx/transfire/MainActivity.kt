package com.belluxx.transfire

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.belluxx.transfire.data.Message
import com.belluxx.transfire.data.MessageAuthor
import com.belluxx.transfire.ui.components.ChatBubble
import com.belluxx.transfire.ui.components.TextDialog
import com.belluxx.transfire.ui.components.TypingIndicator
import com.belluxx.transfire.utils.FirebaseREST
import com.belluxx.transfire.utils.SettingsManager
import com.belluxx.transfire.ui.theme.TransFireTheme
import com.belluxx.transfire.utils.FirebaseError
import com.halilibo.richtext.commonmark.CommonMarkdownParseOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

val mdOptions = CommonMarkdownParseOptions.Default

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val activity = LocalActivity.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

            val settings = SettingsManager(context)
            val hasSettings = runBlocking { settings.hasFirebaseSettings() }

            if (!hasSettings) {
                startActivity(Intent(this, OnboardingActivity::class.java))
                activity?.finish()
            }

            val firebaseUrl = remember { mutableStateOf("") }
            val firebaseApiKey = remember { mutableStateOf("") }
            val firebasePassword = remember { mutableStateOf("") }
            val pollingInterval = remember { mutableLongStateOf(2L) }
            val modelName = remember { mutableStateOf("") }
            val systemPrompt = remember { mutableStateOf("") }
            var firebaseRest by remember { mutableStateOf(FirebaseREST(firebaseApiKey.value, firebaseUrl.value, firebasePassword.value)) }

            val lastError = remember { mutableStateOf("") }
            val lastFbError: MutableState<FirebaseError?> = remember { mutableStateOf(null) }

            LaunchedEffect(lifecycleState) {
                when (lifecycleState) {
                    Lifecycle.State.DESTROYED -> {}
                    Lifecycle.State.INITIALIZED -> {}
                    Lifecycle.State.CREATED -> {}
                    Lifecycle.State.STARTED -> {}
                    Lifecycle.State.RESUMED -> {
                        firebaseUrl.value = settings.getFirebaseUrl()
                        firebaseApiKey.value = settings.getFirebaseApiKey()
                        firebasePassword.value = settings.getFirebasePassword()
                        pollingInterval.longValue = settings.getPollingInterval()
                        modelName.value = settings.getModelName()
                        systemPrompt.value = settings.getSystemPrompt()
                    }
                }
            }

            // State for chat messages
            var chat by remember { mutableStateOf(listOf<Message>()) }
            var isWaitingForResponse by remember { mutableStateOf(false) }

            // Add system prompt
            LaunchedEffect(systemPrompt.value) {
                Log.d("LaunchedEffect", "Adding system prompt: ${systemPrompt.value}")
                chat += Message(MessageAuthor.SYSTEM, systemPrompt.value)
            }

            LaunchedEffect(firebaseApiKey.value, firebaseUrl.value, firebasePassword.value) {
                firebaseRest = FirebaseREST(firebaseApiKey.value, firebaseUrl.value, firebasePassword.value)
                if (firebaseRest.isConfigurationValid()) {
                    while (true) {
                        if (isWaitingForResponse) {
                            firebaseRest.checkResponseAvailable(
                                onMessageFound = { message ->
                                    Log.d(
                                        "MainActivity",
                                        "Message received: ${message.content}"
                                    )
                                    chat = chat + message
                                    isWaitingForResponse = false
                                },
                                onNoMessage = { },
                                onFailure = { error, fbError ->
                                    Log.e("MainActivity", "Cannot read message: $error")
                                    isWaitingForResponse = false
                                }
                            )
                        }
                        delay(pollingInterval.longValue)
                    }
                }
            }

            showFirebaseError(lastFbError, lastError)

            TransFireTheme {
                ChatScreen(
                    context = context,
                    chat = chat,
                    modelName = modelName.value,
                    isWaitingForResponse = isWaitingForResponse,
                    onSendMessage = { userMessage ->
                        // Add user message to chat
                        val userMsg = Message(MessageAuthor.USER, userMessage)
                        Log.d("ChatScreen", "Adding user message: $userMessage")
                        chat = chat + userMsg

                        firebaseRest.sendResponseRequest(
                            chat = chat,
                            model = modelName.value,
                            onFailure = { error, fbError ->
                                lastError.value = error
                                lastFbError.value = fbError
                                isWaitingForResponse = false

                                val lastMsg = chat.last()
                                if (lastMsg.name == MessageAuthor.USER) {
                                    chat = chat - chat.last()
                                }
                            }
                        )
                        isWaitingForResponse = true
                    }
                )
            }
        }
    }
}

@Composable
fun showFirebaseError(fbError: MutableState<FirebaseError?>, error: MutableState<String>) {
    when (fbError.value) {
        FirebaseError.DATABASE_NOT_FOUND -> {
            TextDialog(
                title = "Database not found",
                body = "The provided URL to the Firebase Database is not valid",
                onDismiss = { fbError.value = null; error.value = "" }
            )
        }
        FirebaseError.NO_INTERNET_CONNECTION -> {
            TextDialog(
                title = "No internet connection",
                body = "The message was not successfully sent because you do not have a working internet connection",
                onDismiss = { fbError.value = null; error.value = "" }
            )
        }
        FirebaseError.UNKNOWN -> {
            TextDialog(
                title = "Unknown error",
                body = "The following unknown error occurred: $error",
                onDismiss = { fbError.value = null; error.value = "" }
            )
        }
        FirebaseError.UNAUTHORIZED -> {
            TextDialog(
                title = "Firebase Database API key is not valid",
                body = "The provided API key for the Firebase Database is not valid, the access to the database was denied",
                onDismiss = { fbError.value = null; error.value = "" }
            )
        }
        null -> {}
    }
}

@Composable
fun ChatScreen(
    context: Context,
    chat: List<Message>,
    modelName: String,
    isWaitingForResponse: Boolean,
    onSendMessage: (String) -> Unit
) {
    var userInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive or keyboard opens
    LaunchedEffect(chat.size) {
        if (chat.isNotEmpty()) {
            listState.animateScrollToItem(chat.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .statusBarsPadding()
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TransFire",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = modelName.ifBlank { "No model selected, go to settings" },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.alpha(0.6F)
                        )
                    }

                    Button(
                        onClick = {
                            context.startActivity(Intent(context, SettingsActivity::class.java))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        placeholder = { Text("Type a message...") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        maxLines = 3,
                        enabled = modelName.isNotBlank()
                    )

                    Button(
                        onClick = {
                            if (userInput.isNotBlank()) {
                                onSendMessage(userInput.trim())
                                userInput = ""
                            }
                        },
                        enabled = userInput.isNotBlank() && modelName.isNotBlank() && !isWaitingForResponse
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chat) { message ->
                    ChatBubble(message = message)
                }

                if (isWaitingForResponse) {
                    item {
                        TypingIndicator()
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
