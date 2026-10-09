package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralSecondary
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.util.Localization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrenovaTopAppBar(
    currentLanguage: String,
    isDataSaver: Boolean,
    onDataSaverToggle: () -> Unit,
    onLanguageClick: () -> Unit,
    onAnnouncementsClick: () -> Unit,
    unreadAnnouncementsCount: Int = 0
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(IndigoPrimary, CoralSecondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = Localization.get("app_name", currentLanguage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = Localization.get("tagline", currentLanguage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            // Data saver chip
            IconButton(
                onClick = onDataSaverToggle,
                modifier = Modifier.testTag("data_saver_toggle")
            ) {
                Icon(
                    imageVector = if (isDataSaver) Icons.Filled.DataSaverOn else Icons.Outlined.DataSaverOff,
                    contentDescription = "Data Saver",
                    tint = if (isDataSaver) IndigoLight else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Language switch
            IconButton(
                onClick = onLanguageClick,
                modifier = Modifier.testTag("language_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Language",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Announcements / Notifications
            IconButton(
                onClick = onAnnouncementsClick,
                modifier = Modifier.testTag("announcements_button")
            ) {
                BadgedBox(
                    badge = {
                        if (unreadAnnouncementsCount > 0) {
                            Badge { Text("$unreadAnnouncementsCount") }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Announcements",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun FrenovaBottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    currentLanguage: String,
    unreadMessagesCount: Int = 0
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        val items = listOf(
            Triple("home", Icons.Filled.Home, Localization.get("home", currentLanguage)),
            Triple("discover", Icons.Filled.Explore, Localization.get("discover", currentLanguage)),
            Triple("create", Icons.Filled.AddCircle, Localization.get("create", currentLanguage)),
            Triple("messages", Icons.Filled.ChatBubble, Localization.get("messages", currentLanguage)),
            Triple("profile", Icons.Filled.Person, Localization.get("profile", currentLanguage))
        )

        items.forEach { (route, icon, label) ->
            val selected = currentRoute == route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(route) },
                icon = {
                    if (route == "messages" && unreadMessagesCount > 0) {
                        BadgedBox(badge = { Badge { Text("$unreadMessagesCount") } }) {
                            Icon(icon, contentDescription = label)
                        }
                    } else {
                        Icon(icon, contentDescription = label)
                    }
                },
                label = { Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag("nav_$route")
            )
        }
    }
}

@Composable
fun ReportDialog(
    currentLanguage: String,
    targetType: String,
    targetId: String,
    onDismiss: () -> Unit,
    onSubmitReport: (reason: String, details: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf(Localization.get("report_reason_harassment", currentLanguage)) }
    var details by remember { mutableStateOf("") }

    val reasons = listOf(
        Localization.get("report_reason_harassment", currentLanguage),
        Localization.get("report_reason_inappropriate", currentLanguage),
        Localization.get("report_reason_dating", currentLanguage),
        Localization.get("report_reason_spam", currentLanguage),
        Localization.get("report_reason_other", currentLanguage)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ReportProblem,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(Localization.get("report_title", currentLanguage))
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Help keep Frenova safe and friendly. Choose a reason:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(reason, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Additional context (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmitReport(selectedReason, details)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_submit_report")
            ) {
                Text(Localization.get("submit_report", currentLanguage))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
