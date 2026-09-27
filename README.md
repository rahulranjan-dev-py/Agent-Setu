# Agent Setu

*Grahak se commission tak - aapka setu* · From customer to commission - your bridge

Unofficial, offline Android app for postal agents and GDS to track customers, renewals, maturities
and commission. Hindi + English. Free, no ads.

> **Independent, unofficial application.** Not developed, endorsed, sponsored or operated by India
> Post, the Department of Posts, the Ministry of Communications or India Post Payments Bank. It does
> not connect to any departmental system.

## Status

Step E: reminders. The Today screen lists overdue and due premiums, RD instalments, follow-ups and
maturities in the next 30 days, with Call / WhatsApp / Done. A daily on-phone job sends one summary
notification. Marking a premium collected adds its expected commission to the ledger. Still to come:
app lock, encrypted backup, error report, calculators, scheme cards and the update check.

## Build

Requirements: JDK 17 or newer and the Android SDK (API 35). Android Studio sets both up.

```bash
./gradlew :core:test            # pure-Kotlin logic and seed-file checks (JDK only)
./gradlew :app:assembleDebug    # debug APK -> app/build/outputs/apk/debug/
```

Every push runs both on GitHub Actions (`.github/workflows/build.yml`), and the debug APK is
attached to the run. Release signing reads a git-ignored `keystore.properties`; the keystore itself
must never be committed.

## Project layout

| Path | Contents |
|---|---|
| `app/` | Android app: Jetpack Compose UI, Hilt, Room + SQLCipher |
| `core/` | Pure Kotlin: commission rule matching and calculation, rate revision, product catalogue, Indian number/date formatting, seed-file validation |
| `data/seed/` | Editable sample rate tables, bundled into the APK as assets |
| `docs/` | Roadmap, decisions, rate research, disclaimer, privacy notice |

## Documents

| File | What it is |
|---|---|
| [docs/ROADMAP.md](docs/ROADMAP.md) | Product roadmap v1.2 (summary) |
| [docs/DECISIONS.md](docs/DECISIONS.md) | Decisions made since v1.2 and open questions |
| [docs/COMMISSION_RATES.md](docs/COMMISSION_RATES.md) | Researched commission/interest rates, sources, and how rates are edited |
| [docs/DISCLAIMER.md](docs/DISCLAIMER.md) | Standard disclaimer text (Hindi + English) |
| [docs/PRIVACY.md](docs/PRIVACY.md) | Privacy notice draft (Hindi + English) |
| [data/seed/](data/seed/) | Editable sample rate tables loaded on first install |
