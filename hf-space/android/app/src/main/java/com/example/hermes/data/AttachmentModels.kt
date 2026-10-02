package com.example.hermes.data

import kotlinx.serialization.Serializable

@Serializable
data class ChatAttachment(
    val id: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long = 0,
    val localUri: String? = null,
    val base64Data: String? = null,
    val isImage: Boolean = false,
    val extractedText: String? = null
)
