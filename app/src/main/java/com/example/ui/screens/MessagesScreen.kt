package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.Friendship
import com.example.model.UserProfile
import com.example.ui.components.ReportDialog
import com.example.ui.theme.CoralSecondary
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.util.Localization
import com.example.viewmodel.FrenovaViewModel
import kotlinx.coroutines.launch

@Composable
fun MessagesScreen(
    viewModel: FrenovaViewModel
) {
    val selectedConversationId by viewModel.selectedConversationId.collectAsState()

    if (selectedConversationId != null) {
        ChatConversationScreen(
            conversationId = selectedConversationId!!,
            viewModel = viewModel,
            onBack = { viewModel.selectConversation(null) }
        )
    } else {
        MessagesListScreen(viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesListScreen(
    viewModel: FrenovaViewModel
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val friendships by viewModel.friendships.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val currentUid = viewModel.repository.currentUserId()

    var selectedTab by remember { mutableStateOf(0) } // 0: Chats, 1: Friends, 2: Requests

    val acceptedFriendships = remember(friendships) {
        friendships.filter { it.status == "accepted" }
    }

    val pendingRequests = remember(friendships, currentUid) {
        friendships.filter { it.status == "pending" && it.receiverId == currentUid }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Chats (${conversations.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(Localization.get("friends", currentLanguage) + " (${acceptedFriendships.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    BadgedBox(
                        badge = {
                            if (pendingRequests.isNotEmpty()) {
                                Badge { Text("${pendingRequests.size}") }
                            }
                        }
                    ) {
                        Text(Localization.get("friend_requests", currentLanguage), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        when (selectedTab) {
            0 -> {
                // Chats list
                if (conversations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Forum, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No active chats yet", fontWeight = FontWeight.Bold)
                            Text("Go to Friends tab and tap 'Chat' with an accepted friend!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(conversations, key = { it.id }) { conv ->
                            val otherUid = conv.participantIds.firstOrNull { it != currentUid } ?: ""
                            ConversationItem(
                                conversation = conv,
                                otherUserId = otherUid,
                                viewModel = viewModel,
                                onClick = { viewModel.selectConversation(conv.id) }
                            )
                        }
                    }
                }
            }

            1 -> {
                // Friends list
                if (acceptedFriendships.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No accepted friends yet", fontWeight = FontWeight.Bold)
                            Text("Use Discover tab to search and add friends by interest!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(acceptedFriendships, key = { it.id }) { friendship ->
                            val friendUid = if (friendship.requesterId == currentUid) friendship.receiverId else friendship.requesterId
                            FriendRowItem(
                                friendship = friendship,
                                friendUserId = friendUid,
                                viewModel = viewModel,
                                currentLanguage = currentLanguage
                            )
                        }
                    }
                }
            }

            2 -> {
                // Pending Friend Requests
                if (pendingRequests.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No pending friend requests", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pendingRequests, key = { it.id }) { friendship ->
                            PendingRequestItem(
                                friendship = friendship,
                                viewModel = viewModel,
                                currentLanguage = currentLanguage
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    otherUserId: String,
    viewModel: FrenovaViewModel,
    onClick: () -> Unit
) {
    var otherProfile by remember { mutableStateOf<UserProfile?>(null) }
    LaunchedEffect(otherUserId) {
        if (otherUserId.isNotEmpty()) {
            otherProfile = viewModel.repository.getUserProfile(otherUserId)
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("conversation_item_${conversation.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(IndigoPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (otherProfile?.displayName ?: "Friend").take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = IndigoLight,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = otherProfile?.displayName ?: "Friend",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = conversation.lastMessage.ifEmpty { "Start conversation..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun FriendRowItem(
    friendship: Friendship,
    friendUserId: String,
    viewModel: FrenovaViewModel,
    currentLanguage: String
) {
    var friendProfile by remember { mutableStateOf<UserProfile?>(null) }
    LaunchedEffect(friendUserId) {
        friendProfile = viewModel.repository.getUserProfile(friendUserId)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(CoralSecondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (friendProfile?.displayName ?: "F").take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = CoralSecondary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(friendProfile?.displayName ?: "Friend", fontWeight = FontWeight.Bold)
                    Text("@${friendProfile?.username ?: ""}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { viewModel.startChatWithFriend(friendUserId) },
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chat", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = { viewModel.removeFriend(friendship.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.PersonRemove, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun PendingRequestItem(
    friendship: Friendship,
    viewModel: FrenovaViewModel,
    currentLanguage: String
) {
    var requesterProfile by remember { mutableStateOf<UserProfile?>(null) }
    LaunchedEffect(friendship.requesterId) {
        requesterProfile = viewModel.repository.getUserProfile(friendship.requesterId)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(IndigoPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (requesterProfile?.displayName ?: "U").take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = IndigoLight
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(requesterProfile?.displayName ?: "User", fontWeight = FontWeight.Bold)
                    Text("@${requesterProfile?.username ?: ""}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { viewModel.respondToFriendRequest(friendship.id, accept = true) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(Localization.get("accept", currentLanguage), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedButton(
                    onClick = { viewModel.respondToFriendRequest(friendship.id, accept = false) },
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(Localization.get("reject", currentLanguage), fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationScreen(
    conversationId: String,
    viewModel: FrenovaViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val currentUid = viewModel.repository.currentUserId()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputMessage by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var reportDialogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(conversationId) {
        viewModel.repository.observeMessages(conversationId).collect { msgList ->
            messages = msgList
            if (msgList.isNotEmpty()) {
                listState.animateScrollToItem(msgList.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Direct Chat", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Report Chat") },
                                leadingIcon = { Icon(Icons.Default.Report, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    reportDialogVisible = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔒 Messages between accepted friends are private & secured.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    val isMe = msg.senderId == currentUid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isMe) 16.dp else 4.dp,
                                        bottomEnd = if (isMe) 4.dp else 16.dp
                                    )
                                )
                                .background(
                                    if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column {
                                if (!isMe) {
                                    Text(
                                        text = msg.senderName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                Text(
                                    text = msg.text,
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Input Bar
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputMessage,
                        onValueChange = { inputMessage = it },
                        placeholder = { Text("Type a friendly message...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val txt = inputMessage.trim()
                            if (txt.isNotEmpty()) {
                                val profile = viewModel.currentUserProfile.value
                                val name = profile?.displayName?.ifEmpty { profile.username } ?: "Friend"
                                scope.launch {
                                    viewModel.repository.sendMessage(conversationId, name, txt)
                                    inputMessage = ""
                                }
                            }
                        },
                        enabled = inputMessage.trim().isNotEmpty(),
                        modifier = Modifier.testTag("chat_send_button")
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    if (reportDialogVisible) {
        ReportDialog(
            currentLanguage = currentLanguage,
            targetType = "chat",
            targetId = conversationId,
            onDismiss = { reportDialogVisible = false },
            onSubmitReport = { reason, details ->
                viewModel.submitReport(
                    targetType = "chat",
                    targetId = conversationId,
                    reason = reason,
                    details = details,
                    conversationId = conversationId
                )
            }
        )
    }
}
