package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AuditLog
import com.example.model.ChatMessage
import com.example.model.Report
import com.example.ui.theme.CoralSecondary
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.viewmodel.FrenovaViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperModeScreen(
    viewModel: FrenovaViewModel,
    onExit: () -> Unit
) {
    BackHandler { onExit() }

    val isDevAccount = viewModel.repository.isDeveloperAccount()
    val isPinVerified by viewModel.isDevPinVerified.collectAsState()

    // Defense-in-depth: if not developer email bhawareraj852@gmail.com, completely block
    if (!isDevAccount) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(54.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Access Denied", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Developer Mode is restricted to authorized server credentials.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onExit) { Text("Go Back") }
            }
        }
        return
    }

    if (!isPinVerified) {
        // Step 1: PIN Verification Screen
        DeveloperPinVerificationScreen(
            viewModel = viewModel,
            onVerified = {},
            onCancel = onExit
        )
    } else {
        // Step 2: Developer Console Dashboard
        DeveloperDashboard(
            viewModel = viewModel,
            onExit = {
                viewModel.exitDeveloperMode()
                onExit()
            }
        )
    }
}

@Composable
fun DeveloperPinVerificationScreen(
    viewModel: FrenovaViewModel,
    onVerified: () -> Unit,
    onCancel: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(CoralSecondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = CoralSecondary, modifier = Modifier.size(32.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Developer PIN Verification",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Authorized Account: bhawareraj852@gmail.com",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Text(
                        text = "Enter your secret developer PIN. (Initial setup PIN can be set on first entry)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 10) pinInput = it
                            errorMessage = null
                        },
                        label = { Text("Developer PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("developer_pin_input")
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (pinInput.trim().length >= 4) {
                                isVerifying = true
                                errorMessage = null
                                viewModel.verifyDevPin(pinInput.trim()) { success ->
                                    isVerifying = false
                                    if (success) {
                                        onVerified()
                                    } else {
                                        errorMessage = "Incorrect Developer PIN. Verification failed."
                                    }
                                }
                            } else {
                                errorMessage = "PIN must be at least 4 characters"
                            }
                        },
                        enabled = !isVerifying && pinInput.trim().isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("verify_pin_button"),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Text("Unlock Developer Mode", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperDashboard(
    viewModel: FrenovaViewModel,
    onExit: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Overview & Stats, 1: Moderation Queue, 2: Audit Logs, 3: Announcements, 4: Security
    val allReports by viewModel.allReports.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val devStats by viewModel.devStats.collectAsState()
    val reportedChatToView by viewModel.reportedChatToView.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Frenova Dev Console", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("bhawareraj852@gmail.com", style = MaterialTheme.typography.labelSmall, color = CoralSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Exit Dev Mode")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshDevStats() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 12.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Overview") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        val pendingCount = allReports.count { it.status == "pending" }
                        BadgedBox(
                            badge = {
                                if (pendingCount > 0) Badge { Text("$pendingCount") }
                            }
                        ) {
                            Text("Reports ($pendingCount)")
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Audit Logs (${auditLogs.size})") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Announcements") }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Settings & PIN") }
                )
            }

            // Reported Chat Viewer Inspection Modal (Isolation check)
            if (reportedChatToView != null) {
                ReportedChatInspectionModal(
                    conversationId = reportedChatToView!!,
                    viewModel = viewModel,
                    onDismiss = { viewModel.selectReportedChat(null) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Overview & Stats
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text("System Analytics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatCard("Total Users", "${devStats["users"] ?: 0}", Icons.Default.People, IndigoLight, Modifier.weight(1f))
                                StatCard("Posts & Reels", "${devStats["posts"] ?: 0}", Icons.Default.Feed, CoralSecondary, Modifier.weight(1f))
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatCard("Live Rooms", "${devStats["rooms"] ?: 0}", Icons.Default.RecordVoiceOver, IndigoPrimary, Modifier.weight(1f))
                                StatCard("Pending Reports", "${allReports.count { it.status == "pending" }}", Icons.Default.ReportProblem, Color(0xFFEF4444), Modifier.weight(1f))
                            }
                        }

                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Developer Security Protocol", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "• Only bhawareraj852@gmail.com with verified server credentials has access.\n" +
                                        "• All moderation actions are saved in permanent Audit Logs.\n" +
                                        "• Private chats are strictly confidential: developer access is granted ONLY to the specific conversation when reported by a participant for moderation.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Moderation Queue
                    if (allReports.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No moderation reports in queue. Everything is calm!")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(allReports, key = { it.id }) { report ->
                                ReportItemCard(
                                    report = report,
                                    onViewChat = { convId ->
                                        viewModel.selectReportedChat(convId)
                                    },
                                    onResolve = {
                                        scope.launch {
                                            viewModel.repository.updateReportStatus(report.id, "resolved", "Resolved by developer")
                                            viewModel.showMessage("Report marked as resolved")
                                        }
                                    },
                                    onDismiss = {
                                        scope.launch {
                                            viewModel.repository.updateReportStatus(report.id, "dismissed", "Dismissed by developer")
                                            viewModel.showMessage("Report dismissed")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // Audit Logs
                    if (auditLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No developer audit actions logged yet.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(auditLogs, key = { it.id }) { log ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(log.action, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                            Text(log.developerEmail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(log.details, fontSize = 12.sp)
                                        if (log.targetId.isNotEmpty()) {
                                            Text("Target: ${log.targetId}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Announcements Manager
                    AnnouncementsManagerTab(viewModel = viewModel)
                }

                4 -> {
                    // App Settings & PIN
                    DeveloperSettingsTab(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, count: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(count, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ReportItemCard(
    report: Report,
    onViewChat: (String) -> Unit,
    onResolve: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text("Target: ${report.targetType.uppercase()}", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Badge(
                    containerColor = if (report.status == "pending") Color(0xFFEF4444) else Color(0xFF22C55E)
                ) {
                    Text(report.status.uppercase(), fontSize = 10.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("Reason: ${report.reason}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)

            if (report.details.isNotEmpty()) {
                Text("Details: ${report.details}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            Text("Reporter: ${report.reporterEmail}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Target ID: ${report.targetId}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // If it's a reported chat, allow developer to inspect that specific conversation
                if (report.targetType == "chat" && report.conversationId.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { onViewChat(report.conversationId) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("View Reported Chat", fontSize = 11.sp)
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Dismiss", fontSize = 11.sp)
                }

                Button(
                    onClick = onResolve,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Resolve", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun ReportedChatInspectionModal(
    conversationId: String,
    viewModel: FrenovaViewModel,
    onDismiss: () -> Unit
) {
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }

    LaunchedEffect(conversationId) {
        viewModel.repository.observeMessages(conversationId).collect {
            messages = it
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reported Conversation Audit (ID: ${conversationId.take(12)}...)") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                Text(
                    text = "Moderation Note: Developer access is restricted to this reported chat conversation only.",
                    fontSize = 11.sp,
                    color = CoralSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (messages.isEmpty()) {
                        item { Text("No messages found in this conversation.") }
                    } else {
                        items(messages, key = { it.id }) { msg ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text("${msg.senderName} (${msg.senderId.take(6)}...)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(msg.text, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done Reviewing") }
        }
    )
}

@Composable
fun AnnouncementsManagerTab(viewModel: FrenovaViewModel) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val announcements by viewModel.announcements.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Broadcast Announcement", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Announcement message") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (title.isNotEmpty() && content.isNotEmpty()) {
                                scope.launch {
                                    viewModel.repository.createAnnouncement(title, content)
                                    viewModel.showMessage("Announcement broadcasted!")
                                    title = ""
                                    content = ""
                                }
                            }
                        },
                        enabled = title.isNotEmpty() && content.isNotEmpty()
                    ) {
                        Text("Send to All Users")
                    }
                }
            }
        }

        item {
            Text("Active Announcements (${announcements.size})", fontWeight = FontWeight.Bold)
        }

        items(announcements, key = { it.id }) { ann ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(ann.title, fontWeight = FontWeight.Bold)
                    Text(ann.content, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun DeveloperSettingsTab(viewModel: FrenovaViewModel) {
    var newPinInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Update Developer PIN", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "The PIN is stored as a secure SHA-256 hash in Firestore app_settings and never hard-coded in the APK.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { newPinInput = it },
                        label = { Text("New Developer PIN (min 4 chars)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (newPinInput.length >= 4) {
                                scope.launch {
                                    val res = viewModel.repository.updateDeveloperPin(newPinInput)
                                    if (res.isSuccess) {
                                        viewModel.showMessage("Developer PIN updated successfully!")
                                        newPinInput = ""
                                    } else {
                                        viewModel.showMessage("Failed to update PIN: ${res.exceptionOrNull()?.message}")
                                    }
                                }
                            }
                        },
                        enabled = newPinInput.length >= 4
                    ) {
                        Text("Save New PIN")
                    }
                }
            }
        }
    }
}
