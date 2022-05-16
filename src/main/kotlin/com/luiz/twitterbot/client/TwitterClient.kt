package com.luiz.twitterbot.client

import com.luiz.twitterbot.client.dto.TweetPost
import com.luiz.twitterbot.client.dto.TweetPostResponse
import com.luiz.twitterbot.client.dto.TweetsByUserDto
import com.luiz.twitterbot.exception.InfrastructureException
import com.luiz.twitterbot.exception.NotFoundException
import com.luiz.twitterbot.util.TwitterOauthHeaderGenerator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration
import javax.naming.ServiceUnavailableException


@Component
class TwitterClient(
    @Qualifier("clientTwitter") val client: WebClient,
    @Value("\${application.api.twitter.token}") val token: String,
    @Value("\${application.api.twitter.api-key}") val apiKey: String,
    @Value("\${application.api.twitter.api-key-secret}") val apiKeySecret: String,
    @Value("\${application.api.twitter.accessToken}") val accessToken: String,
    @Value("\${application.api.twitter.accessTokenSecret}") val accessTokenSecret: String,
    @Value("\${application.generic.maxRetries:1}") val maxRetries: Long,
    @Value("\${application.generic.timeout:15000}") timeoutMillis: Long,
    @Value("\${application.generic.firstAttemptDuration:1000}") firstAttemptDuration: Long,
    @Value("\${application.generic.lastAttemptDuration:5000}") lastAttemptDuration: Long,
) {

    private val timeout: Duration = Duration.ofMillis(timeoutMillis)
    private val firstAttemptDuration: Duration = Duration.ofMillis(firstAttemptDuration)
    private val lastAttemptDuration: Duration = Duration.ofMillis(lastAttemptDuration)

    fun getRecentTweets(userId: String, tweetId: String): Mono<TweetsByUserDto> {
        val headers = buildHttpHeaders()
        return client
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(URI_RECENT_TWEETS)
                    .queryParam("query", "from:$userId -is:reply -is:retweet")
                    .queryParam("tweet.fields", "created_at,author_id")
                    .queryParam("since_id", tweetId)
                    .build(userId)
            }
            .headers { httpHeaders -> httpHeaders.putAll(headers) }
            .retrieve()
            .onStatus({ it.is5xxServerError },
                { Mono.error(InfrastructureException(String.format(FORMAT_MESSAGE, userId, it.statusCode().value()))) })
            .onStatus({ it.is4xxClientError },
                { Mono.error(NotFoundException(String.format(FORMAT_MESSAGE, userId, it.statusCode().value()))) })
            .bodyToMono(TweetsByUserDto::class.java)
            .timeout(timeout, Mono.error(ServiceUnavailableException(String.format(TIMEOUT_MESSAGE, userId))))
            .retryWhen(
                Retry.backoff(maxRetries, firstAttemptDuration)
                    .maxBackoff(lastAttemptDuration)
                    .filter { throwable -> throwable is HttpServerErrorException.InternalServerError }
                    .doAfterRetry { LOGGER.info(RETRY_MESSAGE, userId, it.totalRetries() + 1) })
            .switchIfEmpty(Mono.empty())
            .onErrorResume {
                LOGGER.error(String.format(ERROR_MESSAGE, userId))
                Mono.error(it)
            }
    }

    fun postTweet(tweetPost: TweetPost): Mono<TweetPostResponse> {
        val headers = buildOauthHttpHeaders(apiKey, apiKeySecret, accessToken, accessTokenSecret)
        return client
            .post()
            .uri(URI_POST)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(tweetPost))
            .headers { httpHeaders -> httpHeaders.putAll(headers) }
            .retrieve()
            .onStatus({ it.is5xxServerError },
                { Mono.error(InfrastructureException(String.format(FORMAT_MESSAGE, it.statusCode().value()))) })
            .onStatus({ it.is4xxClientError },
                { Mono.error(NotFoundException(String.format(FORMAT_MESSAGE, it.statusCode().value()))) })
            .bodyToMono(TweetPostResponse::class.java)
            .timeout(timeout, Mono.error(ServiceUnavailableException(String.format(TIMEOUT_MESSAGE, tweetPost.text))))
            .retryWhen(
                Retry.backoff(maxRetries, firstAttemptDuration)
                    .maxBackoff(lastAttemptDuration)
                    .filter { throwable -> throwable is HttpServerErrorException.InternalServerError }
                    .doAfterRetry { LOGGER.info(RETRY_MESSAGE, it.totalRetries() + 1) })
            .switchIfEmpty(Mono.empty())
            .onErrorResume { t ->
                LOGGER.error(ERROR_MESSAGE, tweetPost.text)
                Mono.error(t)
            }
    }

    fun buildHttpHeaders(): HttpHeaders {
        val httpHeaders = HttpHeaders()
        httpHeaders.add("Authorization", "Bearer $token")
        return httpHeaders
    }

    fun buildOauthHttpHeaders(consumerKey: String, consumerSecret: String, token: String, tokenSecret: String): HttpHeaders {
        val httpHeaders = HttpHeaders()
        val twitterOauthHeaderGenerator = TwitterOauthHeaderGenerator(consumerKey, consumerSecret, token, tokenSecret)
        val requestParams: Map<String, String> = HashMap()
        val header = twitterOauthHeaderGenerator.generateHeader(
            "POST", "https://api.twitter.com/2/tweets", requestParams
        )

        httpHeaders.add("Authorization", header)
        return httpHeaders
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
        private const val URI_RECENT_TWEETS = "/tweets/search/recent"
        private const val URI_POST = "/tweets"
        private const val TIMEOUT_MESSAGE = "Timeout has been exceeded when calling the Twitter API for %s"
        private const val RETRY_MESSAGE = "Unable to connect to the Twitter API - Retry attempt: %d"
        private const val FORMAT_MESSAGE = "Error %d on Twitter Api for %s"
        private const val ERROR_MESSAGE = "ERROR CALL Twitter API for %s"
    }

}

//https://api.twitter.com/2/tweets/search/recent?query=from%3Aluitj%20new%20-is%3Aretweet&max_results=10