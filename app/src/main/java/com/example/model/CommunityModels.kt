package com.example.model

import com.google.firebase.Timestamp

data class Room(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val roomType: String = "text", // "text", "audio", "video"
    val isPrivate: Boolean = false,
    val allowedMemberIds: List<String> = emptyList(),
    val participantCount: Int = 1,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class Post(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatar: String = "",
    val content: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "text", // "text", "image", "video"
    val isReel: Boolean = false,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val savesCount: Int = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class Comment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatar: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null
)

data class Like(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val createdAt: Timestamp? = null
)

data class Report(
    val id: String = "",
    val reporterId: String = "",
    val reporterEmail: String = "",
    val targetType: String = "post", // "post", "reel", "user", "room", "chat"
    val targetId: String = "",
    val reportedUserId: String = "",
    val conversationId: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "pending", // "pending", "reviewed", "resolved", "dismissed"
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class Block(
    val id: String = "",
    val blockerId: String = "",
    val blockedUserId: String = "",
    val createdAt: Timestamp? = null
)

data class AuditLog(
    val id: String = "",
    val developerEmail: String = "",
    val action: String = "",
    val targetId: String = "",
    val details: String = "",
    val createdAt: Timestamp? = null
)

data class Announcement(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val priority: String = "normal",
    val createdAt: Timestamp? = null
)

data class AppSettings(
    val id: String = "config",
    val developerPinHash: String = "", // SHA-256 hash of developer PIN
    val maintenanceMode: Boolean = false,
    val minAppVersion: String = "1.0.0",
    val updatedAt: Timestamp? = null
)
