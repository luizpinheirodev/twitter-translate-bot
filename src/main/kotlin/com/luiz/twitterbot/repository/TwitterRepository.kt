package com.luiz.twitterbot.repository

import com.luiz.twitterbot.domain.TwitterDocument
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono

interface TwitterRepository: ReactiveMongoRepository<TwitterDocument, String>{
    fun findTopByOrderByTweetIdDesc(): Mono<TwitterDocument>
}
