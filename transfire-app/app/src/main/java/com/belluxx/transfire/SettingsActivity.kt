package com.belluxx.transfire

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.belluxx.transfire.ui.components.DatabaseConfigFields
import com.belluxx.transfire.ui.theme.TransFireTheme
import com.belluxx.transfire.utils.SettingsManager
import kotlinx.coroutines.runBlocking

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TransFireTheme {
                SettingsScreen(
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val scrollState = rememberScrollState()

    // Settings state
    var firebaseUrl by remember { mutableStateOf("") }
    var firebasePassword by remember { mutableStateOf("") }
    var firebaseApiKey by remember { mutableStateOf("") }
    var pollInterval by remember { mutableLongStateOf(0L) }
    var modelName by remember { mutableStateOf("") }
    var systemPrompt by remember { mutableStateOf("") }

    // Load current settings
    LaunchedEffect(Unit) {
        firebaseUrl = settingsManager.getFirebaseUrl()
        firebasePassword = settingsManager.getFirebasePassword()
        firebaseApiKey = settingsManager.getFirebaseApiKey()
        pollInterval = settingsManager.getPollingInterval()
        modelName = settingsManager.getModelName()
        systemPrompt = settingsManager.getSystemPrompt()
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackPressed) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }

                        Text(
                            text = "Settings",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
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
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            runBlocking {
                                try {
                                    val intervalLong = pollInterval.toLong()
                                    settingsManager.saveFirebaseSettings(firebaseUrl.trim(), firebasePassword.trim(), firebaseApiKey.trim())
                                    settingsManager.saveMiscSettings(intervalLong, modelName.trim(), systemPrompt.trim())
                                    Toast.makeText(context, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error saving settings: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = firebaseUrl.isNotBlank() && firebasePassword.isNotBlank() && firebaseApiKey.isNotBlank(),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Firebase Settings Section
            SettingsSection(title = "Firebase Configuration", icon = Icons.Default.Storage) {
                DatabaseConfigFields(
                    firebaseUrl = firebaseUrl,
                    onFirebaseUrlChange = { firebaseUrl = it },
                    firebasePassword = firebasePassword,
                    onFirebasePasswordChange = { firebasePassword = it },
                    firebaseApiKey = firebaseApiKey,
                    onFirebaseApiKeyChange = { firebaseApiKey = it }
                )
            }

            // App Settings Section
            SettingsSection(title = "Chat Configuration", icon = Icons.AutoMirrored.Default.Chat) {
                Text("Polling interval (seconds)")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = pollInterval.toFloat(),
                        onValueChange = { newValue ->
                            pollInterval = newValue.toLong()
                        },
                        onValueChangeFinished = {},
                        valueRange = 1000f..5000f,
                        steps = 3,
                        modifier = Modifier.weight(1F),
                    )
                    Text(modifier = Modifier.width(50.dp), text = (pollInterval / 1000).toString(), textAlign = TextAlign.Center)
                }


                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("Model Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("Must match exactly Ollama name or LMStudio identifier") }
                )

                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("System prompt") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 5,
                    supportingText = { Text("Instruct the model on how to communicate") }
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}
