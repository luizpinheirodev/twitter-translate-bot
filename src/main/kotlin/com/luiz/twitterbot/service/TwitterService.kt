package com.luiz.twitterbot.service

import com.luiz.twitterbot.client.TwitterClient
import com.luiz.twitterbot.client.dto.TweetPost
import com.luiz.twitterbot.client.dto.TweetPostResponse
import com.luiz.twitterbot.client.dto.TweetsByUserDto
import com.luiz.twitterbot.repository.TwitterRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux


@Service
class TwitterService(
    private val twitterClient: TwitterClient,
    private val translateService: TranslateService,
    private val twitterRepository: TwitterRepository
) {

    @Scheduled(fixedRate = 5000)
    fun executeBotScheduled() = this.findTweetsByUserId("44196397")
        .subscribe { LOGGER.info("Starting bot execution") }

    fun findTweetsByUserId(userId: String): Flux<TweetPostResponse> =
        twitterRepository.findTopByOrderByTweetIdDesc()
            .flatMap { twitterClient.getRecentTweets(userId, it.tweetId) }
            .flatMap { validateResponse(it) }
            .map { it!!.data }
            .flatMapMany { it?.toFlux() }
            .flatMap { translateService.translateText(it) }
            .flatMap { twitterRepository.save(it) }
            .map { TweetPost(it.portugueseText) }
            .flatMap { postTweet(it) }
            .doOnRequest { LOGGER.info("Getting tweets from userId $userId") }
            .doOnTerminate { LOGGER.info("Success on getting tweets from userId $userId") }
            .doOnError { LOGGER.info("Error on getting tweets from userId $userId") }

    fun postTweet(tweetPost: TweetPost): Mono<TweetPostResponse> =
        twitterClient.postTweet(tweetPost)
            .doOnRequest { LOGGER.info("Posting tweets from userId ") }
            .doOnSuccess { LOGGER.info("Success on posting tweets from userId ") }
            .doOnError { LOGGER.info("Error on posting tweets from userId ") }

    private fun validateResponse(tweetsByUserDto: TweetsByUserDto?) =
        if (tweetsByUserDto?.data == null) Mono.empty() else Mono.just(tweetsByUserDto)

    companion object {
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }
}