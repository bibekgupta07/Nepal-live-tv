package com.app.nepallivetv.presentation.util

import com.app.nepallivetv.domain.model.Channel

/**
 * The upstream API serves the literal string "All" as `category` for almost
 * every channel, so the field is useless for filtering. This classifier
 * derives a meaningful category from the channel's *name* using a small
 * keyword dictionary tuned for the Nepali / South-Asian feed we actually get.
 *
 * Order of checks matters: more specific keywords win first. Anything that
 * doesn't match falls through to ENTERTAINMENT.
 */
object CategoryClassifier {

    enum class Bucket(val label: String) {
        NEWS("News"),
        SPORTS("Sports"),
        MOVIES("Movies"),
        MUSIC("Music"),
        KIDS("Kids"),
        ENTERTAINMENT("Entertainment");

        companion object {
            fun all(): List<Bucket> = entries.toList()
        }
    }

    private val newsKeywords = listOf(
        "news", "samachar", "khabar", "tv1", "headline", "abp", "aaj tak", "republic",
        "ndtv", "india today", "wion", "nepal television", "image news", "avenues",
        "kantipur tv", "himalayan", "annapurna", "galaxy", "mountain", "prime times",
        "hbc", "cnbc"
    )

    private val sportsKeywords = listOf(
        "sport", "cricket", "espn", "willow", "ten", "star sports", "sony sports",
        "dsport", "eurosport", "fight", "fox sports"
    )

    private val movieKeywords = listOf(
        "movie", "cinema", "max", "gold", "premiere", "filmy", "pix", "set max",
        "star gold", "zee cinema", "and pictures", "bflix", "world movies", "hbo",
        "mtv movies"
    )

    private val musicKeywords = listOf(
        "music", " mtv", "vh1", "9xm", "mastiii", " etc ", "b4u music"
    )

    private val kidsKeywords = listOf(
        "kid", "cartoon", "nick", "pogo", "disney", "hungama", "discovery kids",
        "nicktoons", "baby"
    )

    fun classify(channel: Channel): Bucket = classify(channel.name)

    fun classify(name: String): Bucket {
        val lower = " ${name.lowercase()} "
        return when {
            newsKeywords.any { it in lower } -> Bucket.NEWS
            sportsKeywords.any { it in lower } -> Bucket.SPORTS
            movieKeywords.any { it in lower } -> Bucket.MOVIES
            musicKeywords.any { it in lower } -> Bucket.MUSIC
            kidsKeywords.any { it in lower } -> Bucket.KIDS
            else -> Bucket.ENTERTAINMENT
        }
    }

    fun isHd(channel: Channel): Boolean =
        Regex("""\bhd\b""", RegexOption.IGNORE_CASE).containsMatchIn(channel.name)
}
