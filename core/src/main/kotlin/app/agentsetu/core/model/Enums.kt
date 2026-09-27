package app.agentsetu.core.model

/** Who is earning. Values match the `staffTypes` strings in data/seed/commission_rules.sample.json. */
enum class StaffType {
    DEPARTMENTAL,
    GDS,
    GDS_BPM,
    DIRECT_AGENT,
    FIELD_OFFICER,
    ODE,
    SAS_AGENT,
    MPKBY_AGENT,
}

enum class ProductGroup { PLI, RPLI, SAVINGS, IPPB, OTHER }

/** How a commission rule turns a transaction into an amount. */
enum class CommissionBasis {
    PERCENT_OF_PREMIUM,
    PERCENT_OF_DEPOSIT,
    PERCENT_OF_NET_ACCRETION,
    FLAT_PER_CASE,
}

enum class Confidence { HIGH, MEDIUM, LOW }
