package com.example.pompeiarunners

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform