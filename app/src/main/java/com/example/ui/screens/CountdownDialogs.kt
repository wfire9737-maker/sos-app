package com.example.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun SosCountdownDialog(secondsLeft: Int, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Cannot dismiss by clicking outside */ },
        title = { Text("Emergency SOS") },
        text = { Text("Sending emergency alerts in $secondsLeft seconds. Press cancel to abort.") },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel SOS")
            }
        }
    )
}

@Composable
fun FallCountdownDialog(secondsLeft: Int, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Cannot dismiss */ },
        title = { Text("Fall Detected") },
        text = { Text("A hard fall was detected. Calling for help in $secondsLeft seconds.") },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("I'm Okay (Cancel)")
            }
        }
    )
}
