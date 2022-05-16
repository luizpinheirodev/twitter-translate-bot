package com.luiz.twitterbot.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document
data class TwitterDocument(
    @Id
    val id: String? = null,
    val tweetId: String,
    val userId: String,
    val portugueseText: String,
    val originalText: String,
    val tweetCreatedAt: LocalDateTime,
    val createdAt: LocalDateTime = LocalDateTime.now()
)