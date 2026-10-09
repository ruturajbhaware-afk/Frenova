package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FrenovaRepository
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FrenovaViewModel(application: Application) : AndroidViewModel(application) {

    val repository = FrenovaRepository(application.applicationContext)

    // Current User Profile
    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    // Preferences
    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _isDataSaver = MutableStateFlow(false)
    val isDataSaver: StateFlow<Boolean> = _isDataSaver.asStateFlow()

    // Navigation & Selected Conversation/Room
    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    private val _selectedRoom = MutableStateFlow<Room?>(null)
    val selectedRoom: StateFlow<Room?> = _selectedRoom.asStateFlow()

    // Feeds & Lists
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _likedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val likedPostIds: StateFlow<Set<String>> = _likedPostIds.asStateFlow()

    private val _blockedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedUserIds: StateFlow<Set<String>> = _blockedUserIds.asStateFlow()

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

    private val _friendships = MutableStateFlow<List<Friendship>>(emptyList())
    val friendships: StateFlow<List<Friendship>> = _friendships.asStateFlow()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    // Developer Mode
    private val _isDevPinVerified = MutableStateFlow(false)
    val isDevPinVerified: StateFlow<Boolean> = _isDevPinVerified.asStateFlow()

    private val _allReports = MutableStateFlow<List<Report>>(emptyList())
    val allReports: StateFlow<List<Report>> = _allReports.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
    val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()

    private val _devStats = MutableStateFlow<Map<String, Int>>(emptyMap())
    val devStats: StateFlow<Map<String, Int>> = _devStats.asStateFlow()

    private val _reportedChatToView = MutableStateFlow<String?>(null)
    val reportedChatToView: StateFlow<String?> = _reportedChatToView.asStateFlow()

    // UI feedback / Snackbars
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
        val profile = _currentUserProfile.value
        if (profile != null) {
            viewModelScope.launch {
                repository.createOrUpdateUserProfile(
                    username = profile.username,
                    displayName = profile.displayName,
                    bio = profile.bio,
                    avatarUrl = profile.avatarUrl,
                    isPrivate = profile.isPrivate,
                    locationSharingEnabled = profile.locationSharingEnabled,
                    approxCity = profile.approxCity,
                    language = lang,
                    isDataSaver = profile.isDataSaver
                )
            }
        }
    }

    fun toggleDarkTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun toggleDataSaver() {
        val newVal = !_isDataSaver.value
        _isDataSaver.value = newVal
        val profile = _currentUserProfile.value
        if (profile != null) {
            viewModelScope.launch {
                repository.createOrUpdateUserProfile(
                    username = profile.username,
                    displayName = profile.displayName,
                    bio = profile.bio,
                    avatarUrl = profile.avatarUrl,
                    isPrivate = profile.isPrivate,
                    locationSharingEnabled = profile.locationSharingEnabled,
                    approxCity = profile.approxCity,
                    language = profile.language,
                    isDataSaver = newVal
                )
            }
        }
    }

    fun selectConversation(convId: String?) {
        _selectedConversationId.value = convId
    }

    fun selectRoom(room: Room?) {
        _selectedRoom.value = room
    }

    fun selectReportedChat(convId: String?) {
        _reportedChatToView.value = convId
    }

    fun initSession(userId: String) {
        viewModelScope.launch {
            // Observe Profile
            repository.observeUserProfile(userId).collect { profile ->
                if (profile != null) {
                    _currentUserProfile.value = profile
                    _currentLanguage.value = profile.language
                    _isDataSaver.value = profile.isDataSaver
                } else {
                    // Seed initial default profile
                    val email = repository.currentUserEmail() ?: ""
                    val defaultName = email.substringBefore("@").replace(".", " ").capitalize()
                    val defaultUsername = email.substringBefore("@").lowercase().replace(".", "_")
                    repository.createOrUpdateUserProfile(
                        username = defaultUsername,
                        displayName = defaultName,
                        bio = "Happy to connect on Frenova!",
                        avatarUrl = "",
                        isPrivate = false,
                        locationSharingEnabled = false,
                        approxCity = "",
                        language = "en",
                        isDataSaver = false
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.observePosts().collect { list ->
                _posts.value = list
            }
        }

        viewModelScope.launch {
            repository.observeMyLikes().collect { likes ->
                _likedPostIds.value = likes
            }
        }

        viewModelScope.launch {
            repository.observeBlocks().collect { blocks ->
                _blockedUserIds.value = blocks
            }
        }

        viewModelScope.launch {
            repository.observeRooms().collect { rList ->
                _rooms.value = rList
            }
        }

        viewModelScope.launch {
            repository.observeMyFriendships().collect { fList ->
                _friendships.value = fList
            }
        }

        viewModelScope.launch {
            repository.observeMyConversations().collect { cList ->
                _conversations.value = cList
            }
        }

        viewModelScope.launch {
            repository.observeAnnouncements().collect { aList ->
                _announcements.value = aList
            }
        }

        // If developer, observe reports & audit logs
        if (repository.isDeveloperAccount()) {
            viewModelScope.launch {
                repository.observeAllReports().collect { repList ->
                    _allReports.value = repList
                }
            }
            viewModelScope.launch {
                repository.observeAuditLogs().collect { logs ->
                    _auditLogs.value = logs
                }
            }
            refreshDevStats()
        }
    }

    fun refreshDevStats() {
        if (!repository.isDeveloperAccount()) return
        viewModelScope.launch {
            _devStats.value = repository.getDeveloperStats()
        }
    }

    fun verifyDevPin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repository.verifyDeveloperPin(pin)
            _isDevPinVerified.value = ok
            if (ok) {
                refreshDevStats()
            }
            onResult(ok)
        }
    }

    fun exitDeveloperMode() {
        _isDevPinVerified.value = false
    }

    fun createPost(content: String, mediaUrl: String, mediaType: String, isReel: Boolean) {
        val profile = _currentUserProfile.value ?: return
        viewModelScope.launch {
            val res = repository.createPost(
                authorName = profile.displayName.ifEmpty { profile.username },
                authorAvatar = profile.avatarUrl,
                content = content,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                isReel = isReel
            )
            if (res.isSuccess) {
                showMessage("Post shared successfully!")
            } else {
                showMessage("Failed to share post: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun toggleLike(postId: String) {
        val isLiked = _likedPostIds.value.contains(postId)
        viewModelScope.launch {
            repository.toggleLike(postId, isLiked)
        }
    }

    fun sendFriendRequest(targetUserId: String) {
        viewModelScope.launch {
            val res = repository.sendFriendRequest(targetUserId)
            if (res.isSuccess) {
                showMessage("Friend request sent!")
            } else {
                showMessage("Could not send request: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun respondToFriendRequest(friendshipId: String, accept: Boolean) {
        viewModelScope.launch {
            repository.respondToFriendRequest(friendshipId, accept)
            showMessage(if (accept) "Friend request accepted!" else "Request declined")
        }
    }

    fun removeFriend(friendshipId: String) {
        viewModelScope.launch {
            repository.removeFriend(friendshipId)
            showMessage("Friend removed")
        }
    }

    fun startChatWithFriend(friendUserId: String) {
        viewModelScope.launch {
            val res = repository.getOrCreateConversation(friendUserId)
            if (res.isSuccess) {
                _selectedConversationId.value = res.getOrNull()
            } else {
                showMessage("Failed to open chat: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun blockUser(targetUserId: String) {
        viewModelScope.launch {
            repository.blockUser(targetUserId)
            showMessage("User has been blocked")
        }
    }

    fun unblockUser(targetUserId: String) {
        viewModelScope.launch {
            repository.unblockUser(targetUserId)
            showMessage("User unblocked")
        }
    }

    fun submitReport(
        targetType: String,
        targetId: String,
        reason: String,
        details: String,
        reportedUserId: String = "",
        conversationId: String = ""
    ) {
        viewModelScope.launch {
            val res = repository.submitReport(
                targetType = targetType,
                targetId = targetId,
                reason = reason,
                details = details,
                reportedUserId = reportedUserId,
                conversationId = conversationId
            )
            if (res.isSuccess) {
                showMessage("Report submitted. Our moderation team will review it.")
            } else {
                showMessage("Could not submit report: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun createRoom(
        title: String,
        description: String,
        roomType: String,
        isPrivate: Boolean,
        allowedMemberIds: List<String> = emptyList()
    ) {
        val profile = _currentUserProfile.value ?: return
        viewModelScope.launch {
            val res = repository.createRoom(
                title = title,
                description = description,
                hostName = profile.displayName.ifEmpty { profile.username },
                roomType = roomType,
                isPrivate = isPrivate,
                allowedMemberIds = allowedMemberIds
            )
            if (res.isSuccess) {
                showMessage("Room created!")
            } else {
                showMessage("Could not create room: ${res.exceptionOrNull()?.message}")
            }
        }
    }
}
