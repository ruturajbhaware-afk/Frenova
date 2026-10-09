package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.SignInScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOut
import com.example.ui.components.FrenovaBottomNavigationBar
import com.example.ui.components.FrenovaTopAppBar
import com.example.ui.screens.*
import com.example.ui.theme.FrenovaTheme
import com.example.viewmodel.FrenovaViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val credentialManager = remember { CredentialManager.create(context) }
            val auth = remember { FirebaseAuth.getInstance() }

            var currentUser by remember { mutableStateOf(auth.currentUser) }
            var currentLanguage by remember { mutableStateOf("en") }

            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { fbAuth ->
                    currentUser = fbAuth.currentUser
                }
                auth.addAuthStateListener(listener)
                onDispose {
                    auth.removeAuthStateListener(listener)
                }
            }

            // Attempt silent auto sign-in on startup
            LaunchedEffect(Unit) {
                attemptAutoSignIn(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = { currentUser = auth.currentUser },
                    onUnauthenticated = { /* stay on login screen */ },
                    scope = coroutineScope
                )
            }

            if (currentUser == null) {
                FrenovaTheme(darkTheme = true) {
                    SignInScreen(
                        currentLanguage = currentLanguage,
                        onLanguageChange = { currentLanguage = it },
                        onAuthSuccess = {
                            currentUser = auth.currentUser
                        }
                    )
                }
            } else {
                val frenovaViewModel: FrenovaViewModel = viewModel()

                LaunchedEffect(currentUser!!.uid) {
                    frenovaViewModel.initSession(currentUser!!.uid)
                }

                val isDarkTheme by frenovaViewModel.isDarkTheme.collectAsState()

                FrenovaTheme(darkTheme = isDarkTheme) {
                    MainAppScaffold(
                        viewModel = frenovaViewModel,
                        onSignOut = {
                            signOut(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = {
                                    currentUser = null
                                },
                                scope = coroutineScope
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainAppScaffold(
    viewModel: FrenovaViewModel,
    onSignOut: () -> Unit
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isDataSaver by viewModel.isDataSaver.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val selectedRoom by viewModel.selectedRoom.collectAsState()
    val announcements by viewModel.announcements.collectAsState()

    var currentNavRoute by remember { mutableStateOf("home") }
    var developerModeOpen by remember { mutableStateOf(false) }
    var announcementsDialogVisible by remember { mutableStateOf(false) }
    var languageDialogVisible by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userMessage) {
        if (userMessage != null) {
            snackbarHostState.showSnackbar(userMessage!!)
            viewModel.clearMessage()
        }
    }

    // Active Room overlay
    if (selectedRoom != null) {
        ActiveRoomScreen(
            room = selectedRoom!!,
            viewModel = viewModel,
            onLeave = { viewModel.selectRoom(null) }
        )
        return
    }

    // Developer Mode Screen
    if (developerModeOpen) {
        DeveloperModeScreen(
            viewModel = viewModel,
            onExit = { developerModeOpen = false }
        )
        return
    }

    Scaffold(
        topBar = {
            FrenovaTopAppBar(
                currentLanguage = currentLanguage,
                isDataSaver = isDataSaver,
                onDataSaverToggle = { viewModel.toggleDataSaver() },
                onLanguageClick = { languageDialogVisible = true },
                onAnnouncementsClick = { announcementsDialogVisible = true },
                unreadAnnouncementsCount = announcements.size
            )
        },
        bottomBar = {
            FrenovaBottomNavigationBar(
                currentRoute = currentNavRoute,
                onNavigate = { route -> currentNavRoute = route },
                currentLanguage = currentLanguage
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.systemBars
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentNavRoute) {
                "home" -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToCreate = { currentNavRoute = "create" },
                    onNavigateToRoom = { room -> viewModel.selectRoom(room) }
                )
                "discover" -> DiscoverScreen(
                    viewModel = viewModel,
                    onNavigateToRoom = { room -> viewModel.selectRoom(room) },
                    onStartChatWithUser = { otherUid ->
                        viewModel.startChatWithFriend(otherUid)
                        currentNavRoute = "messages"
                    }
                )
                "create" -> CreateScreen(
                    viewModel = viewModel,
                    onFinish = { currentNavRoute = "home" }
                )
                "messages" -> MessagesScreen(
                    viewModel = viewModel
                )
                "profile" -> ProfileScreen(
                    viewModel = viewModel,
                    onSignOut = onSignOut,
                    onOpenDeveloperMode = { developerModeOpen = true }
                )
            }
        }
    }

    // Language Dialog
    if (languageDialogVisible) {
        AlertDialog(
            onDismissRequest = { languageDialogVisible = false },
            title = { Text("Select Language / भाषा") },
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
                                .padding(vertical = 8.dp),
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
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { languageDialogVisible = false }) { Text("Done") }
            }
        )
    }

    // Announcements Dialog
    if (announcementsDialogVisible) {
        AlertDialog(
            onDismissRequest = { announcementsDialogVisible = false },
            title = { Text("Community Announcements") },
            text = {
                if (announcements.isEmpty()) {
                    Text("No announcements at this time.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(announcements.size) { index ->
                            val a = announcements[index]
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(a.title, fontWeight = FontWeight.Bold)
                                    Text(a.content, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { announcementsDialogVisible = false }) { Text("Close") }
            }
        )
    }
}
