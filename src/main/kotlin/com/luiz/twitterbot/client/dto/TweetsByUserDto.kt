package com.luiz.twitterbot.client.dto

class TweetsByUserDto(
    val data: List<Tweet>? = null,
    val meta: Meta? = null
)

class Tweet(
    val id: String,
    val author_id: String,
    val text: String,
    val created_at: String
)

class Meta(
    val result_count: String
)