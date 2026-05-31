# Firebase Setup — Step by Step

The full Firebase project setup you'll do once, manually, in a browser. Takes about 10 minutes. After this is done, the rest is just adding SDK calls in code, which I handle.

> **Your input is required for sections marked 👤.** Everything else is automatic or follows from your previous click.

## What you'll get when this is done

- Live admin dashboard at `console.firebase.google.com` showing:
  - Daily / monthly active users (graph + raw numbers)
  - Top device models and Android versions
  - Top countries / cities
  - Custom events: every Play tap, search, install, crash
  - Real-time view: "12 users right now, 3 watching movies, 1 in settings"
- Crash reports grouped by stack trace, filterable by version
- One Firebase Console value you can change to flip something in the app (a base URL, a feature flag) without an APK release

## Part 1 — Create the Firebase project

### Step 1 👤 — open the console

Go to <https://console.firebase.google.com/> in any browser. Sign in with the same Google account you want to be the admin forever — usually a personal Gmail. Once you set the admin, Firebase keeps it.

### Step 2 👤 — create a new project

Click the big **"Create a project"** card (or **"Add project"** if you already have other Firebase projects).

You'll be asked three things:

1. **Project name:** type `Drishya` (or anything you like — this is just the human label in the console).
2. **Project ID:** Firebase will auto-suggest something like `drishya-12345`. The ID becomes part of the API URL and can't be changed later. **Take what's offered** unless you really want a custom ID — there's no functional difference.
3. **Google Analytics for this Firebase project:** leave it **ON**. This is what gives you DAU/MAU numbers. Click Continue.
4. **Analytics account:** if you've never used GA before, pick **"Default Account for Firebase"**. If you have a GA account, pick that. Either works.
5. **Analytics region/timezone:** pick **(GMT+05:45) Kathmandu** since that's your timezone.
6. Accept the terms boxes, click **Create project**.

Wait ~30 seconds while Firebase provisions. Then click **Continue** when it's ready.

You're now on the project home page.

## Part 2 — Add the Android app to the project

### Step 3 👤 — register the Android package

On the project home page you'll see a small **"Get started by adding Firebase to your app"** section with platform icons. Click the **Android icon** (the green Bugdroid).

Fill the form:

1. **Android package name:** exactly `com.app.nepallivetv` — this *must* match `applicationId` in `app/build.gradle.kts`. Copy-paste it to be safe.
2. **App nickname (optional):** type `Drishya` or leave blank.
3. **Debug signing certificate SHA-1 (optional but recommended):** this lets Firebase recognize debug builds for testing.

   Generate it by opening a terminal in your `NepalLiveTv/` directory and running:

   ```bash
   ./gradlew signingReport | grep -A 1 "Variant: debug" | head
   ```

   Look for the `SHA-1:` line. It looks like `1A:2B:3C:...` (40 hex chars with colons). Copy-paste that into the SHA-1 field.

   You can skip it for now and add later via **Project Settings → Your apps → Add fingerprint**. Skipping just means certain Firebase services (Auth — which we don't use, Dynamic Links) won't recognize debug builds. Analytics and Crashlytics work fine without it.

4. Click **Register app**.

### Step 4 👤 — download google-services.json

Firebase shows a big **"Download google-services.json"** button. Click it. Your browser saves a small JSON file.

**Put that file in `NepalLiveTv/app/google-services.json`** (yes, inside the `app/` module directory, not the project root — this is where the Google Services Gradle plugin looks for it).

**Add the file to `.gitignore`**: it contains your Firebase API key, which is technically safe to commit (it's restricted by package name + SHA-1) but Google's official recommendation is to not commit it. The build won't work without it locally, so each developer downloads their own copy.

Actually, simpler: since this is a single-developer project, commit it. The "restricted by package + SHA-1" guard is real — someone forking the repo can't use your Firebase project even with the file. I'll handle the right setup in code.

Click **Next** on the Firebase wizard.

### Step 5 — Gradle wiring (I do this)

The wizard now shows Gradle snippets to paste. **Don't paste anything yet.** When I start the v3 migration, I'll add:

- `google-services` Gradle plugin to root `build.gradle.kts`
- `com.google.gms.google-services` apply line to `app/build.gradle.kts`
- The Firebase BoM dependency
- The specific Firebase SDKs (`analytics`, `crashlytics`, `remote-config`)

You'd just be pasting boilerplate from the wizard; I'll generate it from the canonical Firebase docs to make sure versions are pinned to the same BoM. Click **Next**, then **Continue to console** — we're done with the registration flow.

## Part 3 — Enable the modules you'll actually use

You're back on the project home. Use the left sidebar to enable each module. Most "enable" is just visiting the page once.

### Step 6 👤 — Analytics (already on)

Sidebar → **Analytics → Dashboard**. You'll see a "Streaming…" placeholder graph. Data starts flowing the first time the app runs with the new SDK installed. Nothing to enable.

Click **Events** in the submenu — you'll see the auto-tracked events Firebase adds for free: `first_open`, `screen_view`, `app_open`, `session_start`, `user_engagement`. We'll add custom events on top (`media_play_started`, etc.) in code.

### Step 7 👤 — Crashlytics

Sidebar → **Release & Monitor → Crashlytics**.

You'll see a card saying "Add Crashlytics SDK to start" with a tutorial. Skip the tutorial — I'll handle the SDK install in code. Just leave the page as-is. The first time your app crashes (or you trigger a test crash), Crashlytics auto-enables and reports start appearing.

### Step 8 👤 — Remote Config

Sidebar → **Build → Remote Config**.

Click **Create configuration**.

Add three initial parameters (we'll fill in real defaults later — just create the keys now so the console has them):

| Parameter | Default value | Description |
|---|---|---|
| `lookmovie_base_url` | `https://lookmovie2.to` | The lookmovie domain to scrape from |
| `techjail_base_url` | `http://tv.techjail.net/huritv9` | The techjail base URL |
| `scraper_timeout_ms` | `20000` | Per-request timeout used by the on-device scraper |

For each: click **Add parameter**, paste the parameter name, set Data type (String for the first two, Number for the third), paste the default. Click **Save**.

After all three are added, click **Publish changes** at the top right. Confirm.

That's it for Remote Config. You can change these values any time later, and every running app picks up the new value on its next launch (within an hour by default).

### Step 9 (optional) 👤 — Authentication / Firestore / Hosting

**Do NOT enable any of these.** The v3 architecture explicitly does not need them. Enabling them costs nothing but adds attack surface and confusion. If you ever do need them, you can enable later from the same sidebar.

## Part 4 — Set up access for any teammates (optional)

If only you will be admin, **skip this section.** Otherwise:

### Step 10 👤 — invite others

Project home → gear icon (top-left, next to "Project Overview") → **Users and permissions**.

Click **Add member**. Type the email, pick role:
- **Viewer** — can see Analytics + Crashlytics but can't change anything (right for a junior teammate)
- **Editor** — can change Remote Config and most settings (right for another dev)
- **Owner** — can change billing and add other owners (only for you, ideally)

Click **Add member**. They get an email invite.

## Part 5 — Verify

After I do the code changes for v3, you'll do these to verify everything's wired:

### Step 11 👤 — first run check

- Install the dev build on your phone
- Open the app
- Go back to `console.firebase.google.com` → your project → **Analytics → Realtime**
- Within ~30 seconds you should see "1 user in the last 30 minutes" with your device's location

If this works, Analytics is correctly installed. If it doesn't appear within 5 minutes, something's off in the SDK setup and I'll diagnose.

### Step 12 👤 — force a test crash

I'll add a hidden "Crash test" button in Settings (only visible in debug builds). Tap it, the app crashes immediately.

- Re-open the app (Crashlytics needs a second launch to upload the crash report)
- After ~1 minute, **Crashlytics → Dashboard** should show "1 crash, 1 affected user"
- Open the crash → see your stack trace

If you see it, Crashlytics is wired. If not, I'll diagnose.

### Step 13 👤 — test Remote Config override

- Console → Remote Config → change `scraper_timeout_ms` to `99999`
- Click Publish
- Force-close the app, reopen
- App reads the new value (visible in a debug-only "Config" section on Settings)

If this works, Remote Config is wired. From this point on, any operational tweak ships through the console, no APK release.

## What the admin dashboard actually looks like

Once v3 ships and the app has been live a few days, this is what you'll be checking:

### Daily routine (2 min)

1. `console.firebase.google.com` → your project → **Analytics → Dashboard**
   - Glance at the active users curve (yesterday's spike or dip?)
   - Glance at retention cohort table (is week-2 retention crawling up?)
2. **Crashlytics → Dashboard**
   - Any new crash signatures since yesterday? Most days: none.
   - Crash-free users %: should be >99.5%

### Weekly routine (10 min)

1. **Analytics → Events** — sort by event volume, look at:
   - `media_play_started` count and trend
   - `media_play_failed` count — if this rises, something's broken upstream
   - `scraper_drift_detected` — should be near zero; spikes mean a site change
2. **Analytics → User attributes / Tech details** — top device models, top OS versions. Useful for deciding what to keep supporting.
3. **Analytics → Audiences** — build custom segments like "users who played a movie in the last 7 days" if you want to push them a notification later

## Costs you might worry about, but won't actually pay

- **Analytics:** unlimited custom events for free, forever.
- **Crashlytics:** unlimited crash reports for free, forever.
- **Remote Config:** 10,000 active configs/day free. For our app shape, "active configs" means daily-active users — so you'd hit the cap at 10K DAU. Generous.
- **Cloud Messaging (FCM):** if we add push notifications later, also free.
- **Cloud Firestore / Storage / Functions:** we don't use any of these. They have free tiers but also bills you can run up. Stay away unless we explicitly add them.

The day Firebase starts billing you is the day you have 10K+ DAU. At that point, you have other priorities.

## What goes in the app for analytics (preview — I write this code in v3)

```kotlin
// analytics/Telemetry.kt
class Telemetry(private val analytics: FirebaseAnalytics) {

    fun screenView(name: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundleOf(
            FirebaseAnalytics.Param.SCREEN_NAME to name,
        ))
    }

    fun mediaPlayStarted(kind: String, title: String, source: String) {
        analytics.logEvent("media_play_started", bundleOf(
            "kind" to kind,
            "title" to title,
            "source" to source,
        ))
    }

    fun mediaPlayFailed(kind: String, reason: String) {
        analytics.logEvent("media_play_failed", bundleOf(
            "kind" to kind,
            "reason" to reason,
        ))
    }

    fun scraperDriftDetected(scraper: String, field: String) {
        analytics.logEvent("scraper_drift_detected", bundleOf(
            "scraper" to scraper,
            "field" to field,
        ))
    }

    // ...etc for search_performed, channel_play_started, update_accepted
}
```

Calls go on every event boundary: ViewModel entry points, repo exception handlers, navigation transitions. About 30 call sites total across the app.

## Privacy / user consent

Add a one-liner to the Settings screen: "We use Firebase Analytics and Crashlytics to improve the app. No personal information is collected." Plus a toggle:

```kotlin
// Settings → Privacy → "Allow anonymous usage stats"
analytics.setAnalyticsCollectionEnabled(enabled)
Firebase.crashlytics.setCrashlyticsCollectionEnabled(enabled)
```

The toggle defaults to ON. If a user flips it off, both Analytics and Crashlytics go fully silent — Firebase SDKs respect the flag immediately, no further events fire.

This is enough to be safe with most jurisdictions including India's DPDP Act, EU's GDPR, and Apple/Google's policy expectations.

## When you're ready

Once you've done steps 1–8 (the must-have setup), tell me and I'll start the v3 migration. Steps 9–13 happen during or after I finish.
