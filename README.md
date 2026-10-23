# Attention Monitor — Pixel prototype v0.1

A minimal Kotlin Android app that records **accessibility scroll events** (not guaranteed finger swipes), transitions among apps, unlock/lock events, and estimated foreground duration. Data remains in SQLite on the device. No network permission, backend, login, or account.

## Build

1. Open this folder in **Android Studio** (JDK 17, Android SDK platform 35). Let Android Studio download the Gradle dependencies.
2. Run on a Pixel with Android 10+ (API 29+), or use Build > Build APK(s).
3. Open Attention Monitor > **Enable / inspect accessibility permission** > Installed apps / Downloaded apps > Attention Monitor (local tracking) > Allow.
4. After unlocking, test Facebook app, YouTube app, and Chrome with `https://www.instagram.com/` visible in the address bar.
5. Return to Attention Monitor and tap **Refresh report**.

This archive intentionally contains no Gradle wrapper binary. Android Studio can sync using a compatible Gradle version (AGP 8.7.3 uses Gradle 8.9). If you prefer the command line, install Gradle 8.9 or create the wrapper using `gradle wrapper --gradle-version 8.9`.

## Interpretation / limitations

- `SCROLL` means a **positive Android accessibility scrollDeltaY** observed in a monitored app. It is **not proof of one upward finger swipe**, nor does it reliably distinguish Shorts/Reels from other scrollable UI. A scroll might generate multiple events. A 400 ms debounce merges bursts.
- A single `SCROLL` event records epoch milliseconds, app, scrollDeltaY, and time since the previous detected scroll in the same uninterrupted foreground segment.
- `SWITCH` events show app transitions, including entering or leaving monitored apps. Switches to unknown apps are stored as `NONE` (not their name). The app-switch number on the dashboard counts direct monitored-to-monitored transitions, not those separated by another app.
- A screen-lock event closes the current app segment; an unlock event starts a new session. The accessibility service is system-managed and Android may stop it. No guarantee of 24/7 event capture.
- Chrome only qualifies when the Chrome URL bar can be read and the domain is `instagram.com` or its subdomain; addresses and page contents are **never persisted**. Chrome hides the URL field while scrolling and Android/Chrome UI changes can prevent detection. If identification is not reliable, Chrome is excluded, rather than guessing that every Chrome scroll is Instagram.
- Duration derives from foreground state changes seen by accessibility events, so it is an estimate; durations can be inaccurate after service interruption.
- This is a private sideload prototype, not a Play Store-ready accessibility app. Google Play imposes additional disclosure and approval requirements for accessibility use. Do not falsely label it an accessibility tool for people with disabilities.

## Privacy

Only timestamp, event type, normalized monitored-app label, and optional scroll-delta/interval/from-app are stored. No textual page content, Chrome URLs, names of other apps, passwords, photos, taps, or network activity are stored. You can delete all history within the app or uninstall it.

## Next testing steps

- Check Android Studio Logcat for crashes and verify Chrome URL readability on your particular Pixel/Chrome combination.
- Use 10 deliberate swipes in YouTube Shorts and compare with `SCROLL` count. Repeat in Facebook Reels and Instagram Web. Record overcount/undercount.
- Validate lock/unlock sessions and switches, including Home launcher, notifications, and split-screen.
- Optionally add export CSV, a foreground service status/heartbeat, per-platform Shorts/Reels classification, and historical charts after real-device validation.

## Cloud build (no Android Studio on the laptop)

See [GITHUB_BUILD.md](GITHUB_BUILD.md). The project includes `.github/workflows/build-apk.yml` for GitHub Actions.
