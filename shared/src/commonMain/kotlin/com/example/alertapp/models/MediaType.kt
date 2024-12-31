package com.example.alertapp.models

import kotlinx.serialization.Serializable

@Serializable
enum class MediaType {
    MUSIC,
    MOVIE,
    TV_SHOW,
    BOOK,
    GAME,
    PODCAST,
    UNKNOWN;

    fun getDisplayName(): String = when (this) {
        MUSIC -> "Music"
        MOVIE -> "Movie"
        TV_SHOW -> "TV Show"
        BOOK -> "Book"
        GAME -> "Game"
        PODCAST -> "Podcast"
        UNKNOWN -> "Unknown"
    }
}
