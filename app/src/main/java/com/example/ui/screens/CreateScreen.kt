package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralSecondary
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.TealAccent
import com.example.util.Localization
import com.example.viewmodel.FrenovaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    viewModel: FrenovaViewModel,
    onFinish: () -> Unit
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isDataSaver by viewModel.isDataSaver.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) } // 0: Post, 1: Reel, 2: Room

    // Post / Reel Form
    var contentText by remember { mutableStateOf("") }
    var mediaUrlText by remember { mutableStateOf("") }
    var compressionPreset by remember { mutableStateOf("480p (Lightweight Data Saver)") }

    // Room Form
    var roomTitle by remember { mutableStateOf("") }
    var roomDescription by remember { mutableStateOf("") }
    var roomType by remember { mutableStateOf("audio") } // "text", "audio", "video"
    var isRoomPrivate by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Create & Share",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Share uplifting moments or start a friendship room",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(Localization.get("create_post", currentLanguage), fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Feed, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text(Localization.get("create_reel", currentLanguage), fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.SlowMotionVideo, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text(Localization.get("create_room", currentLanguage), fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        when (selectedTabIndex) {
            0, 1 -> {
                // Post or Reel
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (selectedTabIndex == 0) "Write Community Post" else "Create Short Reel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = contentText,
                                onValueChange = { contentText = it },
                                label = { Text(Localization.get("what_on_mind", currentLanguage)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("create_content_input"),
                                minLines = 4,
                                maxLines = 8
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = mediaUrlText,
                                onValueChange = { mediaUrlText = it },
                                label = { Text(if (selectedTabIndex == 0) "Image or Media URL (optional)" else "Video Stream URL / Preview") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        if (selectedTabIndex == 0) Icons.Default.Image else Icons.Default.Videocam,
                                        contentDescription = null
                                    )
                                }
                            )

                            if (selectedTabIndex == 1 || isDataSaver) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = IndigoPrimary.copy(alpha = 0.08f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Compress, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Low-Data Mobile Compression", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IndigoPrimary)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Target profile: $compressionPreset. Optimized for 3G/4G low-end Android devices with smooth caching.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (contentText.trim().isNotEmpty()) {
                                        viewModel.createPost(
                                            content = contentText.trim(),
                                            mediaUrl = mediaUrlText.trim(),
                                            mediaType = if (selectedTabIndex == 1) "video" else if (mediaUrlText.isNotEmpty()) "image" else "text",
                                            isReel = selectedTabIndex == 1
                                        )
                                        contentText = ""
                                        mediaUrlText = ""
                                        onFinish()
                                    }
                                },
                                enabled = contentText.trim().isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("publish_post_button"),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Publish to Community", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            2 -> {
                // Room Form
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Setup New Room", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = roomTitle,
                                onValueChange = { roomTitle = it },
                                label = { Text("Room Topic / Title (e.g. Marathi Tech Chat, Book Lovers)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("room_title_input"),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = roomDescription,
                                onValueChange = { roomDescription = it },
                                label = { Text("Short description") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Room Format", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Triple("audio", Icons.Default.Mic, "Audio"),
                                    Triple("video", Icons.Default.Videocam, "Video"),
                                    Triple("text", Icons.Default.Chat, "Text")
                                ).forEach { (type, icon, label) ->
                                    val isSelected = roomType == type
                                    OutlinedButton(
                                        onClick = { roomType = type },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                        )
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Private Room", fontWeight = FontWeight.Bold)
                                    Text(
                                        "Only invited accepted friends can join",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isRoomPrivate,
                                    onCheckedChange = { isRoomPrivate = it }
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    if (roomTitle.trim().isNotEmpty()) {
                                        viewModel.createRoom(
                                            title = roomTitle.trim(),
                                            description = roomDescription.trim(),
                                            roomType = roomType,
                                            isPrivate = isRoomPrivate
                                        )
                                        roomTitle = ""
                                        roomDescription = ""
                                        onFinish()
                                    }
                                },
                                enabled = roomTitle.trim().isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("publish_room_button"),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Launch Room", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
