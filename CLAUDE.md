# Agent Setu: notes for Claude

Unofficial, offline Android app (Kotlin, Jetpack Compose) that helps postal staff and GDS track
customers, renewals, maturities and commission. Hindi + English. Product plan: `docs/ROADMAP.md`.

## Working agreement with the owner

- **Ask before each step.** Report findings or a plan first, and start work only after the owner
  says "go ahead". Approval for one step does not cover the next one.
- Develop on the branch named for the session and push there; do not open PRs unless asked.

## Build and test

- `./gradlew :core:test`: pure-Kotlin unit tests, including validation of `data/seed/*.json`.
- `./gradlew :app:assembleDebug`: needs the Android SDK and Google Maven (dl.google.com). If those
  are unreachable (as in some cloud sandboxes), build `core` alone and rely on the GitHub Actions run
  to compile `app`.
- Kotlin package / namespace is `app.agentsetu`; `applicationId` is `in.agentsetu.app` (final, never
  change it). Releases: follow `docs/RELEASE.md`; signing happens only on the owner's computer.

## Guardrails (never break these in code, text or assets)

- No departmental credentials, and no connection to or scraping of any departmental system
  (Finacle, CSI/McCamish, IPPB, UIDAI, etc.).
- No full Aadhaar, PAN or bank/policy numbers: store at most the last 4 digits (`refLast4`).
- No payments, and no handling of money.
- No ads, analytics, tracking or crash SDKs. Errors go through the manual "Send error report" flow
  (see `docs/DECISIONS.md`).
- No India Post / DoP / IPPB logos, emblems, red-and-yellow colours, or words like "official" or
  "authorised". The disclaimer in `docs/DISCLAIMER.md` must appear in the app.
- Permissions: the only one users are asked for is notifications (plus "install unknown apps" only
  if in-app APK download is built). Install-time permissions from libraries are accepted
  (WorkManager: wake lock, boot completed, network state; androidx.biometric: use biometric /
  fingerprint). INTERNET is used only by `UpdateChecker` to read the public `release/version.json`;
  no other network call may be added, and no user data may ever be sent. Never add SMS, call log,
  contacts, location or storage. Files go through the
  system file picker or share sheet, never storage permissions.
- Scheme cards and calculators never contain a rate: they read the user's `InterestRate` table.
- **Never hard-code a commission or interest rate.** Rates live in the editable `CommissionRule` /
  `InterestRate` tables, seeded from `data/seed/*.sample.json`, with `effectiveFrom` and `orderRef`.
- Never commit a keystore, key passwords, `local.properties` or any real customer data.

## Security conventions

- Only a salted PBKDF2 hash of the app PIN is stored (`core/security/PinHasher`).
- Backup files: AES-256-GCM with a PBKDF2 key from the user's password; header authenticated
  (`core/backup/BackupCrypto`). Never add a way to open a backup without its password.
- Error reports are built on the phone, masked for long digit runs, shown to the user, and sent
  only through the share sheet.

## Data conventions

- Every entity has a UUID `id`, `createdAt`, `updatedAt` and a soft-delete `deleted` flag (sync-ready).
- Changing a rate closes the old rule (`effectiveTo`) and inserts a new one; history is never overwritten.
- Numbers in Indian format (1,00,000); dates DD-MM-YYYY; every UI string in both `values/` and `values-hi/`.
