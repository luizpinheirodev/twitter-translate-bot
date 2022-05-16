package com.luiz.twitterbot.configuration

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.DefaultUriBuilderFactory
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider

@Configuration
class WebClientConfiguration {

    @Bean
    fun clientTwitter(
        builder: WebClient.Builder,
        @Value("\${application.api.twitter.url}") endpoint: String,
        @Value("\${application.api.twitter.poll-name}") poolName: String,
        @Value("\${application.api.generic.connect-timeout-ms:10000}") connectTimeout: Int,
        @Value("\${application.api.generic.read-timeout-seconds:10}") readTimeout: Int,
        @Value("\${application.api.generic.write-timeout-seconds:10}") writeTimeout: Int,
        @Value("\${application.api.generic.max-connections:500}") maxConnection: Int
    ): WebClient {
        return getWebClient(builder, endpoint, connectTimeout, readTimeout, writeTimeout, maxConnection, poolName)
    }

    @Bean
    fun clientTranslate(
        builder: WebClient.Builder,
        @Value("\${application.api.deep-translate.url}") endpoint: String,
        @Value("\${application.api.deep-translate.poll-name}") poolName: String,
        @Value("\${application.api.generic.connect-timeout-ms:10000}") connectTimeout: Int,
        @Value("\${application.api.generic.read-timeout-seconds:10}") readTimeout: Int,
        @Value("\${application.api.generic.write-timeout-seconds:10}") writeTimeout: Int,
        @Value("\${application.api.generic.max-connections:500}") maxConnection: Int
    ): WebClient {
        return getWebClient(builder, endpoint, connectTimeout, readTimeout, writeTimeout, maxConnection, poolName)
    }

    private fun getWebClient(
        builder: WebClient.Builder, endpoint: String, connectTimeout: Int,
        readTimeout: Int, writeTimeout: Int, maxConnection: Int,
        poolName: String
    ): WebClient {
        return builder
            .uriBuilderFactory(uriBuilderFactory(endpoint))
            .clientConnector(clientHttpConnector(connectTimeout, readTimeout, writeTimeout, maxConnection, poolName))
            .build()
    }

    private fun uriBuilderFactory(baseUri: String): DefaultUriBuilderFactory {
        return DefaultUriBuilderFactory(baseUri)
    }

    private fun clientHttpConnector(
        connectTimeout: Int, readTimeout: Int,
        writeTimeout: Int, maxConnection: Int, poolName: String
    ): ClientHttpConnector {

        val httpClient = HttpClient.create(ConnectionProvider.create(poolName, maxConnection)).wiretap(false)
            .tcpConfiguration { client ->
                client.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                    .doOnConnected { conn ->
                        conn
                            .addHandlerLast(ReadTimeoutHandler(readTimeout))
                            .addHandlerLast(WriteTimeoutHandler(writeTimeout))
                    }
            }

        return ReactorClientHttpConnector(httpClient)
    }

}