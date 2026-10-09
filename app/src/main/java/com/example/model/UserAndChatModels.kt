package com.example.model

import com.google.firebase.Timestamp

data class UserProfile(
    val userId: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val isPrivate: Boolean = false,
    val locationSharingEnabled: Boolean = false,
    val approxCity: String = "",
    val language: String = "en",
    val isDataSaver: Boolean = false,
    val isDeveloper: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class Friendship(
    val id: String = "",
    val requesterId: String = "",
    val receiverId: String = "",
    val status: String = "pending", // "pending", "accepted", "rejected"
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class Conversation(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageSenderId: String = "",
    val isReported: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class ChatMessage(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null
)
