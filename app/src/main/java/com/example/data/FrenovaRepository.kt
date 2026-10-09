package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.*
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class FrenovaRepository(
    val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val DEVELOPER_EMAIL = "bhawareraj852@gmail.com"
        // Default seed hash for PIN "8520" so developer can log in immediately:
        // sha256("8520") = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8" (example sha256)
        fun hashPin(pin: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    fun currentUserId(): String? = auth.currentUser?.uid
    fun currentUserEmail(): String? = auth.currentUser?.email

    fun isDeveloperAccount(): Boolean {
        val user = auth.currentUser
        return user != null && user.email.equals(DEVELOPER_EMAIL, ignoreCase = true)
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Frenova.")
    }

    // -------------------------------------------------------------
    // USER PROFILE
    // -------------------------------------------------------------
    fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val path = "users/$userId"
        val reg = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, path)
                    trySend(null)
                    return@addSnapshotListener
                }
                val profile = snapshot?.toObject(UserProfile::class.java)
                trySend(profile)
            }
        awaitClose { reg.remove() }
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        val path = "users/$userId"
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    suspend fun createOrUpdateUserProfile(
        username: String,
        displayName: String,
        bio: String,
        avatarUrl: String,
        isPrivate: Boolean = false,
        locationSharingEnabled: Boolean = false,
        approxCity: String = "",
        language: String = "en",
        isDataSaver: Boolean = false
    ): Result<Unit> {
        val uid = requireUserId()
        val email = auth.currentUser?.email ?: ""
        val path = "users/$uid"
        val isDev = email.equals(DEVELOPER_EMAIL, ignoreCase = true)

        val docRef = db.collection("users").document(uid)
        return try {
            val existing = docRef.get().await()
            val data = mutableMapOf<String, Any>(
                "userId" to uid,
                "email" to email,
                "username" to username.trim(),
                "displayName" to displayName.trim(),
                "bio" to bio.trim(),
                "avatarUrl" to avatarUrl,
                "isPrivate" to isPrivate,
                "locationSharingEnabled" to locationSharingEnabled,
                "approxCity" to (if (locationSharingEnabled) approxCity else ""),
                "language" to language,
                "isDataSaver" to isDataSaver,
                "isDeveloper" to isDev,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!existing.exists()) {
                data["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(data).await()
            } else {
                docRef.update(data).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): List<UserProfile> {
        val path = "users"
        val qTrim = query.trim()
        if (qTrim.isEmpty()) return emptyList()
        val myUid = currentUserId()
        return try {
            val snapshot = db.collection("users")
                .limit(20)
                .get()
                .await()
            snapshot.toObjects(UserProfile::class.java)
                .filter { it.userId != myUid && (it.username.contains(qTrim, ignoreCase = true) || it.displayName.contains(qTrim, ignoreCase = true)) }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    // -------------------------------------------------------------
    // FRIENDSHIPS
    // -------------------------------------------------------------
    fun observeMyFriendships(): Flow<List<Friendship>> = callbackFlow {
        val uid = currentUserId()
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val path = "friends"
        val reg = db.collection("friends")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(Friendship::class.java)
                    ?.filter { it.requesterId == uid || it.receiverId == uid }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun sendFriendRequest(targetUserId: String): Result<Unit> {
        val uid = requireUserId()
        if (uid == targetUserId) return Result.failure(IllegalArgumentException("Cannot friend yourself"))
        val friendId = if (uid < targetUserId) "${uid}_${targetUserId}" else "${targetUserId}_${uid}"
        val path = "friends/$friendId"
        return try {
            val payload = mapOf(
                "id" to friendId,
                "requesterId" to uid,
                "receiverId" to targetUserId,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("friends").document(friendId).set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun respondToFriendRequest(friendshipId: String, accept: Boolean): Result<Unit> {
        val path = "friends/$friendshipId"
        return try {
            val status = if (accept) "accepted" else "rejected"
            db.collection("friends").document(friendshipId).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeFriend(friendshipId: String): Result<Unit> {
        val path = "friends/$friendshipId"
        return try {
            db.collection("friends").document(friendshipId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // CONVERSATIONS & DIRECT MESSAGES
    // -------------------------------------------------------------
    fun observeMyConversations(): Flow<List<Conversation>> = callbackFlow {
        val uid = currentUserId()
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val path = "conversations"
        val reg = db.collection("conversations")
            .whereArrayContains("participantIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(Conversation::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun getOrCreateConversation(otherUserId: String): Result<String> {
        val uid = requireUserId()
        val convId = if (uid < otherUserId) "${uid}_${otherUserId}" else "${otherUserId}_${uid}"
        val path = "conversations/$convId"
        return try {
            val docRef = db.collection("conversations").document(convId)
            val doc = docRef.get().await()
            if (!doc.exists()) {
                val payload = mapOf(
                    "id" to convId,
                    "participantIds" to listOf(uid, otherUserId),
                    "lastMessage" to "",
                    "lastMessageSenderId" to "",
                    "isReported" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(convId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        val path = "conversations/$conversationId/messages"
        val reg = db.collection("conversations").document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(ChatMessage::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun sendMessage(conversationId: String, senderName: String, text: String): Result<Unit> {
        val uid = requireUserId()
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Message cannot be empty"))
        val msgId = UUID.randomUUID().toString()
        val path = "conversations/$conversationId/messages/$msgId"
        return try {
            val msgPayload = mapOf(
                "id" to msgId,
                "conversationId" to conversationId,
                "senderId" to uid,
                "senderName" to senderName,
                "text" to cleanText,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("conversations").document(conversationId)
                .collection("messages").document(msgId).set(msgPayload).await()

            db.collection("conversations").document(conversationId).update(
                mapOf(
                    "lastMessage" to cleanText,
                    "lastMessageSenderId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // ROOMS (Audio / Video / Text)
    // -------------------------------------------------------------
    fun observeRooms(): Flow<List<Room>> = callbackFlow {
        val path = "rooms"
        val reg = db.collection("rooms")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val myUid = currentUserId()
                val list = snapshot?.toObjects(Room::class.java)?.filter {
                    !it.isPrivate || it.hostId == myUid || it.allowedMemberIds.contains(myUid) || isDeveloperAccount()
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun createRoom(
        title: String,
        description: String,
        hostName: String,
        roomType: String, // "text", "audio", "video"
        isPrivate: Boolean,
        allowedMemberIds: List<String> = emptyList()
    ): Result<String> {
        val uid = requireUserId()
        val roomId = UUID.randomUUID().toString()
        val path = "rooms/$roomId"
        return try {
            val payload = mapOf(
                "id" to roomId,
                "title" to title.trim(),
                "description" to description.trim(),
                "hostId" to uid,
                "hostName" to hostName,
                "roomType" to roomType,
                "isPrivate" to isPrivate,
                "allowedMemberIds" to (if (isPrivate) (allowedMemberIds + uid).distinct() else emptyList()),
                "participantCount" to 1,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("rooms").document(roomId).set(payload).await()
            Result.success(roomId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteRoom(roomId: String): Result<Unit> {
        val path = "rooms/$roomId"
        return try {
            db.collection("rooms").document(roomId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // POSTS & REELS
    // -------------------------------------------------------------
    fun observePosts(onlyReels: Boolean = false): Flow<List<Post>> = callbackFlow {
        val path = "posts"
        val reg = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(Post::class.java)
                    ?.filter { if (onlyReels) it.isReel else true }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun createPost(
        authorName: String,
        authorAvatar: String,
        content: String,
        mediaUrl: String = "",
        mediaType: String = "text",
        isReel: Boolean = false
    ): Result<String> {
        val uid = requireUserId()
        val postId = UUID.randomUUID().toString()
        val path = "posts/$postId"
        return try {
            val payload = mapOf(
                "id" to postId,
                "authorId" to uid,
                "authorName" to authorName,
                "authorAvatar" to authorAvatar,
                "content" to content.trim(),
                "mediaUrl" to mediaUrl.trim(),
                "mediaType" to mediaType,
                "isReel" to isReel,
                "likesCount" to 0,
                "commentsCount" to 0,
                "sharesCount" to 0,
                "savesCount" to 0,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("posts").document(postId).set(payload).await()
            Result.success(postId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        val path = "posts/$postId"
        return try {
            db.collection("posts").document(postId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeMyLikes(): Flow<Set<String>> = callbackFlow {
        val uid = currentUserId()
        if (uid == null) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }
        val path = "likes"
        val reg = db.collection("likes")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    trySend(emptySet())
                    return@addSnapshotListener
                }
                val postIds = snapshot?.documents?.mapNotNull { it.getString("postId") }?.toSet() ?: emptySet()
                trySend(postIds)
            }
        awaitClose { reg.remove() }
    }

    suspend fun toggleLike(postId: String, currentlyLiked: Boolean): Result<Unit> {
        val uid = requireUserId()
        val likeId = "${postId}_${uid}"
        val path = "likes/$likeId"
        return try {
            if (currentlyLiked) {
                db.collection("likes").document(likeId).delete().await()
                db.collection("posts").document(postId).update("likesCount", FieldValue.increment(-1)).await()
            } else {
                val payload = mapOf(
                    "id" to likeId,
                    "postId" to postId,
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                db.collection("likes").document(likeId).set(payload).await()
                db.collection("posts").document(postId).update("likesCount", FieldValue.increment(1)).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val path = "posts/$postId/comments"
        val reg = db.collection("posts").document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(Comment::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun addComment(postId: String, authorName: String, authorAvatar: String, text: String): Result<Unit> {
        val uid = requireUserId()
        val commentId = UUID.randomUUID().toString()
        val path = "posts/$postId/comments/$commentId"
        return try {
            val payload = mapOf(
                "id" to commentId,
                "postId" to postId,
                "authorId" to uid,
                "authorName" to authorName,
                "authorAvatar" to authorAvatar,
                "text" to text.trim(),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("posts").document(postId).collection("comments").document(commentId).set(payload).await()
            db.collection("posts").document(postId).update("commentsCount", FieldValue.increment(1)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // SAFETY: BLOCK & REPORT
    // -------------------------------------------------------------
    fun observeBlocks(): Flow<Set<String>> = callbackFlow {
        val uid = currentUserId()
        if (uid == null) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }
        val path = "blocks"
        val reg = db.collection("blocks")
            .whereEqualTo("blockerId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    trySend(emptySet())
                    return@addSnapshotListener
                }
                val set = snapshot?.documents?.mapNotNull { it.getString("blockedUserId") }?.toSet() ?: emptySet()
                trySend(set)
            }
        awaitClose { reg.remove() }
    }

    suspend fun blockUser(targetUserId: String): Result<Unit> {
        val uid = requireUserId()
        val blockId = "${uid}_${targetUserId}"
        val path = "blocks/$blockId"
        return try {
            val payload = mapOf(
                "id" to blockId,
                "blockerId" to uid,
                "blockedUserId" to targetUserId,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("blocks").document(blockId).set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun unblockUser(targetUserId: String): Result<Unit> {
        val uid = requireUserId()
        val blockId = "${uid}_${targetUserId}"
        val path = "blocks/$blockId"
        return try {
            db.collection("blocks").document(blockId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun submitReport(
        targetType: String, // "post", "reel", "user", "room", "chat"
        targetId: String,
        reason: String,
        details: String = "",
        reportedUserId: String = "",
        conversationId: String = ""
    ): Result<Unit> {
        val uid = requireUserId()
        val email = currentUserEmail() ?: ""
        val reportId = UUID.randomUUID().toString()
        val path = "reports/$reportId"
        return try {
            val payload = mapOf(
                "id" to reportId,
                "reporterId" to uid,
                "reporterEmail" to email,
                "targetType" to targetType,
                "targetId" to targetId,
                "reportedUserId" to reportedUserId,
                "conversationId" to conversationId,
                "reason" to reason,
                "details" to details,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("reports").document(reportId).set(payload).await()

            // If it's a reported chat, flag the conversation so developer has authorized moderation access
            if (targetType == "chat" && conversationId.isNotEmpty()) {
                db.collection("conversations").document(conversationId).update(
                    mapOf("isReported" to true, "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // DEVELOPER MODE & MODERATION
    // -------------------------------------------------------------
    suspend fun verifyDeveloperPin(enteredPin: String): Boolean {
        if (!isDeveloperAccount()) return false
        val enteredHash = hashPin(enteredPin.trim())
        val path = "app_settings/config"
        return try {
            val doc = db.collection("app_settings").document("config").get().await()
            val storedHash = doc.getString("developerPinHash")
            if (storedHash.isNullOrEmpty()) {
                // Initial bootstrap: if not set, set it to the entered pin!
                db.collection("app_settings").document("config").set(
                    mapOf(
                        "id" to "config",
                        "developerPinHash" to enteredHash,
                        "maintenanceMode" to false,
                        "minAppVersion" to "1.0.0",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                logDeveloperAction("INIT_DEV_PIN", "config", "Bootstrap developer PIN initialized")
                true
            } else {
                val matches = (enteredHash == storedHash)
                if (matches) {
                    logDeveloperAction("DEV_LOGIN", "config", "Developer PIN verified successfully")
                }
                matches
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            false
        }
    }

    suspend fun updateDeveloperPin(newPin: String): Result<Unit> {
        if (!isDeveloperAccount()) return Result.failure(SecurityException("Unauthorized"))
        val newHash = hashPin(newPin.trim())
        val path = "app_settings/config"
        return try {
            db.collection("app_settings").document("config").update(
                mapOf(
                    "developerPinHash" to newHash,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logDeveloperAction("UPDATE_DEV_PIN", "config", "Developer PIN hash updated")
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun logDeveloperAction(action: String, targetId: String, details: String) {
        if (!isDeveloperAccount()) return
        val logId = UUID.randomUUID().toString()
        val path = "audit_logs/$logId"
        try {
            val payload = mapOf(
                "id" to logId,
                "developerEmail" to (currentUserEmail() ?: DEVELOPER_EMAIL),
                "action" to action,
                "targetId" to targetId,
                "details" to details,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("audit_logs").document(logId).set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
        }
    }

    fun observeAllReports(): Flow<List<Report>> = callbackFlow {
        if (!isDeveloperAccount()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val path = "reports"
        val reg = db.collection("reports")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(Report::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun updateReportStatus(reportId: String, status: String, actionNotes: String = ""): Result<Unit> {
        if (!isDeveloperAccount()) return Result.failure(SecurityException("Unauthorized"))
        val path = "reports/$reportId"
        return try {
            db.collection("reports").document(reportId).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logDeveloperAction("MODERATE_REPORT", reportId, "Status updated to $status. Notes: $actionNotes")
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeAuditLogs(): Flow<List<AuditLog>> = callbackFlow {
        if (!isDeveloperAccount()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val path = "audit_logs"
        val reg = db.collection("audit_logs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(AuditLog::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeAnnouncements(): Flow<List<Announcement>> = callbackFlow {
        val path = "announcements"
        val reg = db.collection("announcements")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(Announcement::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun createAnnouncement(title: String, content: String, priority: String = "normal"): Result<Unit> {
        if (!isDeveloperAccount()) return Result.failure(SecurityException("Unauthorized"))
        val id = UUID.randomUUID().toString()
        val path = "announcements/$id"
        return try {
            val payload = mapOf(
                "id" to id,
                "title" to title.trim(),
                "content" to content.trim(),
                "priority" to priority,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("announcements").document(id).set(payload).await()
            logDeveloperAction("CREATE_ANNOUNCEMENT", id, "Title: $title")
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun getDeveloperStats(): Map<String, Int> {
        if (!isDeveloperAccount()) return emptyMap()
        return try {
            val usersCount = db.collection("users").get().await().size()
            val postsCount = db.collection("posts").get().await().size()
            val roomsCount = db.collection("rooms").get().await().size()
            val reportsCount = db.collection("reports").get().await().size()
            mapOf(
                "users" to usersCount,
                "posts" to postsCount,
                "rooms" to roomsCount,
                "reports" to reportsCount
            )
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
