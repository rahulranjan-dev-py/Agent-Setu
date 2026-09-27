# Decisions log

Decisions made after roadmap v1.2. Newest first.

| Date | Decision | Detail |
|---|---|---|
| 27-09-2026 | **Crash reporting: manual "Send error report" button only** | No Firebase Crashlytics or any other crash SDK. On an uncaught exception the app saves the stack trace locally (no customer data). Settings → "Send error report" shows the report to the user (app version, Android version, device model, stack trace), then opens the Android share sheet so the user chooses WhatsApp or email. Keeps the "no tracking SDK" guardrail intact. |
| 27-09-2026 | **Rates ship as editable sample data** | Commission and interest rates collected from PoTools, SAPost PO Tools, Postalstudy and news sites are bundled as sample seed files, marked unverified. Users edit them in Settings → Rates & rules. See [COMMISSION_RATES.md](COMMISSION_RATES.md). |
| 27-09-2026 | **Name confirmed: Agent Setu** | Launcher label "Agent Setu", tagline "Grahak se commission tak - aapka setu". |

## Open questions (owner to decide)

| Question | Why it matters |
|---|---|
| Final package name (e.g. `in.agentsetu.app`) | Permanent; cannot change after first release without users reinstalling. |
| Is this GitHub repository public or private? | The keystore, passwords and any personal notes must never be committed either way; a public repo also exposes work-in-progress. |
| Verified order copies for the rates | Needed to replace sample rates and settle the PLI renewal 1% vs 2% conflict. |
