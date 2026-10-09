package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfile
import com.example.ui.theme.CoralSecondary
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.util.Localization
import com.example.viewmodel.FrenovaViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: FrenovaViewModel,
    onSignOut: () -> Unit,
    onOpenDeveloperMode: () -> Unit
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isDataSaver by viewModel.isDataSaver.collectAsState()
    val profile by viewModel.currentUserProfile.collectAsState()
    val blockedUserIds by viewModel.blockedUserIds.collectAsState()
    val isDevAccount = viewModel.repository.isDeveloperAccount()
    val scope = rememberCoroutineScope()

    var editProfileDialogVisible by remember { mutableStateOf(false) }
    var languageDialogVisible by remember { mutableStateOf(false) }
    var blockedUsersDialogVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Header Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(IndigoPrimary, CoralSecondary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (profile?.displayName ?: "User").take(1).uppercase(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = profile?.displayName ?: "Frenova Friend",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "@${profile?.username ?: "friend"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (profile?.bio?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = profile?.bio ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (profile?.locationSharingEnabled == true && profile?.approxCity?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "📍 ${profile?.approxCity} (Approximate location only)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { editProfileDialogVisible = true },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Profile")
                    }
                }
            }
        }

        // Privacy & Safety Controls Section
        item {
            Text(
                text = "Privacy & Safety Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Private Account
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Localization.get("private_account", currentLanguage), fontWeight = FontWeight.SemiBold)
                            Text(
                                Localization.get("private_account_desc", currentLanguage),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = profile?.isPrivate == true,
                            onCheckedChange = { isPriv ->
                                scope.launch {
                                    val p = profile ?: return@launch
                                    viewModel.repository.createOrUpdateUserProfile(
                                        username = p.username,
                                        displayName = p.displayName,
                                        bio = p.bio,
                                        avatarUrl = p.avatarUrl,
                                        isPrivate = isPriv,
                                        locationSharingEnabled = p.locationSharingEnabled,
                                        approxCity = p.approxCity,
                                        language = p.language,
                                        isDataSaver = p.isDataSaver
                                    )
                                }
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Location Sharing (strictly OFF by default, approximate city only)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Localization.get("location_sharing", currentLanguage), fontWeight = FontWeight.SemiBold)
                            Text(
                                Localization.get("location_sharing_desc", currentLanguage),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = profile?.locationSharingEnabled == true,
                            onCheckedChange = { locOn ->
                                scope.launch {
                                    val p = profile ?: return@launch
                                    viewModel.repository.createOrUpdateUserProfile(
                                        username = p.username,
                                        displayName = p.displayName,
                                        bio = p.bio,
                                        avatarUrl = p.avatarUrl,
                                        isPrivate = p.isPrivate,
                                        locationSharingEnabled = locOn,
                                        approxCity = if (locOn) (p.approxCity.ifEmpty { "Mumbai" }) else "",
                                        language = p.language,
                                        isDataSaver = p.isDataSaver
                                    )
                                }
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Blocked Users
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { blockedUsersDialogVisible = true }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Blocked Users (${blockedUserIds.size})", fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // App Preferences Section
        item {
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Data Saver Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Localization.get("data_saver", currentLanguage), fontWeight = FontWeight.SemiBold)
                            Text(
                                Localization.get("data_saver_desc", currentLanguage),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDataSaver,
                            onCheckedChange = { viewModel.toggleDataSaver() }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Language Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { languageDialogVisible = true }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = IndigoPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(Localization.get("language", currentLanguage), fontWeight = FontWeight.SemiBold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when (currentLanguage) {
                                    "mr" -> "मराठी"
                                    "hi" -> "हिंदी"
                                    else -> "English"
                                },
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Dark Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = IndigoLight)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(Localization.get("dark_mode", currentLanguage), fontWeight = FontWeight.SemiBold)
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { viewModel.toggleDarkTheme() }
                        )
                    }
                }
            }
        }

        // DEVELOPER MODE (COMPLETELY HIDDEN FROM NORMAL USERS!)
        // ONLY visible if authenticated developer account email == bhawareraj852@gmail.com
        if (isDevAccount) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CoralSecondary.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenDeveloperMode() }
                        .testTag("developer_mode_entry_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CoralSecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = Localization.get("developer_mode", currentLanguage),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralSecondary
                                )
                                Text(
                                    text = "Secure Admin & Moderation Console",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.Lock, contentDescription = null, tint = CoralSecondary)
                    }
                }
            }
        }

        // Sign Out Button
        item {
            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Localization.get("sign_out", currentLanguage),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Edit Profile Dialog
    if (editProfileDialogVisible) {
        val p = profile
        var newDisplayName by remember { mutableStateOf(p?.displayName ?: "") }
        var newBio by remember { mutableStateOf(p?.bio ?: "") }
        var newCity by remember { mutableStateOf(p?.approxCity ?: "") }

        AlertDialog(
            onDismissRequest = { editProfileDialogVisible = false },
            title = { Text("Edit Profile") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newDisplayName,
                        onValueChange = { newDisplayName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newBio,
                        onValueChange = { newBio = it },
                        label = { Text("Bio") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCity,
                        onValueChange = { newCity = it },
                        label = { Text("Approximate City (e.g. Pune, Nagpur)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentP = profile ?: return@Button
                        scope.launch {
                            viewModel.repository.createOrUpdateUserProfile(
                                username = currentP.username,
                                displayName = newDisplayName.trim().ifEmpty { currentP.displayName },
                                bio = newBio.trim(),
                                avatarUrl = currentP.avatarUrl,
                                isPrivate = currentP.isPrivate,
                                locationSharingEnabled = currentP.locationSharingEnabled,
                                approxCity = newCity.trim(),
                                language = currentP.language,
                                isDataSaver = currentP.isDataSaver
                            )
                            editProfileDialogVisible = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editProfileDialogVisible = false }) { Text("Cancel") }
            }
        )
    }

    // Language Selector Dialog
    if (languageDialogVisible) {
        AlertDialog(
            onDismissRequest = { languageDialogVisible = false },
            title = { Text("Select Language / भाषा निवडा") },
            text = {
                Column {
                    listOf(
                        "en" to "English",
                        "mr" to "मराठी (Marathi)",
                        "hi" to "हिंदी (Hindi)"
                    ).forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLanguage(code)
                                    languageDialogVisible = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLanguage == code,
                                onClick = {
                                    viewModel.setLanguage(code)
                                    languageDialogVisible = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, fontWeight = if (currentLanguage == code) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { languageDialogVisible = false }) { Text("Done") }
            }
        )
    }

    // Blocked Users Dialog
    if (blockedUsersDialogVisible) {
        AlertDialog(
            onDismissRequest = { blockedUsersDialogVisible = false },
            title = { Text("Blocked Users (${blockedUserIds.size})") },
            text = {
                if (blockedUserIds.isEmpty()) {
                    Text("You have not blocked any users.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(blockedUserIds.toList()) { blockedUid ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("User ID: ${blockedUid.take(8)}...", fontSize = 12.sp)
                                OutlinedButton(
                                    onClick = { viewModel.unblockUser(blockedUid) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Unblock", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { blockedUsersDialogVisible = false }) { Text("Close") }
            }
        )
    }
}
