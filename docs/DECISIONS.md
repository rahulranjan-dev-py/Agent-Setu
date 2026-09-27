# Decisions log

Decisions made after roadmap v1.2. Newest first.

| Date | Decision | Detail |
|---|---|---|
| 27-09-2026 | **Project structure (step B)** | Two modules: `core` (pure Kotlin, testable with only a JDK) and `app` (Android). Code package `app.agentsetu` is kept separate from `applicationId`, which is a placeholder (`in.agentsetu.app`) until the final package name is chosen. Library versions are a mid-2025 set known to work together (AGP 8.9.3, Kotlin 2.1.21, Compose BOM 2025.05.01, Room 2.7.1, Hilt 2.56.2, SQLCipher 4.9.0, compile/target SDK 35); update them together once a full build can be run. |
| 27-09-2026 | **Database key handling** | SQLCipher passphrase is random, wrapped with an AES-256-GCM key held in Android Keystore. App data is excluded from Android cloud backup and device transfer; users move data only through their own encrypted backup file. |
| 27-09-2026 | **CI on GitHub Actions** | Every push runs `core` tests and builds a debug APK (downloadable from the run for testers). CI never holds the release keystore. |
| 27-09-2026 | **Crash reporting: manual "Send error report" button only** | No Firebase Crashlytics or any other crash SDK. On an uncaught exception the app saves the stack trace locally (no customer data). Settings → "Send error report" shows the report to the user (app version, Android version, device model, stack trace), then opens the Android share sheet so the user chooses WhatsApp or email. Keeps the "no tracking SDK" guardrail intact. |
| 27-09-2026 | **Rates ship as editable sample data** | Commission and interest rates collected from PoTools, SAPost PO Tools, Postalstudy and news sites are bundled as sample seed files, marked unverified. Users edit them in Settings → Rates & rules. See [COMMISSION_RATES.md](COMMISSION_RATES.md). |
| 27-09-2026 | **Name confirmed: Agent Setu** | Launcher label "Agent Setu", tagline "Grahak se commission tak - aapka setu". |

## Open questions (owner to decide)

| Question | Why it matters |
|---|---|
| Final package name (e.g. `in.agentsetu.app`) | Permanent; cannot change after first release without users reinstalling. |
| Is this GitHub repository public or private? | The keystore, passwords and any personal notes must never be committed either way; a public repo also exposes work-in-progress. |
| Verified order copies for the rates | Needed to replace sample rates and settle the PLI renewal 1% vs 2% conflict. |
