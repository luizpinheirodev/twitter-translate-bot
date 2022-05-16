package com.luiz.twitterbot.client.dto

class TweetPost(val text: String)

class TweetPostResponse(val data: TweetPostResponseData)
class TweetPostResponseData(val id: String, val text: String)