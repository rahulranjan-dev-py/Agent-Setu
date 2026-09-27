# Changelog

Every published version of Agent Setu, newest first. The Hindi release notes for each version are in
[`release/notes/`](release/notes/); they are pasted into the GitHub Release and the WhatsApp post.

Version numbers: `versionName` (shown to users) follows 1.0.0 → 1.0.1 (fixes) → 1.1.0 (new
features); `versionCode` goes up by exactly 1 with every published APK.

## 1.0.0 (versionCode 1) - first pilot release, not yet published

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
