package com.luiz.twitterbot.service

import com.luiz.twitterbot.client.TranslateClient
import com.luiz.twitterbot.client.dto.TranslateRequestDto
import com.luiz.twitterbot.client.dto.Tweet
import com.luiz.twitterbot.domain.TwitterDocument
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
class TranslateService(
    private val translateClient: TranslateClient,
    @Value("\${application.api.deep-translate.source}") val source: String,
    @Value("\${application.api.deep-translate.target}") val target: String
) {

    fun translateText(tweet: Tweet): Mono<TwitterDocument> =
        translateClient.getTweets(TranslateRequestDto(tweet.text, source, target))
            .map {
                TwitterDocument(
                    tweetId = tweet.id,
                    userId = tweet.author_id,
                    portugueseText = it.data.translations.translatedText,
                    originalText = tweet.text,
                    tweetCreatedAt = LocalDateTime.parse(tweet.created_at.removeSuffix("Z"))
                )
            }
            .doOnRequest { LOGGER.info("Getting translate of text ${tweet.text}") }
            .doOnSuccess { LOGGER.info("Success on getting translate of text  ${tweet.text}") }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }
}