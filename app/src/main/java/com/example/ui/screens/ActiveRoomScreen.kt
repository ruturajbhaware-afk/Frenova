package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Room
import com.example.ui.components.ReportDialog
import com.example.ui.theme.CoralSecondary
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.TealAccent
import com.example.util.Localization
import com.example.viewmodel.FrenovaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveRoomScreen(
    room: Room,
    viewModel: FrenovaViewModel,
    onLeave: () -> Unit
) {
    BackHandler { onLeave() }

    val currentLanguage by viewModel.currentLanguage.collectAsState()
    var isMicMuted by remember { mutableStateOf(false) }
    var isCameraOn by remember { mutableStateOf(room.roomType == "video") }
    var isHandRaised by remember { mutableStateOf(false) }
    var reportDialogVisible by remember { mutableStateOf(false) }

    var roomMessages by remember {
        mutableStateOf(
            listOf(
                "System: Welcome to ${room.title}!",
                "${room.hostName} (Host): Welcome everyone! Let's have a great conversation."
            )
        )
    }
    var chatInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(room.title, fontWeight = FontWeight.Bold, maxLines = 1)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${room.roomType.capitalize()} Room • Host: ${room.hostName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onLeave) {
                        Icon(Icons.Default.Close, contentDescription = "Leave Room")
                    }
                },
                actions = {
                    IconButton(onClick = { reportDialogVisible = true }) {
                        Icon(Icons.Default.Report, contentDescription = "Report Room", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mic toggle
                    IconButton(
                        onClick = {
                            isMicMuted = !isMicMuted
                            viewModel.showMessage(if (isMicMuted) "Microphone muted" else "Microphone unmuted")
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isMicMuted) MaterialTheme.colorScheme.surfaceVariant else IndigoPrimary)
                    ) {
                        Icon(
                            if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mic",
                            tint = if (isMicMuted) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                        )
                    }

                    // Video toggle
                    if (room.roomType == "video") {
                        IconButton(
                            onClick = {
                                isCameraOn = !isCameraOn
                                viewModel.showMessage(if (isCameraOn) "Camera enabled" else "Camera disabled")
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isCameraOn) TealAccent else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = "Camera",
                                tint = if (isCameraOn) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Raise hand toggle
                    IconButton(
                        onClick = {
                            isHandRaised = !isHandRaised
                            viewModel.showMessage(if (isHandRaised) "Hand raised to speak!" else "Hand lowered")
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isHandRaised) CoralSecondary else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            Icons.Default.FrontHand,
                            contentDescription = "Hand",
                            tint = if (isHandRaised) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Leave Room
                    Button(
                        onClick = onLeave,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Leave", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Stage: Participants Grid
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Host Avatar with live ripple aura
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(IndigoPrimary, CoralSecondary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (room.roomType == "video") Icons.Default.Videocam else Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${room.hostName} (Speaking)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (room.roomType == "video") "Live Video Stream active" else "Live Audio Stream active",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Room Live Chat Box
            Text("Room Chat", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(roomMessages) { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { Text("Send a message to room...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (chatInput.trim().isNotEmpty()) {
                                    val profile = viewModel.currentUserProfile.value
                                    val sender = profile?.displayName?.ifEmpty { profile.username } ?: "Member"
                                    roomMessages = roomMessages + "$sender: ${chatInput.trim()}"
                                    chatInput = ""
                                }
                            },
                            enabled = chatInput.trim().isNotEmpty()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    if (reportDialogVisible) {
        ReportDialog(
            currentLanguage = currentLanguage,
            targetType = "room",
            targetId = room.id,
            onDismiss = { reportDialogVisible = false },
            onSubmitReport = { reason, details ->
                viewModel.submitReport(
                    targetType = "room",
                    targetId = room.id,
                    reason = reason,
                    details = details
                )
            }
        )
    }
}
