# Agent Setu

*Grahak se commission tak - aapka setu* · From customer to commission - your bridge

Unofficial, offline Android app for postal agents and GDS to track customers, renewals, maturities
and commission. Hindi + English. Free, no ads.

> **Independent, unofficial application.** Not developed, endorsed, sponsored or operated by India
> Post, the Department of Posts, the Ministry of Communications or India Post Payments Bank. It does
> not connect to any departmental system.

## Download

Get the app only from **[Releases](https://github.com/rahulranjan-dev-py/Agent-Setu/releases)** or
the official Agent Setu WhatsApp group. Each release lists the APK's SHA-256 checksum and the
signing-key fingerprint; after installing, compare the fingerprint in *Settings → About*. A copy with
a different fingerprint is not genuine. What changed in each version: [CHANGELOG.md](CHANGELOG.md).

## Status

Step G: the MVP feature set from the roadmap is in place: customers and business with a live
commission preview, month-wise ledger, editable commission and interest rates, reminders with a
daily notification, TD/RD/MIS calculators, Hindi/English scheme cards shared as images, PIN lock,
encrypted backup, manual error report, and an update check. Package name `in.agentsetu.app`.
Next: testing on real phones, the signing key and the first signed release (see `docs/RELEASE.md`
and `docs/PILOT.md`).

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
| `release/version.json` | What the in-app update check reads; update it with every release |
| `release/notes/` | Release notes (Hindi + English) to paste into each GitHub Release |
| `docs/` | Roadmap, decisions, rate research, disclaimer, privacy notice |

## Documents

| File | What it is |
|---|---|
| [docs/ROADMAP.md](docs/ROADMAP.md) | Product roadmap v1.2 (summary) |
| [docs/DECISIONS.md](docs/DECISIONS.md) | Decisions made since v1.2 and open questions |
| [docs/COMMISSION_RATES.md](docs/COMMISSION_RATES.md) | Researched commission/interest rates, sources, and how rates are edited |
| [docs/DISCLAIMER.md](docs/DISCLAIMER.md) | Standard disclaimer text (Hindi + English) |
| [docs/RELEASE.md](docs/RELEASE.md) | Signing key, signed build, checksum, GitHub Release, `version.json`, WhatsApp post |
| [docs/PILOT.md](docs/PILOT.md) | Pilot stages, test checklist, commission hand-check sheet, feedback form, install video script |
| [docs/PRIVACY.md](docs/PRIVACY.md) | Privacy notice (Hindi + English), linked from Settings → About |
| [docs/REVIEW_2026-09.md](docs/REVIEW_2026-09.md) | Bug, risk and security review of 1.1.3: findings, what 1.1.4 fixed, what needs the owner |
| [CHANGELOG.md](CHANGELOG.md) | Every published version, newest first |
| [data/seed/](data/seed/) | Editable sample rate tables loaded on first install |
