# Decisions log

Decisions made after roadmap v1.2. Newest first.

| Date | Decision | Detail |
|---|---|---|
| 28-09-2026 | **Reminders (step E)** | A WorkManager job runs daily around 7 am, entirely on the phone. It creates premium, RD-instalment, maturity (30-day radar) and follow-up reminders, and posts **one** summary notification. Dues and follow-ups notify when due; maturities at 30, 15, 7 and 1 day(s) before, then daily. Customer names are hidden on the lock screen. Reminders missed while the phone was off are kept for 30 days. Rules live in `core` (`ReminderPlanner`) and are unit-tested. |
| 28-09-2026 | **Collected premiums feed the ledger** | Marking a premium or RD instalment *Collected* adds its expected commission (policy year and rule as of the due date). There is at most one ledger entry per holding per month, so marking twice cannot double-count. |
| 28-09-2026 | **Permissions** | Notifications is the only permission users are asked for (Android 13+). WorkManager adds install-time permissions with no prompt (wake lock, boot completed, network state) so reminders survive restarts; they give no access to personal data. |
| 28-09-2026 | **General follow-ups** | A follow-up can be added from a customer's page without choosing a product (a lead with an empty product). The full sales pipeline screen comes later. |
| 28-09-2026 | **Data model and commission engine (step C)** | All 10 tables from the roadmap in Room (schema version 2; version 1 existed only in pre-release debug builds and is dropped on upgrade). Money is stored as paise, rates as decimal text, dates as ISO text. Commission logic lives in `core` and is unit-tested: rule matching, amount calculation (rounded half-up to the paisa), and rate revision without losing history. Details in [COMMISSION_RATES.md](COMMISSION_RATES.md#rule-matching). |
| 28-09-2026 | **One product code per product** | Codes (`PLI`, `TD_5Y`, …) are shared by catalogue, rules and interest rates; a rule can name a family (`TD`). PLI rules carry AEA / non-AEA in `policyCategory` instead of in the code. The BPM 1/2/3-year TD rule was split into one rule per term. |
| 28-09-2026 | **Guardrail enforced in the data layer** | A holding cannot be created with anything but exactly 4 digits (or nothing) in `refLast4`; a full policy/account number is rejected, not trimmed. There are no columns for Aadhaar, PAN or account numbers. |
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
| First public release needs real database migrations | From the first release, each schema change must keep users' data; the pre-release shortcut (dropping version 1) must not be reused. |
| Verified order copies for the rates | Needed to replace sample rates and settle the PLI renewal 1% vs 2% conflict. |
