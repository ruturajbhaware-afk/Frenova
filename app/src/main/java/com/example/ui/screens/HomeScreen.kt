package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Announcement
import com.example.model.Comment
import com.example.model.Post
import com.example.model.Room
import com.example.ui.components.ReportDialog
import com.example.ui.theme.CoralSecondary
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.TealAccent
import com.example.util.Localization
import com.example.viewmodel.FrenovaViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FrenovaViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToRoom: (Room) -> Unit
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isDataSaver by viewModel.isDataSaver.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val likedPostIds by viewModel.likedPostIds.collectAsState()
    val blockedUserIds by viewModel.blockedUserIds.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val announcements by viewModel.announcements.collectAsState()

    var selectedFeedTab by remember { mutableStateOf(0) } // 0 = Posts, 1 = Reels
    var activeCommentPostId by remember { mutableStateOf<String?>(null) }
    var reportTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // targetType to targetId

    val filteredPosts = remember(posts, selectedFeedTab, blockedUserIds) {
        posts.filter { post ->
            !blockedUserIds.contains(post.authorId) &&
            if (selectedFeedTab == 0) !post.isReel else post.isReel
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("home_fab_create")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Data Saver Active Banner
            if (isDataSaver) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IndigoPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = IndigoLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Data Saver Active: Low-bandwidth media & smooth performance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Announcements Banner
            if (announcements.isNotEmpty()) {
                item {
                    AnnouncementCard(announcement = announcements.first())
                }
            }

            // Live Audio & Video Rooms Carousel
            if (rooms.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Live Friendship Rooms",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${rooms.size} active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(rooms, key = { it.id }) { room ->
                                RoomChip(
                                    room = room,
                                    onClick = { onNavigateToRoom(room) }
                                )
                            }
                        }
                    }
                }
            }

            // Posts & Reels Toggle Tab
            item {
                TabRow(
                    selectedTabIndex = selectedFeedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Tab(
                        selected = selectedFeedTab == 0,
                        onClick = { selectedFeedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Feed, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Localization.get("posts", currentLanguage), fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedFeedTab == 1,
                        onClick = { selectedFeedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SlowMotionVideo, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Localization.get("reels", currentLanguage), fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }

            // Empty state
            if (filteredPosts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (selectedFeedTab == 0) Icons.Default.ChatBubbleOutline else Icons.Default.Videocam,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (selectedFeedTab == 0) "No community posts yet" else "No short videos yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Be the first to share something positive!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(filteredPosts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        isLiked = likedPostIds.contains(post.id),
                        isDataSaver = isDataSaver,
                        currentLanguage = currentLanguage,
                        onLikeClick = { viewModel.toggleLike(post.id) },
                        onCommentClick = { activeCommentPostId = post.id },
                        onShareClick = { viewModel.showMessage("Link copied to clipboard!") },
                        onSaveClick = { viewModel.showMessage("Saved to your profile bookmarks") },
                        onReportClick = { reportTarget = Pair(if (post.isReel) "reel" else "post", post.id) },
                        onBlockUserClick = { viewModel.blockUser(post.authorId) }
                    )
                }
            }
        }
    }

    // Comment Sheet Dialog
    if (activeCommentPostId != null) {
        val targetPost = posts.firstOrNull { it.id == activeCommentPostId }
        if (targetPost != null) {
            CommentsDialog(
                post = targetPost,
                viewModel = viewModel,
                currentLanguage = currentLanguage,
                onDismiss = { activeCommentPostId = null }
            )
        }
    }

    // Report Dialog
    if (reportTarget != null) {
        val (type, id) = reportTarget!!
        ReportDialog(
            currentLanguage = currentLanguage,
            targetType = type,
            targetId = id,
            onDismiss = { reportTarget = null },
            onSubmitReport = { reason, details ->
                viewModel.submitReport(
                    targetType = type,
                    targetId = id,
                    reason = reason,
                    details = details
                )
            }
        )
    }
}

@Composable
fun RoomChip(room: Room, onClick: () -> Unit) {
    val icon = when (room.roomType) {
        "audio" -> Icons.Default.Mic
        "video" -> Icons.Default.Videocam
        else -> Icons.Default.Chat
    }
    val badgeColor = when (room.roomType) {
        "audio" -> CoralSecondary
        "video" -> TealAccent
        else -> IndigoLight
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .width(180.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(18.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("${room.participantCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = room.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Host: ${room.hostName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PostCard(
    post: Post,
    isLiked: Boolean,
    isDataSaver: Boolean,
    currentLanguage: String,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onReportClick: () -> Unit,
    onBlockUserClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Author info & options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.authorName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = IndigoLight
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (post.isReel) "Short Reel" else "Community Post",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(Localization.get("report", currentLanguage)) },
                            leadingIcon = { Icon(Icons.Default.Report, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onReportClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(Localization.get("block", currentLanguage)) },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onBlockUserClick()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content text
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )

            // Media Preview (if any)
            if (post.mediaUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (post.isReel) 280.dp else 190.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDataSaver) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (post.isReel) Icons.Default.PlayCircle else Icons.Default.Image,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Low-data preview active",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        AsyncImage(
                            model = post.mediaUrl,
                            contentDescription = "Post media",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action row: Like, Comment, Share, Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onLikeClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) CoralSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${post.likesCount}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(
                        onClick = onCommentClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${post.commentsCount}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onSaveClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentsDialog(
    post: Post,
    viewModel: FrenovaViewModel,
    currentLanguage: String,
    onDismiss: () -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var commentsList by remember { mutableStateOf<List<Comment>>(emptyList()) }

    LaunchedEffect(post.id) {
        viewModel.repository.observeComments(post.id).collect {
            commentsList = it
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Comments (${commentsList.size})") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (commentsList.isEmpty()) {
                        item {
                            Text(
                                "No comments yet. Start the conversation!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(commentsList, key = { it.id }) { c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(c.authorName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(c.text, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Write a positive comment...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val txt = commentText.trim()
                            if (txt.isNotEmpty()) {
                                val profile = viewModel.currentUserProfile.value
                                val name = profile?.displayName?.ifEmpty { profile.username } ?: "Friend"
                                scope.launch {
                                    viewModel.repository.addComment(post.id, name, "", txt)
                                    commentText = ""
                                }
                            }
                        },
                        enabled = commentText.trim().isNotEmpty()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun AnnouncementCard(announcement: Announcement) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = IndigoPrimary.copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Campaign, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = announcement.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
