package com.luiz.twitterbot.client.dto

class TranslateRequestDto(
    val q: String,
    val source: String,
    val target: String
)

class TranslateResponseDto(val data: Translations)

class Translations(val translations: TranslationText)

class TranslationText(val translatedText: String)