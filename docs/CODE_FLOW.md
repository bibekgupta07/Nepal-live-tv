# Code Flow

Runtime sequences for the v3 architecture. Each section traces a real user action through every component that handles it. ASCII diagrams + a short prose explanation.

> Where things live:
> - Cold = on the GitHub Actions runner, every 6h
> - Hot = on the user's device, at click time
> - Telemetry = Firebase SDK, always passive

## 1. Cold scrape — GitHub Actions cycle

Runs every 6 hours via cron and on every push to `scraper/**` for testing.

```
cron trigger                              actions runner
     │                                          │
     ▼                                          ▼
┌──────────────┐        ┌──────────────────────────────────────┐
│ scrape.yml   │───▶    │ checkout repo                        │
└──────────────┘        │ setup-python@v5                      │
                        │ pip install httpx beautifulsoup4     │
                        │                                       │
                        │ python scraper/channels.py            │
                        │   ├── GET techjail/channels.php       │
                        │   ├── parallel probe each m3u8 URL    │
                        │   │   (drops dead channels)           │
                        │   └── write data/channels.json        │
                        │                                       │
                        │ python scraper/movies_home.py         │
                        │   ├── GET lookmovie2.to/             │
                        │   ├── GET lookmovie2.to/movies        │
                        │   ├── GET lookmovie2.to/shows         │
                        │   ├── GET /genre/action, comedy, …    │
                        │   ├── parse all into HomeFeed shape   │
                        │   └── write data/movies-home.json     │
                        │                                       │
                        │ git diff data/                        │
                        │ if changed: commit + push as bot      │
                        └──────────────────────────────────────┘
                                          │
                                          ▼
                            commit appears on main
                                          │
                                          ▼
                         raw.githubusercontent.com sees new SHA
                            (5-min CDN cache TTL)
```

Failure handling: any unhandled exception fails the workflow run and is visible in the Actions tab. The previous `data/*.json` stays untouched, so users keep seeing the last good catalog. We add a Crashlytics-style "scrape job failed" notification later if it becomes a real concern.

## 2. App cold start

```
APK launch
     │
     ▼
┌─────────────────────────────────────────┐
│ Application.onCreate                    │
│ ├── start Koin (DI graph)               │
│ ├── FirebaseApp.initialize              │
│ ├── Crashlytics ready (catches throws)  │
│ ├── RemoteConfig.fetchAndActivate (bg)  │
│ └── Analytics.logEvent("app_open")      │
└───────────────┬─────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────┐
│ MainActivity → AppNavigation            │
│ start destination: HomeRoute            │
│ (no more PreLoginGraph / SplashRoute    │
│  gating — auth is removed)              │
└───────────────┬─────────────────────────┘
                │
                ▼
HomeScreen mounts        MoviesScreen mounts (when user taps tab)
     │                              │
     ▼                              ▼
ChannelRepository.getChannels()     MediaRepository.getHome()
     │                              │
     ▼                              ▼
CatalogApi.fetchChannelsJson()      CatalogApi.fetchMoviesHomeJson()
     │                              │
     ▼                              ▼
OkHttp GET raw.githubusercontent.com/<owner>/NepalLiveTv/main/data/{channels,movies-home}.json
     │
     ▼
JSON → kotlinx.serialization → domain models → StateFlow → UI
```

Both fetches are cached for ~5 minutes by OkHttp's cache (matches the GitHub CDN TTL), so tab switches don't re-hit the network.

## 3. Playing a movie

The most-exercised hot path.

```
User taps a movie poster on MoviesScreen
     │
     ▼
nav: MovieDetailRoute(kind="movie", slug="<slug>")
     │
     ▼
MovieDetailScreen mounts → MoviesViewModel.loadDetail(MOVIE, slug)
     │
     ▼
MediaRepository.getDetail(MOVIE, slug)
     │
     ▼
LookmovieDetailScraper.fetchMovie(slug)        ← on-device, Jsoup
     │
     ├── OkHttp GET lookmovie2.to/movies/view/<slug>
     ├── extract h1.bd-hd (title, year)
     ├── extract .genres, .rate, .actor__card
     ├── extract first /images/p/w500 (poster)
     └── extract first /images/b/w1280 (backdrop)
     │
     ▼
return MediaDetail → UI renders backdrop hero + Play button
     │
     │ user taps Play
     ▼
nav: MoviePlayerRoute(kind="movie", slug, title, idEpisode=null)
     │
     ▼
MoviePlayerScreen mounts → MoviePlayerViewModel.load("movie", slug, null)
     │
     ▼
MediaRepository.getMovieStream(slug)
     │
     ▼
LookmovieStreamScraper.fetchMovieStream(slug)  ← on-device, two-step
     │
     ├── 1. OkHttp GET lookmovie2.to/movies/play/<slug>
     │      └── extract id_movie, hash, expires from inline JS
     └── 2. OkHttp GET lookmovie2.to/api/v1/security/movie-access
              ?id_movie={id}&hash={hash}&expires={exp}
            with Referer: <play URL>
            └── JSON: {streams: {480p, 720p, 1080p}}
     │
     ▼
return StreamSet.best (1080p > 720p > 480p, pick first non-null)
     │
     ▼
ExoPlayer.setMediaItem(MediaItem.fromUri(streamUrl)) → play
     │
     ▼ (telemetry, fire-and-forget)
Analytics.logEvent("media_play_started", {kind, source="lookmovie"})
```

Time budget on a decent connection: detail fetch ~400ms, stream resolve ~600ms, ExoPlayer warmup ~500ms = ~1.5s tap-to-frame.

If any step fails:
- detail fetch → "Couldn't load this title" with retry
- stream resolve → "Can't play this title" with retry (covers expired-hash race, upstream rate-limit, parse drift)
- `Analytics.logEvent("media_play_failed", {reason: ...})` fires either way

## 4. Playing a TV episode

Adds one ingredient — the user has to pick which episode.

```
User taps a series poster
     │
     ▼
MovieDetailScreen for kind=SHOW
     │
     ├── detail fetch ALSO hits lookmovie2.to/shows/play/<slug>
     │   (the play page is the only page that exposes the seasons[] array)
     ├── parse seasons[] from inline JS → List<Season>
     └── render EpisodePicker(seasons)
     │
     ▼
User taps an episode row
     │
     ▼
nav: MoviePlayerRoute(kind="show", slug, title, idEpisode=N)
     │
     ▼
MoviePlayerViewModel.load("show", slug, N)
     │
     ▼
MediaRepository.getEpisodeStream(slug, idEpisode=N)
     │
     ▼
LookmovieStreamScraper.fetchEpisodeStream(slug, N)
     │
     ├── 1. GET lookmovie2.to/shows/play/<slug>?id_episode=N
     │      └── extract hash, expires (no need for id_show again — the
     │          security endpoint takes id_episode directly)
     └── 2. GET /api/v1/security/episode-access
              ?id_episode={N}&hash={hash}&expires={exp}
     │
     ▼
return StreamSet → ExoPlayer
```

The seasons array on the show page contains every episode's `id_episode`, so the picker is fully populated from a single HTTP request — no per-episode network call until the user actually picks one.

## 5. Playing a live TV channel

The simpler path — no detail page, just channel list → stream URL.

```
User taps a channel on HomeScreen
     │
     ▼
SharedViewModel.selectChannel(channel)
     │
     ▼
ChannelRepository.getStreamUrl(encodedUrl)
     │
     ▼
TechjailLiveScraper.fetchStreamUrl(channelId)  ← on-device
     │
     ├── OkHttp GET tv.techjail.net/huritv9/getlink.php?vv=1&CHID=N
     └── parse response (already returns a fresh tokenized m3u8)
     │
     ▼
ExoPlayer.setMediaItem(streamUrl) → play in MINI mode by default
```

The channel list itself comes from `data/channels.json` (the cold scrape probed each URL for liveness before committing, so the list is already filtered to working channels).

## 6. Search

Stays on-device, no Actions involvement.

```
User types in the Movies search bar
     │
     ▼ debounced 250ms (MoviesViewModel.search)
MediaRepository.search("dracula")
     │
     ▼
LookmovieSearchScraper.search(q)
     │
     ├── OkHttp GET lookmovie2.to/movies + /shows  (in parallel)
     ├── parse cards on each page
     └── return entries whose title contains q (case-insensitive)
     │
     ▼
StateFlow<List<MediaItem>> → grid UI
```

Recall is limited to "what's on the current /movies and /shows first pages" — about 60 titles. Good enough for "the new release I just heard about"; bad for deep back-catalog. A v3.1 improvement is paginating through `?page=2..N` until we find matches, capped at e.g. 5 pages.

## 7. In-app update flow

Unchanged from v2 — included here for completeness because it's load-bearing for the "site change needs an APK update" recovery strategy.

```
HomeScreen mounts
     │
     ▼ on app open
UpdateViewModel.check()
     │
     ▼
GET api.github.com/repos/<owner>/NepalLiveTv/releases/latest
     │
     ▼
compare tag (v3.0.5) to BuildConfig.VERSION_NAME (v3.0.4)
     │
     ▼
greater → show UpdateDialog
     │
     ▼ user taps "Update"
DownloadManager → APK to Downloads/Drishya-update.apk
     │
     ▼
Intent.VIEW (APK installer) → Android system installer prompt
     │
     ▼ user accepts
APK replaces the running app, re-launches on next open
     │
     ▼
Analytics.logEvent("update_accepted", {from_version, to_version})
```

## 8. Crash reporting

```
exception thrown anywhere in app code
     │
     ▼
Crashlytics SDK (auto-installed via Application.onCreate) captures stack
     │
     ▼ when device next has network
POST to crashlytics.googleapis.com (gzipped)
     │
     ▼
admin sees in console.firebase.google.com → Crashlytics
   grouped by stack signature, filterable by app version + device + OS
```

Non-fatal exceptions can be logged explicitly via `Crashlytics.recordException(e)` — useful for scrape-parse failures where we'd swallow and show a UI error but still want a paper trail.

## 9. Remote Config refresh

```
app cold start
     │
     ▼
RemoteConfig.fetchAndActivate()  ← runs once, async, ~200ms
     │
     ▼ Firebase backend
returns current config snapshot
     │
     ▼
Repository / scraper reads config values via RemoteConfig.getString("lookmovie_base_url")
     │
     ▼
if value differs from compiled default → use the override
```

This is how a "lookmovie moved to a new domain" emergency gets fixed without an APK release: we change one value in the console, every user picks it up on their next app open. Compiled defaults always exist as a fallback in case Firebase itself is unreachable.

## What's NOT on any of these paths

- Servers. The only server-like thing is GitHub Actions, and that only runs the cold scrape.
- Authentication. No login round-trip exists anywhere.
- Database queries. All local-only data (favorites, recents, dark mode) sits in DataStore.
- API keys exposed to clients. Firebase APIs use a public API key tied to the package signature + Firebase config — safe to ship in the APK.

If you see any code in the v3 codebase that does something none of these flows mention, it's either a leftover or an outright bug.
