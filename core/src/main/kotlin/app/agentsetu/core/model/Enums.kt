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
    ;

    /** A rule written for [ruleType] also applies to this user. A GDS BPM earns every GDS incentive too. */
    fun isCoveredBy(ruleType: StaffType): Boolean =
        this == ruleType || (this == GDS_BPM && ruleType == GDS)
}

enum class ProductGroup { PLI, RPLI, SAVINGS, IPPB, OTHER }

/** PLI incentive bands differ for Anticipated Endowment Assurance; everything else uses ANY. */
enum class PolicyCategory { ANY, AEA, NON_AEA }

/** How a commission rule turns a transaction into an amount. */
enum class CommissionBasis {
    PERCENT_OF_PREMIUM,
    PERCENT_OF_DEPOSIT,
    PERCENT_OF_NET_ACCRETION,

    /** `rate` is a rupee amount per case, not a percentage. */
    FLAT_PER_CASE,
}

enum class Confidence { HIGH, MEDIUM, LOW }

/** Pipeline lanes from the roadmap, plus the two ways a lead ends. */
enum class LeadStage { CONTACTED, INTERESTED, PROPOSAL, MEDICAL_DOCS, ISSUED, LOST }

enum class HoldingStatus { ACTIVE, LAPSED, MATURED, CLOSED, SURRENDERED }

enum class PaymentFrequency(val monthsBetween: Int?) {
    MONTHLY(1),
    QUARTERLY(3),
    HALF_YEARLY(6),
    YEARLY(12),
    ONE_TIME(null),
}

enum class ReminderType { PREMIUM_DUE, INSTALMENT_DUE, MATURITY, FOLLOW_UP }

enum class CommissionStatus {
    EXPECTED,
    PARTLY_RECEIVED,
    RECEIVED,

    /** No rule matched when the entry was created; the user must add or fix a rule. */
    NO_RULE,
}

/** How a commission payment reached the user. */
enum class ReceiptMode {
    CASH,

    /** Credited to the user's Post Office Savings Bank account. */
    POSB,
    BANK,

    /** Anything else, and receipts recorded before modes existed. */
    OTHER,
}
