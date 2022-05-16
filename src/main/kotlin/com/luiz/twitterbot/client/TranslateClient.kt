package com.luiz.twitterbot.client

import com.luiz.twitterbot.client.dto.TranslateRequestDto
import com.luiz.twitterbot.client.dto.TranslateResponseDto
import com.luiz.twitterbot.client.dto.TweetsByUserDto
import com.luiz.twitterbot.exception.InfrastructureException
import com.luiz.twitterbot.exception.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration
import javax.naming.ServiceUnavailableException

@Component
class TranslateClient(
    @Qualifier("clientTranslate") val client: WebClient,
    @Value("\${application.api.deep-translate.host}") val host: String,
    @Value("\${application.api.deep-translate.key}") val key: String,
    @Value("\${application.generic.maxRetries:1}") val maxRetries: Long,
    @Value("\${application.generic.timeout:15000}") timeoutMillis: Long,
    @Value("\${application.generic.firstAttemptDuration:1000}") firstAttemptDuration: Long,
    @Value("\${application.generic.lastAttemptDuration:5000}") lastAttemptDuration: Long,
) {

    private val timeout: Duration = Duration.ofMillis(timeoutMillis)
    private val firstAttemptDuration: Duration = Duration.ofMillis(firstAttemptDuration)
    private val lastAttemptDuration: Duration = Duration.ofMillis(lastAttemptDuration)

    fun getTweets(translateRequestDto: TranslateRequestDto): Mono<TranslateResponseDto> {
        val headers = buildHttpHeaders()
        return client
            .post()
            .uri(URI)
            .body(BodyInserters.fromValue(translateRequestDto))
            .headers { httpHeaders -> httpHeaders.putAll(headers) }
            .retrieve()
            .onStatus({ it.is5xxServerError },
                { Mono.error(InfrastructureException(String.format(FORMAT_MESSAGE, translateRequestDto.q, it.statusCode().value()))) })
            .onStatus({ it.is4xxClientError },
                { Mono.error(NotFoundException(String.format(FORMAT_MESSAGE, translateRequestDto.q, it.statusCode().value()))) })
            .bodyToMono(TranslateResponseDto::class.java)
            .timeout(timeout, Mono.error(ServiceUnavailableException(String.format(TIMEOUT_MESSAGE, translateRequestDto.q))))
            .retryWhen(
                Retry.backoff(maxRetries, firstAttemptDuration)
                    .maxBackoff(lastAttemptDuration)
                    .filter { throwable -> throwable is HttpServerErrorException.InternalServerError }
                    .doAfterRetry { LOGGER.info(RETRY_MESSAGE, translateRequestDto.q, it.totalRetries() + 1) })
            .switchIfEmpty(Mono.empty())
            .onErrorResume {
                LOGGER.error(String.format(ERROR_MESSAGE, translateRequestDto.q))
                Mono.error(it)
            }
    }

    fun buildHttpHeaders(): HttpHeaders {
        val httpHeaders = HttpHeaders()
        httpHeaders.add("content-type", "application/json")
        httpHeaders.add("X-RapidAPI-Host", host)
        httpHeaders.add("X-RapidAPI-Key", key)
        return httpHeaders
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
        private const val URI = "/language/translate/v2"
        private const val TIMEOUT_MESSAGE = "Timeout has been exceeded when calling the Translate API for %s"
        private const val RETRY_MESSAGE = "Unable to connect to the Translate API for %s - Retry attempt: %d"
        private const val FORMAT_MESSAGE = "Error %d on Translate Api for %s"
        private const val ERROR_MESSAGE = "ERROR CALL Translate API for %s"
    }
}