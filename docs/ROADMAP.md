# Agent Setu: product roadmap (v1.2, 27 Sep 2026)

> *Grahak se commission tak - aapka setu* · From customer to commission - your bridge
>
> **Independent, unofficial application.** Not developed, endorsed, sponsored or operated by India
> Post, the Department of Posts, the Ministry of Communications or India Post Payments Bank. It
> does not connect to any departmental system.

This is a working summary of the roadmap PDF (v1.2). Decisions made after v1.2 are in
[DECISIONS.md](DECISIONS.md).

## 1. Vision

Give every departmental employee and GDS who sells PLI, RPLI, savings and other incentive-bearing
products one simple, private, offline place to track customers, follow-ups, renewals, maturities
and the commission they have earned.

| Item | Plan |
|---|---|
| Platform | Android 8.0+ (minSdk 26), phone-first, 2–3 GB RAM devices |
| Languages | Hindi and English at launch; Bengali later |
| Price | Free, no ads, no in-app purchases, no sponsorship |
| Distribution | Signed APK via WhatsApp group + GitHub Releases; no Play Store |
| Login | None at launch (Mode A); optional login designed for v2.0 (Mode B) |
| Timeline | MVP in about 16 weeks; first WhatsApp release about week 20 (part-time, solo) |
| Running cost | About ₹0 in Mode A |

## 2. Brand

- Launcher label **Agent Setu** (confirmed). Colours teal and indigo. Avoid postal red and yellow.
- Original icon (handshake or notebook with a tick). No emblem, envelope logo or uniform imagery.
- Package name: permanent, must not contain `gov`, `indiapost`, `dop` or `ippb`. **Still to be chosen.**
- Release file name: `AgentSetu-vX.Y.Z.apk`.
- Before first release: search Play Store and web for "Agent Setu" and run a trademark search on the
  IP India registry (classes 9 and 42).

## 3. Users and problems

| # | Problem | Feature |
|---|---|---|
| 1 | Renewal premiums and RD instalments missed | Due-date reminders + "this week's collections" |
| 2 | TD/MIS/NSC/KVP maturities pass without a reinvestment talk | Maturity radar: 30/15/7-day alerts with Call/WhatsApp |
| 3 | Leads forgotten | Lead pipeline with a next follow-up date on every lead |
| 4 | Incentive earned vs received unclear | Commission ledger with reconciliation |
| 5 | Products hard to explain | Hindi scheme cards and calculators to share |
| 6 | Target progress unknown | Personal target dashboard |

Users: GDS (BPM / ABPM / Dak Sevak), departmental staff (PA / SPM / Postman / MTS). Supervisors
only in Mode B, with aggregate data only.

## 4. Products

The app never hard-codes a rate. See [COMMISSION_RATES.md](COMMISSION_RATES.md).

| Group | Products | Release |
|---|---|---|
| PLI | Proposals, first premium, renewals, revival | MVP |
| RPLI | Same lifecycle as PLI | MVP |
| Small savings | TD, RD, MIS, NSC, KVP, SCSS, PPF, SSA | MVP |
| IPPB / banking | Account opening, doorstep, DLC, insurance referrals | v1.5 |
| Other | Aadhaar work, custom products | v1.5 |

## 5. Features by release

| Module | MVP (v1.0) | v1.5 | v2.0 |
|---|---|---|---|
| Onboarding | Staff type, designation, office, division, agent codes, language | Multiple agent codes | Login and sync |
| Customers & leads | Add customer, lead status, next follow-up, Call/WhatsApp | Contacts import (opt-in), tags, family groups | Referral tracking |
| Pipeline | PLI/RPLI proposal stages; savings accounts | Custom products and stages | Document checklist |
| Commission ledger | Editable rule table, expected amount, mark received, month view | Reconciliation report | Year-wise statement |
| Reminders | Premium, RD instalment, maturity 30/15/7, follow-ups | Daily digest | "Best customers to call today" |
| Calculators | TD, RD, MIS (editable rates) | NSC, KVP, SCSS, PPF, SSA | Indicative PLI/RPLI premium |
| Share | Hindi/English scheme cards as image | Card with agent's name and phone | Audio explainers |
| Targets | Monthly target + progress ring | Product-wise targets | Trend charts |
| Data safety | PIN/biometric lock, encrypted DB, encrypted backup file | Backup reminder, restore wizard | Cloud sync |
| Updates | In-app "new version" check | Hindi changelog screen | - |
| Reports | Monthly summary | PDF/Excel export | Supervisor aggregate |
| Errors | Manual "Send error report" button | - | - |

**MVP rule:** if a feature does not help the user remember a customer, act on a due date or know
their earnings, it waits for v1.5.

## 6. Guardrails: the app must never

- ask for or store departmental logins (Finacle, CSI/McCamish, IPPB, UIDAI client, etc.);
- scrape, automate or connect to any departmental portal or API;
- collect full Aadhaar, PAN or bank account numbers (at most the last 4 digits of a policy/account number);
- accept, collect or route money;
- promise returns beyond official scheme rules;
- use the India Post logo, colours, emblem or words like "official" or "authorised";
- include any ad, analytics or tracking SDK (crash reports are manual only, see DECISIONS.md);
- ask users to install APKs from anyone other than the owner.

## 7. Compliance checklist (owner's actions)

- CCS (Conduct) Rules 1964, Rule 15: written intimation to, or permission from, the competent
  authority through the Divisional Head before wider distribution. Accept no money or sponsorship.
  Develop on your own time and devices.
- Use a separate "Agent Setu Users" group, not official groups; repeat the disclaimer in every release post.
- DPDP Act 2023: minimum data, consent note, per-customer delete, "Delete all my data", and a
  Hindi + English privacy notice on GitHub Pages ([draft](PRIVACY.md)).
- Android developer verification for sideloaded apps: register before global enforcement in 2027.
- Permissions: notifications is the only one users are asked for (plus "install unknown apps" if
  in-app APK download is added). WorkManager adds install-time permissions without a prompt (wake
  lock, boot completed, network state). No SMS, call log, contacts or location.
- Scheme text written in our own words from public sources, dated, with a "rules may change" note.

## 8. Technology

Kotlin + Jetpack Compose (Material 3) · MVVM · Room + SQLCipher (key in Android Keystore) · Hilt ·
WorkManager (nightly reminder scan, update check) · local notifications · `strings.xml` for Hindi
and English · password-protected backup file via the share sheet · `version.json` on GitHub
Pages/Releases for update checks.

**Sync-ready from day one:** every record has a UUID `id`, `createdAt`, `updatedAt` and a
`deleted` flag.

## 9. Data model (MVP)

| Table | Key fields |
|---|---|
| UserProfile | staffType, designation, officeName, division, agentCodes[], language |
| Customer | name, mobile, village, dob?, tags, consentGiven, notes (no Aadhaar/PAN) |
| Lead | customerId, productId, stage, nextFollowUp, source, lostReason |
| Product | group, name, isCustom, active |
| Holding | customerId, productId, refLast4, amount/sumAssured, premium/instalment, frequency, startDate, maturityDate, status |
| CommissionRule | see [COMMISSION_RATES.md §6](COMMISSION_RATES.md#6-how-rates-stay-editable) |
| CommissionEntry | holdingId, period, expectedAmount, receivedAmount, receivedDate, status |
| Reminder | type, dueDate, holdingId or leadId, done |
| Target | month, productGroup, targetValue, achievedValue |
| InterestRate | scheme, rate, compounding, effectiveFrom, source |

## 10. Screens

Welcome + disclaimer · Home/Today · Customers · Pipeline (Contacted → Interested → Proposal →
Medical/Docs → Issued) · Add business · Commission · Calculators · Scheme cards · Settings.

Design rules: large tap targets, one-handed use, fully offline, Indian number format (1,00,000),
dates DD-MM-YYYY, Hindi tested on a 5-inch phone.

## 11. Distribution (APK via WhatsApp)

- One release keystore for Agent Setu, separate from DakSetu. Keep two offline copies.
  **Never commit it to this repository**, and never send it over WhatsApp or email.
- Show the signing key's SHA-256 fingerprint on the About screen.
- Release steps: bump version → signed R8 release APK (under 15 MB) → test update-over-install on
  two phones → GitHub Release + SHA-256 → update `version.json` → admin-only WhatsApp post with
  APK, version, checksum, Hindi changelog and disclaimer.
- `version.json` includes a minimum supported version to force users off a bad release.

## 12. Timeline (10–12 h/week)

| Phase | Weeks | Exit criteria |
|---|---|---|
| 0 Groundwork | 1–3 | Interviews, orders collected, name check, permission letter sent |
| 1 Design | 4–6 | Prototype tested with 5 users |
| 2 Foundation | 7–9 | Installs, locks, encrypted storage, language switch |
| 3 Core MVP | 10–14 | All MVP features end-to-end on a low-end phone |
| 4 Hardening | 15–16 | 3 days of daily use without a crash; update-over-install tested |
| 5 Pilot | 17–20 | Top 10 feedback items fixed |
| 6 Rollout | 21–24 | 100+ active users |
| 7 v1.5 | months 7–9 | Users stay active month to month |
| 8 v2.0 / Mode B | months 10–14 | Only if Part B conditions are met |

Pilot: GDS of Benagoria, Birsinghpur, Barwa, Pandra, Poddardih and Kaliasol BOs plus staff at
Nirsa Chatti SO, then East Sub Division, then Dhanbad Division.

## 13. Part B: optional login (v2.0), in short

Add login only when at least two are true: users lose data on phone change; rate tables are hard to
keep correct; supervisors ask for team summaries; the app must be limited to postal staff; the
Division has confirmed that running an online service is acceptable.

Design: Google Sign-In + invite code + admin approval · Firestore in asia-south1 · strict Security
Rules (own data only) · admin-published rate tables · optional client-side encryption of customer
fields · Agent / Office Admin / Super Admin roles · "Download my data" and "Delete my account" ·
written breach plan · grievance contact. About 6–8 extra weeks part-time.
