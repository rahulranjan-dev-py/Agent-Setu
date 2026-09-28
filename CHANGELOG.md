# Changelog

Every published version of Agent Setu, newest first. The Hindi release notes for each version are in
[`release/notes/`](release/notes/); they are pasted into the GitHub Release and the WhatsApp post.

Version numbers: `versionName` (shown to users) follows 1.0.0 → 1.0.1 (fixes) → 1.1.0 (new
features); `versionCode` goes up by exactly 1 with every published APK.

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
