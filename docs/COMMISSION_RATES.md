# Commission and interest rates: research notes and editing design

> **Status: sample data, not verified.** These rates were collected on 27 Sep 2026 from public
> websites (PoTools blog, Postalstudy, SAPost PO Tools, news sites) through search results. The
> original order copies could not be opened from the build environment. Before anyone relies on a
> rate, check it against the official order and record the order number in the app.

Seed files (loaded on first install, then fully editable):

- [`data/seed/commission_rules.sample.json`](../data/seed/commission_rules.sample.json): 19 rules
- [`data/seed/interest_rates.sample.json`](../data/seed/interest_rates.sample.json): 12 scheme rates

Every record has `isSample: true`, `verified: false`, a `confidence` level (`high` / `medium` /
`low`), the `sourceUrls` it came from, and an `orderRef` that says `VERIFY` until a user enters the
real order number.

## 1. PLI / RPLI incentive (sales force)

Applies to Departmental Employees, GDS, Direct Agents, Field Officers and, from 1 July 2026,
Other Departmental Employees (ODE, i.e. other Central/State Government employees up to Level 8).

| Product | Policy type | Premium-paying term | Rate | Paid on | Confidence |
|---|---|---|---|---|---|
| PLI | Non-AEA | up to 15 years | 4% | first-year premium | medium |
| PLI | Non-AEA | over 15, up to 25 years | 10% | first-year premium | medium |
| PLI | Non-AEA | over 25 years | 20% | first-year premium | medium |
| PLI | AEA | up to 15 years | 5% | first-year premium | medium |
| PLI | AEA | over 15 years | 7% | first-year premium | medium |
| PLI | any | - | 1% | renewal premium | **low** (see conflict) |
| RPLI | any | - | 10% | first-year premium | high |
| RPLI | any | - | 2.5% | renewal premium | high |

**Source disagreements to settle from the order copy:**

- **PLI renewal:** a DoP clarification dated 31.08.2020 gives Departmental Employees **2%** on cash
  PLI renewals. The 2023–2026 summaries say **1% for all sales force**. The seed uses 1%.
- **Which plans count as "AEA":** some summaries group WLA, CWLA, EA, Children Policy and Yugal
  Suraksha with the 5%/7% band, while the DoP wording names only AEA. Check the exact list.
- **June 2026 revision:** PoTools published "Revised Incentive Structure for PLI and RPLI" in June
  2026. The search results confirm the RPLI 10% / 2.5% figures but did not show the order number
  or any PLI changes.

Rules in force since 1 April 2025 (PSB minutes 17.03.2025) that affect the ledger:

- no Minimum Business Requirement for any sales force;
- a promoted Departmental Employee keeps renewal incentive on policies they procured earlier;
- procurement incentive goes to whoever holds the post's charge when the policy is indexed.

Supervisors' monitoring incentives are not in the MVP seed. The reported rates are 0.2% of GDS new
business premium for Mail Overseers, 0.8% of Departmental Employee new business premium for
Sub-Divisional Heads, and 1% of Direct Agent new business premium for Development Officers.

## 2. Small savings agent commission (SAS / MPKBY agents)

| Scheme | Commission | Confidence |
|---|---|---|
| RD (5-year) | 4% of deposit | medium |
| TD, MIS, NSC, KVP | 0.5% | medium |
| PPF, SCSS, SSA | none (PPF/SCSS removed in 2011) | medium |

These apply to authorised small-savings agents, **not** to Departmental Employees or GDS, who are
not agents. The seed attaches them only to the `SAS_AGENT` and `MPKBY_AGENT` staff types.

## 3. GDS Branch Postmaster incentive (DoP order dated 18.10.2021, as amended)

| Item | Rate | Confidence |
|---|---|---|
| 5-year TD opened at BO | 2% of deposit | **low** |
| 1/2/3-year TD | reported as 0.5%–1.0% (seed uses 0.5%) | **low** |
| SB net accretion | 1% (excludes March deposits, includes March withdrawals) | **low** |

Not payable if agency commission has already been paid on the same deposit. These figures come
from a single blog summary. Replace them with the figures from the order.

## 4. IPPB incentives (v1.5, not seeded)

Reported figures include ₹48.43 per Premium Khata opened with Aadhaar + UPI, ₹11.01 per child
enrolment (CELC), and about ₹312 on a health policy with roughly ₹8,000 premium. These are per-case
amounts that IPPB changes often, so they wait for v1.5, which adds the `FLAT_PER_CASE` basis and
custom products.

## 5. Small savings interest rates (Q2 FY2026-27: 1 Jul – 30 Sep 2026)

| Scheme | Rate | | Scheme | Rate |
|---|---|---|---|---|
| Savings account | 4.0% | | RD 5-year | 6.7% |
| TD 1-year | 6.9% | | MIS | 7.4% |
| TD 2-year | 7.0% | | SCSS | 8.2% |
| TD 3-year | 7.1% | | NSC | 7.7% |
| TD 5-year | 7.5% | | KVP | 7.5% (115 months) |
| PPF | 7.1% | | SSA | 8.2% |

The Government reviews these every quarter. Rates for **Oct–Dec 2026 are due about 30 Sep 2026**, so
refresh the seed before the first release.

## 6. How rates stay editable

The app never hard-codes a rate. On first launch it loads these files into the Room tables
`CommissionRule` and `InterestRate`. From then on:

1. **Settings → Rates & rules** lists every rule with its rate, effective date, order number and a
   `Sample` / `Verified` badge.
2. **Edit** any field: rate, basis, staff types, term band, effective from/to, order number, notes.
   Saving a changed rate does **not** overwrite history. It closes the old rule (`effectiveTo` =
   the day before) and creates a new one, so past commission entries keep the rate that applied
   when they were earned.
3. **Mark as verified** after entering the order number. The banner then reads *"Rates last updated
   on [date] from order [number]"*. Unverified sample rules show *"Sample rate - verify before
   relying on it"*.
4. **Add** a rule for any product, including custom products (v1.5), and **delete** (soft delete)
   rules that do not apply to the user.
5. **Reset to sample** reloads the bundled seed without touching the user's customers or ledger.
6. **Export / import** rules as a JSON file (same format as the seed), so one person can verify
   rates and share the file with colleagues over WhatsApp. In Mode B the admin publishes it
   centrally instead.

### Rule matching

For a holding, the ledger picks the rule where:
`productCode` matches, the user's `staffType` is in `staffTypes`, `policyCategory` matches (or is
`ANY`), the premium-paying term falls in `[minPremiumTermYears, maxPremiumTermYears]` (null = open),
`yearOfPolicy` matches (1 = procurement, 2 = any renewal year), and the transaction date falls in
`[effectiveFrom, effectiveTo]`. If more than one rule matches, the most specific one wins. If none
matches, the entry shows "No rule - add one", never a guessed amount.

> Data-model note: the roadmap lists a single `staffType` per rule. The seed uses a `staffTypes`
> list so one rule can cover all PLI sales force, and adds `policyCategory`, the term band,
> `confidence`, `verified`, `isSample` and `sourceUrls`.

## Sources

- [PoTools: Revised Incentive Structure for PLI and RPLI (June 2026)](https://www.potoolsblog.in/2026/06/revised-incentive-structure-for-pli-and.html)
- [PoTools: PLI/RPLI ODE framework (July 2026)](https://www.potoolsblog.in/2026/07/plirpli-odeother-departmental-employee.html)
- [PoTools: Changes in incentive structure (March 2025)](https://www.potoolsblog.in/2025/03/changes-in-incentive-structure-of.html)
- [PoTools: Rate of commission to small savings agents](https://www.potoolsblog.in/2016/10/rate-of-commission-to-small-savings.html)
- [SAPost PO Tools: PLI incentive structure / agent commission chart](https://sapostpotools.com/pli-incentive-structure-2024-agent-commission-chart-pdf/)
- [Postalstudy: Latest PLI & RPLI incentive structure (May 2026)](https://www.postalstudy.in/2026/05/latest-pli-rpli-incentive-structure.html)
- [Postalstudy: PLI/RPLI incentive structure 2026 clarification (April 2026)](https://www.postalstudy.in/2026/04/plirpli-incentive-structure-2026.html)
- [Postalstudy: GDS BPM incentive scheme 2026](https://www.postalstudy.in/2026/05/gds-bpm-incentive-scheme-2026-complete.html)
- [GConnect: PLI/RPLI clarification in respect of sales force](https://www.gconnect.in/news/promotional-incentive-monitoring-structure-pli-rpli.html)
- [GConnect: Incentive scheme for GDS BPMs, order 18.10.2021](https://www.gconnect.in/news/incentive-scheme-gds-branch-postmasters-dop.html)
- [Upstox: small savings agent commission](https://upstox.com/news/personal-finance/latest-updates/post-office-recurring-deposit-to-nsc-pomis-how-much-commission-do-small-savings-agents-get/article-186231/)
- [Business Today: small savings rates unchanged for Jul–Sep 2026](https://www.businesstoday.in/personal-finance/investment/story/govt-keeps-small-savings-interest-rates-unchanged-for-july-september-quarter-check-latest-ppf-scss-ssy-rates-540090-2026-06-30)
- [Central 8th Pay Commission: GDS incentive chart 2026 (IPPB)](https://central8thpaycommission.com/gds-incentive-chart-2026-ippb-pli-posb-rates/)
