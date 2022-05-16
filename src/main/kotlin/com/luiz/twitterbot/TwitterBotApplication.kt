package com.luiz.twitterbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class TwitterBotApplication

fun main(args: Array<String>) {
	runApplication<TwitterBotApplication>(*args)
}
