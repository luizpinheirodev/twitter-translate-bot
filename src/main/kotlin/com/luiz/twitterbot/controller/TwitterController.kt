package com.luiz.twitterbot.controller

import com.luiz.twitterbot.client.dto.TweetPost
import com.luiz.twitterbot.client.dto.TweetPostResponse
import com.luiz.twitterbot.service.TwitterService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class TwitterController(
    private val twitterService: TwitterService
) {

    @GetMapping("user/{userId}")
    fun getTweetByUserId(@PathVariable userId: String): Flux<TweetPostResponse> =
        twitterService.findTweetsByUserId(userId)
            .doAfterTerminate { LOGGER.info("Finalizing all process from Rest call") }

    @PostMapping("user")
    fun post(): Mono<TweetPostResponse> {
        return twitterService.postTweet(TweetPost("testezin"))
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

}