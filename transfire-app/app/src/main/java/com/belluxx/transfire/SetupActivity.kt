package com.belluxx.transfire

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.belluxx.transfire.ui.components.DatabaseConfigFields
import com.belluxx.transfire.ui.theme.TransFireTheme
import com.belluxx.transfire.utils.SettingsManager
import com.belluxx.transfire.utils.normalizeUrl
import kotlinx.coroutines.launch


class SetupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TransFireTheme {
                SetupScreen()
            }
        }
    }
}

@Composable
fun SetupScreen() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()

    // Initialize SettingsManager
    val settingsManager = remember(context) { SettingsManager(context) }

    var firebaseUrl by remember { mutableStateOf("") }
    var firebasePassword by remember { mutableStateOf("") }
    var firebaseApiKey by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cable,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configure connection",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            DatabaseConfigFields(
                firebaseUrl = firebaseUrl,
                onFirebaseUrlChange = { firebaseUrl = it },
                firebasePassword = firebasePassword,
                onFirebasePasswordChange = { firebasePassword = it },
                firebaseApiKey = firebaseApiKey,
                onFirebaseApiKeyChange = { firebaseApiKey = it }
            )

            Spacer(Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    scope.launch {
                        val normalizedFirebaseUrl = normalizeUrl(firebaseUrl)
                        settingsManager.saveFirebaseSettings(normalizedFirebaseUrl, firebasePassword, firebaseApiKey)
                        showSuccess = true
                    }
                },
                enabled = firebaseUrl.isNotBlank() &&
                        firebasePassword.isNotBlank() &&
                        firebaseApiKey.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Configuration")
            }

            // Success message with smooth slide animation
            AnimatedVisibility(
                visible = showSuccess,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(durationMillis = 300)
                ),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(durationMillis = 200)
                )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Configuration saved!",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1F)
                        )
                        Button(
                            onClick = {
                                activity?.startActivity(Intent(activity, MainActivity::class.java))
                                activity?.finish()
                            }
                        ) {
                            Spacer(Modifier.width(8.dp))
                            Text("Next")
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowRight,
                                contentDescription = "Close setup screen"
                            )
                        }
                    }
                }
            }
        }
    }
}

