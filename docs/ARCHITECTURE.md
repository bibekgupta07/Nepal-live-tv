# Architecture

How Drishya works after the v3 migration: zero hosted backend, scraping split between GitHub Actions (cold, periodic) and the device (hot, click-time), with Firebase covering analytics and crash reporting.

## Why no backend

The original `backend-scrapetv` FastAPI service ran on Railway. It scraped two upstreams (techjail for live TV, lookmovie2.to for movies) and re-served the data behind a JWT auth gate.

Two things made that backend optional rather than load-bearing:

1. **Neither upstream is Cloudflare-walled.** Plain HTTP requests with a normal user-agent get real HTML back. The same scraping logic runs identically on a server or on a phone.
2. **Stream URLs expire fast.** techjail tokens last ~24h, lookmovie hashes last minutes. Pre-computing them server-side and caching wouldn't help — by the time the user taps Play, the URL is dead. The scrape has to happen at click time, and it might as well happen on the phone.

The pieces that *did* need pre-computation (the channel list, the home feed) are batch-friendly: a single Python script run every 6 hours produces JSON, and that JSON is small. GitHub Actions runs it for free, GitHub serves it from `raw.githubusercontent.com` for free.

So the backend's value collapsed to: hide a JWT gate, and host a Python interpreter that runs cron-style. Both are removed in v3.

## The two-track scrape split

| Track | Where | Cadence | Output |
|---|---|---|---|
| Cold catalog | GitHub Actions runner | Every 6 hours | `data/channels.json`, `data/movies-home.json` committed to repo |
| Hot URLs | User's device | At click time | Live `m3u8` URL passed straight to ExoPlayer |

The split lines up neatly with what each system is good at:

- **Actions is good at batch work that touches many URLs at once.** Scraping 200+ channels and 9 catalog pages takes ~30 seconds. A user device doing the same on every launch would be slow and would hammer upstreams from every IP.
- **The device is good at one-shot work that has to be fresh.** Getting *this* movie's stream URL is one HTTP request and one regex extraction — fast on a phone, no caching needed.

If an Actions run fails (upstream down, parser regex broke), the previous JSON stays in the repo. Users see a slightly stale catalog but the app still works. The only true outage is when a parser change breaks both the Actions scraper *and* the on-device scraper at once — and a fix to both can ship together in a single PR.

## High-level diagram

```
                    ┌─────────────────────────┐
                    │  lookmovie2.to          │
                    │  tv.techjail.net        │
                    └────────┬────────────────┘
                             │
              scraped by     │
        ┌────────────────────┴──────────────────┐
        │                                       │
        ▼                                       ▼
┌──────────────────┐                ┌─────────────────────────┐
│ GitHub Actions   │                │  Android app            │
│ (every 6h)       │                │  (on user tap)          │
│                  │                │                          │
│ scraper/*.py     │                │  OkHttp + Jsoup          │
└────────┬─────────┘                │  parses HTML/JSON live   │
         │ writes                   │                          │
         ▼                          └──────────┬──────────────┘
┌──────────────────┐                           │
│ data/*.json      │                           │ HLS m3u8
│ (committed)      │                           ▼
└────────┬─────────┘                ┌──────────────────────────┐
         │                          │  ExoPlayer (Media3 1.5)  │
         │ raw.githubusercontent.com │  Native HLS playback     │
         └──────────────────────────►                          │
                                    └──────────────────────────┘

                    Telemetry only (no data path):
                    ┌────────────────────────┐
                    │  Firebase Analytics    │  ← events, screen views
                    │  Firebase Crashlytics  │  ← stack traces
                    │  Firebase Remote Config│  ← live config flags
                    └────────────────────────┘
```

## Repo layout

After consolidation (v3+):

```
NepalLiveTv/
├── app/                       Android app (existing)
│   └── src/main/java/com/app/nepallivetv/
│       ├── data/
│       │   ├── remote/
│       │   │   ├── catalog/   ← NEW: raw.gh JSON fetcher
│       │   │   └── scrape/    ← NEW: on-device Jsoup scrapers
│       │   ├── repository/
│       │   └── local/
│       ├── domain/
│       ├── presentation/
│       └── analytics/         ← NEW: Firebase wrapper
├── scraper/                   NEW: Python, run by Actions
│   ├── channels.py
│   ├── movies_home.py
│   ├── lookmovie.py           ← shared parsing helpers
│   └── requirements.txt
├── data/                      NEW: bot-committed JSON
│   ├── channels.json
│   ├── movies-home.json
│   └── last-updated.json
├── docs/
│   ├── ARCHITECTURE.md        ← this file
│   ├── CODE_FLOW.md
│   └── FIREBASE_SETUP.md
└── .github/workflows/
    ├── release.yml            existing: APK release on tag
    └── scrape.yml             NEW: cron every 6h
```

`backend-scrapetv` is archived (read-only on GitHub) and no longer hosted anywhere.

## External dependencies and what we use them for

| Source | Used for | How |
|---|---|---|
| `lookmovie2.to` | Movie/show catalog + streams | Actions scrapes home rows; device scrapes detail + stream URLs |
| `tv.techjail.net` | Live TV catalog + stream URLs | Actions scrapes channel list; device scrapes stream URLs |
| `raw.githubusercontent.com` | Catalog JSON delivery | Free CDN, 5-min cache TTL |
| `api.github.com/repos/.../releases/latest` | In-app updater | Already wired (v2) |
| Firebase Analytics | DAU/MAU, screen views, custom events | Anonymous Advertising ID, free at our scale |
| Firebase Crashlytics | Crash + ANR reporting | Stack traces grouped by signature |
| Firebase Remote Config | Live config flags | Override base URLs, timeouts, feature toggles without an APK release |

Nothing else. No database, no auth provider, no message broker, no compute.

## How adding a new content source works

Both `MediaSource` (movies/shows) and `ChannelSource` (live TV) are still defined as Kotlin interfaces in the domain layer — the hexagonal port idea moved from Python to Kotlin verbatim. Adding a new source is:

1. Implement the port (e.g. `FmoviesMediaSource : MediaSource`) under `data/remote/scrape/`.
2. Register it in the Koin module.
3. If the new source contributes to the cold catalog, add a `scraper/fmovies_home.py` and a step in `scrape.yml`.

The rest of the app — UI, ViewModels, ExoPlayer wiring, analytics — stays untouched.

## Failure modes and what they look like

| Failure | Symptom | Mitigation |
|---|---|---|
| Actions run fails | Stale catalog | Last good JSON keeps serving; alert via Crashlytics-issue analog event |
| Upstream HTML changes, both scrapers break | "Couldn't load" everywhere | `scraper_drift_detected` analytics event fires; fix in single PR; ship via in-app updater |
| Upstream rate-limits the user | Specific titles fail to play | Backoff + a friendly error; consider DNS-over-HTTPS or alternate domain via Remote Config |
| Phone's ISP DNS blocks upstream | Catalog loads (raw.gh works) but playback fails | DoH in OkHttp (default on); user-visible "check your network" hint |
| Firebase throttles us | None observable until 10K+ MAU | Free tier is generous; concern only at scale |

## What v3 explicitly removes

- All JWT authentication (login screen, register screen, auth viewmodel, auth interceptor)
- All backend HTTP routes
- All Mongo / bcrypt / passlib / python-jose code
- `BuildConfig.BASE_URL` (catalog URL is a const in code; live URLs vary per request)
- The `backend-scrapetv` Railway deployment

What stays: favorites, recently watched, dark mode preference, all DataStore-local. None of that ever needed a backend.

## What v3 explicitly adds

- `scraper/` Python scripts (ported from `backend-scrapetv/app/adapters/outbound/scraping/`)
- `data/` JSON outputs (auto-generated)
- `scrape.yml` Actions workflow
- `data/remote/scrape/*` Kotlin on-device scrapers
- Firebase SDKs (analytics, crashlytics, remote-config)
- `analytics/Telemetry.kt` thin wrapper for event logging

## Versioning

- v2.x — current hosted-backend era
- v3.0 — first release with serverless architecture and Firebase
- The in-app updater (shipped in v2.0) bridges users to v3 automatically — no Play Store needed
