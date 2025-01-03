package com.example.alertapp.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ReleaseMediaType {
    @SerialName("movie")
    MOVIE,
    @SerialName("tv_show")
    TV_SHOW,
    @SerialName("book")
    BOOK,
    @SerialName("game")
    GAME,
    @SerialName("music")
    MUSIC,
    @SerialName("podcast")
    PODCAST,
    @SerialName("software")
    SOFTWARE;

    companion object {
        fun fromString(value: String): ReleaseMediaType {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                MOVIE // Default to MOVIE if unknown type
            }
        }

        fun toString(type: ReleaseMediaType): String {
            return when (type) {
                MOVIE -> "movie"
                TV_SHOW -> "tv_show"
                BOOK -> "book"
                GAME -> "game"
                MUSIC -> "music"
                PODCAST -> "podcast"
                SOFTWARE -> "software"
            }
        }
    }
}
