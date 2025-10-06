package com.demo.ai.chat

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform