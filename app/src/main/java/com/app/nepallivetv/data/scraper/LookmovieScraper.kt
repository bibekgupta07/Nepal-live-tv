package com.app.nepallivetv.data.scraper

import com.app.nepallivetv.analytics.RemoteConfigService
import com.app.nepallivetv.analytics.Telemetry
import com.app.nepallivetv.domain.model.Episode
import com.app.nepallivetv.domain.model.MediaDetail
import com.app.nepallivetv.domain.model.MediaItem
import com.app.nepallivetv.domain.model.MediaKind
import com.app.nepallivetv.domain.model.Season
import com.app.nepallivetv.domain.model.StreamSet
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * On-device port of the FastAPI backend's LookmovieMediaSource. Handles
 * everything except the home feed (that comes from raw.gh JSON, refreshed
 * every 6h by the scrape workflow). All public methods are suspend and safe
 * to call from any dispatcher — they offload IO via [ScraperHttpClient].
 */
class LookmovieScraper(
    private val http: ScraperHttpClient,
    private val remoteConfig: RemoteConfigService,
    private val telemetry: Telemetry,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val base: String get() = remoteConfig.lookmovieBaseUrl.trimEnd('/')

    // ---- detail -----------------------------------------------------------

    suspend fun getDetail(kind: MediaKind, slug: String): MediaDetail? {
        val pathKind = if (kind == MediaKind.MOVIE) "movies" else "shows"
        return if (kind == MediaKind.SHOW) {
            coroutineScope {
                val playHtml = async { http.getText("$base/shows/play/$slug") }
                val viewHtml = async { http.getText("$base/shows/view/$slug") }
                val play = playHtml.await().orEmpty()
                val view = viewHtml.await().orEmpty()
                if (play.isEmpty() && view.isEmpty()) return@coroutineScope null
                val detail = parseDetail(kind, slug, if (view.isNotEmpty()) view else play)
                val seasons = if (play.isNotEmpty()) parseSeasonsFromPlayHtml(play) else emptyList()
                val upstreamId =
                    if (play.isNotEmpty()) extractStorageInt(play, "id_show") else 0L
                detail.copy(
                    seasons = seasons,
                    upstreamId = if (upstreamId != 0L) upstreamId.toString() else "",
                )
            }
        } else {
            val html = http.getText("$base/$pathKind/view/$slug") ?: return null
            parseDetail(kind, slug, html)
        }
    }

    private fun parseDetail(kind: MediaKind, slug: String, html: String): MediaDetail {
        val doc = Jsoup.parse(html)

        var title = slug
        var year: Int? = null
        val h1 = doc.selectFirst("h1.bd-hd") ?: doc.selectFirst("h1")
        if (h1 != null) {
            val span = h1.selectFirst("span")
            if (span != null) {
                Regex("\\d{4}").find(span.text())?.let { year = it.value.toInt() }
                span.remove()
            }
            title = h1.text().trim().ifEmpty { slug }
        }

        val genres = mutableListOf<String>()
        doc.selectFirst(".genres")?.text()?.let { line ->
            line.split(",").map { it.trim() }.forEach { p ->
                if (p.matches(Regex("\\d{4}"))) {
                    if (year == null) year = p.toIntOrNull()
                } else if (p.isNotEmpty()) {
                    genres += p
                }
            }
        }

        val overview = doc.selectFirst(
            ".movie-summary, .overview, .description, .synopsis, [class*=overview]",
        )?.text().orEmpty()

        val cast = mutableListOf<String>()
        doc.select(".actor__card").forEach {
            val n = it.text().trim()
            if (n.isNotEmpty()) cast += n
        }
        if (cast.isEmpty()) {
            doc.selectFirst(".cast, .actors")?.text()?.trim()?.let { raw ->
                val cleaned = if (raw.lowercase().startsWith("stars:"))
                    raw.substringAfter(":").trim() else raw
                if (cleaned.isNotEmpty()) cast += cleaned
            }
        }

        val rating = doc.selectFirst(".rate")?.text()?.let {
            Regex("(\\d+(?:\\.\\d+)?)").find(it)?.value?.toDoubleOrNull()
        }

        val runtime = Regex("\\b(\\d{1,3})\\s*min\\b", RegexOption.IGNORE_CASE)
            .find(html)?.let { "${it.groupValues[1]} min" }

        var poster = ""
        var backdrop = ""
        Regex("/images/[bp]/w(\\d+)/[a-f0-9]+\\.webp").findAll(html).forEach { m ->
            val path = m.value
            val size = m.groupValues[1].toInt()
            if (poster.isEmpty() && "/p/" in path && size >= 300) poster = path
            if (backdrop.isEmpty() && "/b/" in path && size >= 780) backdrop = path
        }
        if (poster.isEmpty()) {
            poster = doc.selectFirst("meta[property=og:image]")?.attr("content").orEmpty()
        }
        if (backdrop.isEmpty()) backdrop = poster

        val playerUrl = absolute("/${if (kind == MediaKind.MOVIE) "movies" else "shows"}/view/$slug")

        return MediaDetail(
            id = slug,
            kind = kind,
            title = title,
            year = year,
            poster = absolute(poster),
            backdrop = absolute(backdrop),
            overview = overview,
            genres = genres,
            cast = cast,
            runtime = runtime,
            rating = rating,
            playerUrl = playerUrl,
            upstreamId = "",
            seasons = emptyList(),
        )
    }

    // ---- streams ----------------------------------------------------------

    suspend fun getMovieStream(slug: String): StreamSet? = fetchStreams("movie", slug, null)

    suspend fun getEpisodeStream(slug: String, idEpisode: Long): StreamSet? =
        fetchStreams("show", slug, idEpisode)

    private suspend fun fetchStreams(kind: String, slug: String, idEpisode: Long?): StreamSet? {
        val playPath = when (kind) {
            "movie" -> "/movies/play/$slug"
            "show" -> "/shows/play/$slug?id_episode=$idEpisode"
            else -> return null
        }
        val playHtml = http.getText("$base$playPath") ?: run {
            telemetry.scraperDriftDetected("lookmovie_play", "fetch_failed")
            return null
        }
        val hash = extractStorageStr(playHtml, "hash")
        val expires = extractStorageInt(playHtml, "expires")
        if (hash.isEmpty() || expires == 0L) {
            telemetry.scraperDriftDetected("lookmovie_play", "hash_or_expires_missing")
            return null
        }
        val referer = "$base$playPath"
        val apiPath = if (kind == "movie") {
            val idMovie = extractStorageInt(playHtml, "id_movie")
            if (idMovie == 0L) {
                telemetry.scraperDriftDetected("lookmovie_play", "id_movie_missing")
                return null
            }
            "/api/v1/security/movie-access?id_movie=$idMovie&hash=$hash&expires=$expires"
        } else {
            "/api/v1/security/episode-access?id_episode=$idEpisode&hash=$hash&expires=$expires"
        }

        val body = http.getText("$base$apiPath", mapOf("Referer" to referer)) ?: return null
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val success = root["success"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() == true
        if (!success) {
            telemetry.scraperDriftDetected("lookmovie_security_api", "success_false")
            return null
        }

        val streams = root["streams"] as? JsonObject ?: return null
        fun pick(vararg keys: String): String {
            for (k in keys) {
                streams[k]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() }?.let { return it }
            }
            return ""
        }

        val p480 = pick("480p", "480")
        val p720 = pick("720p", "720")
        val p1080 = pick("1080p", "1080")
        val best = p1080.ifEmpty { p720.ifEmpty { p480 } }
        return StreamSet(p480 = p480, p720 = p720, p1080 = p1080, best = best)
    }

    // ---- search -----------------------------------------------------------

    suspend fun search(query: String): List<MediaItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()

        val pages = (1..SEARCH_PAGES).flatMap { page ->
            listOf("movie" to "/movies?page=$page", "show" to "/shows?page=$page")
        }
        val results = coroutineScope {
            pages.map { (kind, path) ->
                async {
                    val html = http.getText("$base$path") ?: return@async emptyList()
                    parseGrid(html, kind)
                }
            }.awaitAll()
        }

        val out = mutableListOf<MediaItem>()
        val seen = mutableSetOf<String>()
        for (page in results) {
            for (item in page) {
                if (item.id in seen) continue
                if (q in item.title.lowercase()) {
                    seen += item.id
                    out += item
                    if (out.size >= SEARCH_MAX_RESULTS) return out
                }
            }
        }
        return out
    }

    // ---- grid parsing -----------------------------------------------------

    private fun parseGrid(html: String, kindFilter: String? = null): List<MediaItem> {
        val doc = Jsoup.parse(html)
        val items = mutableListOf<MediaItem>()
        val seen = mutableSetOf<String>()
        for (a in doc.select("a[href*=/movies/view/], a[href*=/shows/view/]")) {
            val href = a.attr("href")
            val m = VIEW_HREF_RE.find(href) ?: continue
            val kindPath = m.groupValues[1]
            val slug = m.groupValues[2].substringBefore("?")
            val kind = if (kindPath == "movies") "movie" else "show"
            if (kindFilter != null && kind != kindFilter) continue
            if (slug in seen) continue
            seen += slug
            items += buildItem(a, slug, kind)
        }
        return items
    }

    private fun buildItem(anchor: Element, slug: String, kind: String): MediaItem {
        val card = cardFor(anchor)
        var titleRaw = ""
        for (sel in listOf(".slide-item__title", "h1", "h2", "h3", "h4", "h5", "h6")) {
            val el = card.selectFirst(sel)
            if (el != null && el.text().trim().isNotEmpty()) {
                titleRaw = el.text().trim(); break
            }
        }
        if (titleRaw.isEmpty()) titleRaw = anchor.attr("title").trim()
        if (titleRaw.isEmpty()) titleRaw = card.selectFirst("img")?.attr("alt").orEmpty().trim()
        if (titleRaw.isEmpty()) {
            titleRaw = slug.replace(Regex("^\\d+-"), "")
                .replace("-", " ")
                .split(" ")
                .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
        }

        var title = titleRaw
        var year: Int? = null
        TITLE_YEAR.find(titleRaw)?.let {
            title = it.groupValues[1].trim()
            year = it.groupValues[2].toInt()
        }
        if (year == null) {
            card.selectFirst(".year")?.text()?.let { txt ->
                Regex("\\d{4}").find(txt)?.let { year = it.value.toInt() }
            }
        }

        var poster = ""
        var backdrop = ""
        card.selectFirst("img")?.let { img ->
            backdrop = img.attr("data-src-landscape").ifEmpty {
                img.attr("data-src").ifEmpty { img.attr("src") }
            }
            poster = img.attr("data-src-portrait").ifEmpty { backdrop }
            if (poster.startsWith("data:")) { poster = ""; backdrop = "" }
        }

        return MediaItem(
            id = slug,
            kind = if (kind == "movie") MediaKind.MOVIE else MediaKind.SHOW,
            title = title,
            year = year,
            poster = absolute(poster),
            backdrop = absolute(backdrop),
        )
    }

    private fun cardFor(anchor: Element): Element {
        var node: Element? = anchor.parent()
        while (node != null) {
            val classes = node.classNames()
            if (classes.any { it.startsWith("movie-item") || it.startsWith("slide-item") }) {
                return node
            }
            node = node.parent()
        }
        return anchor.parent()?.parent()?.parent() ?: anchor
    }

    // ---- play-page helpers ------------------------------------------------

    private fun parseSeasonsFromPlayHtml(html: String): List<Season> {
        val bySeason = mutableMapOf<Int, MutableList<Episode>>()
        for (m in SEASON_BLOCK_RE.findAll(html)) {
            val seasonNum = m.groupValues[5].toInt()
            val titleRaw = m.groupValues[1].replace("\\'", "'").trim()
            val episode = Episode(
                idEpisode = m.groupValues[4].toLong(),
                season = seasonNum,
                number = m.groupValues[3].toInt(),
                title = titleRaw,
                overview = null,
                still = null,
            )
            bySeason.getOrPut(seasonNum) { mutableListOf() } += episode
        }
        return bySeason.toSortedMap().map { (n, eps) ->
            Season(number = n, episodes = eps.sortedBy { it.number })
        }
    }

    private fun extractStorageStr(html: String, key: String): String {
        val pattern = Regex("${Regex.escape(key)}\\s*:\\s*['\"]([^'\"]+)['\"]")
        return pattern.find(html)?.groupValues?.get(1).orEmpty()
    }

    private fun extractStorageInt(html: String, key: String): Long {
        val pattern = Regex("${Regex.escape(key)}\\s*:\\s*(\\d+)")
        return pattern.find(html)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    }

    private fun absolute(url: String?): String {
        if (url.isNullOrEmpty()) return ""
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        if (url.startsWith("//")) return "https:$url"
        if (url.startsWith("/")) return base + url
        return "$base/$url"
    }

    private companion object {
        const val SEARCH_PAGES = 5
        const val SEARCH_MAX_RESULTS = 60

        val TITLE_YEAR = Regex("^(.*?)\\s*\\((\\d{4})\\)\\s*$")
        val VIEW_HREF_RE = Regex("^/(movies|shows)/view/([^?]+)")

        val SEASON_BLOCK_RE = Regex(
            "\\{\\s*" +
                "(?:title\\s*:\\s*['\"]((?:[^'\"\\\\]|\\\\.)*)['\"]\\s*,\\s*)?" +
                "(?:index\\s*:\\s*['\"](\\d+)['\"]\\s*,\\s*)?" +
                "episode\\s*:\\s*['\"](\\d+)['\"]\\s*,\\s*" +
                "id_episode\\s*:\\s*(\\d+)\\s*,\\s*" +
                "season\\s*:\\s*['\"](\\d+)['\"]"
        )
    }
}
