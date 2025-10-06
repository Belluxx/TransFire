package com.belluxx.transfire.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.belluxx.transfire.ui.theme.TransFireTheme

@Composable
fun TextDialog(
    title: String,
    body: String,
    buttonText: String = "OK",
    onDismiss: () -> Unit
) {
    TransFireTheme {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = title)
            },
            text = {
                Text(text = body)
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(buttonText)
                }
            }
        )
    }
}