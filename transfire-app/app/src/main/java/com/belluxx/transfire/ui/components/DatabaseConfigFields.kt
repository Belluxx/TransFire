package com.belluxx.transfire.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.belluxx.transfire.utils.normalizeUrl

@Composable
fun DatabaseConfigFields(
    firebaseUrl: String,
    onFirebaseUrlChange: (String) -> Unit,
    firebasePassword: String,
    onFirebasePasswordChange: (String) -> Unit,
    firebaseApiKey: String,
    onFirebaseApiKeyChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var urlError by remember { mutableStateOf<String?>(null) }

    // Dialog states
    var showUrlInfoDialog by remember { mutableStateOf(false) }
    var showPasswordInfoDialog by remember { mutableStateOf(false) }
    var showApiKeyInfoDialog by remember { mutableStateOf(false) }

    // URL validation function
    fun validateFirebaseUrl(url: String): String? {
        if (url.isBlank()) return null

        val normalizedUrl = normalizeUrl(url)

        val domain = try {
            normalizedUrl.removePrefix("https://").removePrefix("http://").removeSuffix("/")
        } catch (e: Exception) {
            return "Invalid URL format"
        }

        val validPatterns = listOf(
            // us-central1
            Regex("^[a-zA-Z0-9-]+\\.firebaseio\\.com$"),
            // europe-west1
            Regex("^[a-zA-Z0-9-]+\\.europe-west1\\.firebasedatabase\\.app$"),
            // asia-southeast1
            Regex("^[a-zA-Z0-9-]+\\.asia-southeast1\\.firebasedatabase\\.app$")
        )

        val isValid = validPatterns.any { pattern -> pattern.matches(domain) }

        return if (isValid) null else "Invalid Firebase Realtime Database URL format"
    }

    // Check URL validity whenever it changes
    LaunchedEffect(firebaseUrl) {
        urlError = validateFirebaseUrl(firebaseUrl)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Firebase URL Input
        OutlinedTextField(
            value = firebaseUrl,
            onValueChange = { newUrl ->
                onFirebaseUrlChange(newUrl)
            },
            label = { Text("Firebase Database URL") },
            trailingIcon = {
                IconButton(onClick = { showUrlInfoDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "How to get Firebase URL",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (urlError != null) 4.dp else 0.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            isError = urlError != null,
            supportingText = urlError?.let { error ->
                {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )

        // Show valid URL formats as helper text
        if (urlError != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Valid URL formats:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• your-project.firebaseio.com\n" +
                                "• your-project.europe-west1.firebasedatabase.app\n" +
                                "• your-project.asia-southeast1.firebasedatabase.app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // API Key Input
        OutlinedTextField(
            value = firebaseApiKey,
            onValueChange = { onFirebaseApiKeyChange(it) },
            label = { Text("Firebase Database API key") },
            visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                Row {
                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                        Icon(
                            imageVector = if (apiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (apiKeyVisible) "Hide API key" else "Show API key"
                        )
                    }
                    IconButton(onClick = { showApiKeyInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "How to get Firebase API key",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Password Input
        OutlinedTextField(
            value = firebasePassword,
            onValueChange = { onFirebasePasswordChange(it) },
            label = { Text("Encryption password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                Row {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                    IconButton(onClick = { showPasswordInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About encryption password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }

    // Info Dialogs
    InfoDialog(
        title = "Database URL",
        icon = {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "This is the URL used to reach the Firebase Realtime Database, you will need to create a firebase project to obtain one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                StepItem("1", "Go to Firebase Console")
                StepItem("2", "Select or create your project")
                StepItem("3", "Navigate to Build → Realtime Database")
                StepItem("4", "Create database (locked mode)")
                StepItem("5", "Copy the database URL")
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "The URL should be in one of the following formats:\n" +
                            "• <proj-id>.firebaseio.com\n" +
                            "• <proj-id>.<loc>.firebasedatabase.app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        showDialog = showUrlInfoDialog,
        onDismiss = { showUrlInfoDialog = false }
    )

    InfoDialog(
        title = "Encryption password",
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = buildAnnotatedString {
                        append("This password encrypts all the communications between your app and Firebase with AES for maximum privacy.\n\nYou can freely choose any password as long as ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)) { append("you use the same exact one between the app and the server") }
                        append(".")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        showDialog = showPasswordInfoDialog,
        onDismiss = { showPasswordInfoDialog = false }
    )

    InfoDialog(
        title = "API Key",
        icon = {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
        },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "This is a database secret key used to gain full access to it. You will need to go to your Firebase project and paste it here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                StepItem("1", "Click settings icon in Firebase project")
                StepItem("2", "Select 'Project Settings'")
                StepItem("3", "Go to 'Service accounts' tab")
                StepItem("4", "Click on 'Database secrets'")
                StepItem("5", "Add secret if none exists")
                StepItem("6", "Click 'Show' and copy the key")
            }
        },
        showDialog = showApiKeyInfoDialog,
        onDismiss = { showApiKeyInfoDialog = false }
    )
}

@Composable
fun InfoDialog(
    title: String,
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit,
    showDialog: Boolean,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = icon,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        content()
                    }
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)
                ) {
                    Text("Got it")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.extraLarge
        )
    }
}

@Composable
fun StepItem(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
