# Changelog

Every published version of Agent Setu, newest first. The Hindi release notes for each version are in
[`release/notes/`](release/notes/); they are pasted into the GitHub Release and the WhatsApp post.

Version numbers: `versionName` (shown to users) follows 1.0.0 → 1.0.1 (fixes) → 1.1.0 (new
features); `versionCode` goes up by exactly 1 with every published APK.

## 1.1.7 (versionCode 11) - keyboard no longer hides fields; skip or delete a ledger entry; published 30-09-2026

- Every screen now moves up above the keyboard, so the field being typed in stays visible
  (profile setup, add business, add rule, search, PIN screens, dialogs).
- Ledger: an entry can be marked "Skip: not eligible" (kept for the record, not counted, reversible)
  or deleted with its receipts; a deleted entry does not come back when the premium is collected again.

## 1.1.6 (versionCode 10) - updated libraries; published 28-09-2026

- Kotlin 2.4.20, Android Gradle Plugin 9.4.1, core-ktx 1.19.1, navigation-compose 2.10.2,
  hilt-navigation-compose 1.4.0. No user-facing change. Release builds now need the owner's
  approval on GitHub (secrets in the `release` environment); Gradle wrapper checksum verified.

## 1.1.5 (versionCode 9) - updated build toolchain and libraries; published 28-09-2026

- Build toolchain: Android Gradle Plugin 9.4, Gradle 9.6.1, Kotlin 2.3.21, KSP 2.3.12,
  compileSdk 37 (targetSdk unchanged); libraries: Hilt 2.60.1, SQLCipher 4.19.0, Room 2.8.5,
  WorkManager 2.12.0, AppCompat 1.8.0. No user-facing change; ships with the next version.

## 1.1.4 (versionCode 8) - review fixes: security, privacy and bugs; published 28-09-2026

Result of a bug, risk and security review (docs/REVIEW_2026-09.md). Nothing here changes what the
app does day to day; it closes gaps found in the review.

- Security: a downloaded update must be this app (same package and signing key, the announced
  version) and come from the app's own release address before Android is asked to install it;
  the lock-out after wrong PINs cannot be shortened by changing the phone's date; the PIN and
  recovery-code screens cannot be captured and the app is hidden in Recents on Android 13+;
  backup files are capped in size and their key-stretching raised to 600,000 rounds (old files
  still open); a lost database key shows an explanation and an erase button instead of crashing.
- Privacy: deleting a customer or a policy now also wipes the policy number and follow-up notes
  from the hidden rows; deleted receipts lose their reference and note; the privacy notice is
  updated in both languages and linked from Settings → About.
- Bugs: "Collected" after an Undo recorded nothing (fixed); collecting a premium dated before an
  edited start date crashed (fixed); editing a policy now replaces its reminders instead of leaving
  stale ones; Undo of a follow-up removes the follow-up reminder it created; a changed reminder time
  now really takes effect; Today notices the date change and refreshes on return; "Reset to sample"
  cannot revive a sample rule you revised; entries with "No rule" are recomputed when you add or
  change a rule; a rule can be ended from a date; a term band with min above max is refused;
  searching with % or _ works literally; rotation keeps the recovery code on screen.
- Build: GitHub Actions pinned to commits, Dependabot enabled, signed test builds only on demand.

## 1.1.3 (versionCode 7) - update check on every start, resumable download; published 28-09-2026

- The update check runs whenever the app starts or comes to the foreground (at most once every
  15 minutes), not once a day, so a new version shows on Today the same day.
- Download: a slow link never times out; only a 60-second stall does. After a drop the app
  reconnects up to 3 times and continues from where it stopped (HTTP ranges); a failure offers
  Retry, which also resumes. The checksum is always checked on the complete file.

## 1.1.2 (versionCode 6) - in-app update install, recovery-code fix; published 28-09-2026

- Download on an update now fetches the APK inside the app, verifies its SHA-256 against
  `version.json` and opens Android's installer (Android asks once to allow installs from Agent
  Setu). Falls back to the release page when the link or checksum is missing or does not match.
- Fix: the recovery code now appears at first PIN setup (it was shown only when changing the PIN
  from Settings). PINs set on 1.1.1 have no code until changed once.

## 1.1.1 (versionCode 5) - policy numbers, Forgot PIN, face unlock, update pop-up; published 28-09-2026

- Optional full policy/account number on a policy (shown in full only on its own screen; last 4
  digits everywhere else; never Aadhaar, PAN or bank accounts). Schema 4.
- Forgot PIN: 8-digit recovery code shown once when a PIN is set, or the phone's own lock; either
  lets you set a new PIN. Erasing data remains the last resort.
- Unlock with fingerprint, face or phone lock (was fingerprint only).
- Update pop-up on Today once per launch when a newer version is found.

## 1.1.0 (versionCode 4) - received commission, own rules, small fixes; published 28-09-2026

(1.0.3 was prepared with versionCode 4 but never published; its fixes ship here.)

- Receipts: a ledger entry can hold several receipts (part payments), each with amount, date, mode
  (cash / POSB account / bank / other), voucher or reference number and a note. The entry's received
  total and status follow its receipts. Amounts marked received in 1.0.x become one receipt each.
- Monthly incentive statement: enter the department's statement for a month, see the difference
  against expected and received, and mark all pending entries of the month received from it.
- Rates and rules: add your own rule for any product (+ button), and add a custom product (name in
  both languages, type) with its commission. Custom products appear in Add business.
- Customer page shows the customer's commission history.
- Database schema 3 with a hand-written migration checked by a unit test against Room's exported
  schema; backups from 1.0.x (schema 2) restore and are upgraded.
- Small fixes from the unpublished 1.0.3: edit or delete a policy/account; calendar picker on every
  date field; reminder time setting; appearance setting (same as phone / light / dark);
  add-customer button on Today; Undo after acting on a reminder; single-line tab labels ("Ledger");
  compact search box.

## 1.0.2 (versionCode 3) - new colours, published 28-09-2026

- Colour scheme changed from teal/indigo to navy and sea green: app icon, buttons, tabs, card
  tints, notification tint, share-card divider. No feature changes; installs over 1.0.1 with data
  kept.

## 1.0.1 (versionCode 2) - smaller download, published 28-09-2026 (full release)

- APK about 7 MB instead of about 22 MB: only the ARM copies of the database-encryption library
  (every Android 8+ phone is ARM), stored compressed. No feature changes; installs over 1.0.0 with
  data kept.

## 1.0.0 (versionCode 1) - first pilot release, published 27-09-2026 (pre-release)

First version for the pilot group. Everything the roadmap lists for the MVP:

- Hindi and English; non-affiliation disclaimer on first start; staff-type profile.
- Customers (with consent), add business with the expected commission shown instantly, month-wise
  commission ledger with "mark received".
- Editable commission rules and interest rates with dates, order numbers and "verified" marks
  (sample values included; verify before relying on them).
- Reminders: premiums, RD instalments, maturities (30/15/7/1 days), follow-ups; one daily
  notification; "Collected" adds the commission to the ledger.
- TD, RD and MIS calculators; scheme cards for 10 schemes; both can be shared as images.
- Optional PIN and fingerprint lock; password-protected backup and restore; manual error report;
  delete a customer or all data.
- Update check against `release/version.json`; signing-key fingerprint on the About screen.

Known limits: commission sample rates are unverified; database changes after this version need
proper migrations.
