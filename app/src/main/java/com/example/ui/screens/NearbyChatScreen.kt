package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ble.nearby.NearbyConnectionState
import com.example.model.MessageDeliveryState
import com.example.model.NearbyChatMessage
import com.example.ui.NearbyChatViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyChatScreen(
    macAddress: String,
    deviceName: String,
    onNavigateBack: () -> Unit,
    viewModel: NearbyChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val connectionStateFlow = remember(viewModel, macAddress) {
        viewModel.getConnectionState(macAddress)
    }
    val connectionState by connectionStateFlow.collectAsState()
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    var inputText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf<String?>(null) }
    
    // Auto-scroll logic
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            // If near bottom, auto-scroll to the newest
            if (lastVisibleItemIndex >= messages.size - 3) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = deviceName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = when (connectionState) {
                                NearbyConnectionState.CONNECTED -> "Connected"
                                NearbyConnectionState.REQUESTING -> "Connecting..."
                                NearbyConnectionState.DISCONNECTED -> "Disconnected"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (connectionState == NearbyConnectionState.CONNECTED) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Error Message Banner
            if (showError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = showError!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { showError = null }) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // Connection Warning Banner
            if (connectionState == NearbyConnectionState.DISCONNECTED) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Connect to a Nearby device to start chatting.",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Message List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No messages yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Messages sent through Nearby BLE stay between the connected devices.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages, key = { it.messageId }) { msg ->
                            ChatMessageBubble(msg)
                        }
                    }
                }
            }

            // Input Area
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isConnected = connectionState == NearbyConnectionState.CONNECTED
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { 
                            inputText = it
                            val byteSize = it.toByteArray(Charsets.UTF_8).size
                            if (byteSize > 400) {
                                // show lightweight warning if near limit
                                showError = if (byteSize > 512) "Message is too large to send." else null
                            } else {
                                showError = null
                            }
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        enabled = isConnected,
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    val canSend = isConnected && inputText.isNotBlank() && (showError != "Message is too large to send.")
                    FilledIconButton(
                        onClick = {
                            val textToSend = inputText.trim()
                            if (textToSend.isNotEmpty()) {
                                // Delegate to repository for exact failure logic
                                val success = viewModel.sendText(macAddress, textToSend)
                                if (success) {
                                    inputText = ""
                                    showError = null
                                    coroutineScope.launch {
                                        if (messages.isNotEmpty()) {
                                            listState.animateScrollToItem(messages.size)
                                        }
                                    }
                                } else {
                                    showError = "Message could not be sent. Check connection or message size."
                                }
                            }
                        },
                        enabled = canSend,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: NearbyChatMessage) {
    val formatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (message.isOutgoing) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.8f) // max width 80%
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isOutgoing) 16.dp else 4.dp,
                    bottomEnd = if (message.isOutgoing) 4.dp else 16.dp
                ),
                color = if (message.isOutgoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (message.isOutgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
            ) {
                Text(
                    text = formatter.format(Date(message.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (message.isOutgoing) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (message.deliveryState) {
                            MessageDeliveryState.SENDING -> "•"
                            MessageDeliveryState.SENT -> "✓"
                            MessageDeliveryState.DELIVERED -> "✓✓"
                            MessageDeliveryState.FAILED -> "!"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (message.deliveryState == MessageDeliveryState.FAILED) 
                            MaterialTheme.colorScheme.error 
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
