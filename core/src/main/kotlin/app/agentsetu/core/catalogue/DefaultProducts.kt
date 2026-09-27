package app.agentsetu.core.catalogue

import app.agentsetu.core.model.ProductGroup
import java.util.UUID

data class CatalogueProduct(
    val id: String,
    val code: String,
    val group: ProductGroup,
    val nameEn: String,
    val nameHi: String,
    /**
     * Usual term, used only to suggest a maturity date the user can change. Not a rate; for KVP it
     * follows the current rate (115 months at 7.5%) and must be edited if the rate changes.
     */
    val defaultTermMonths: Int? = null,
) {
    val isInsurance: Boolean get() = group == ProductGroup.PLI || group == ProductGroup.RPLI
}

/**
 * Products seeded on first launch. Codes are shared with the rate seed files: a commission rule
 * for "TD" covers TD_1Y ... TD_5Y (see ProductCodes.covers). Users can add custom products later.
 */
object DefaultProducts {
    private val NAMESPACE: UUID = UUID.fromString("6f1c2a52-3b7e-4c1d-9a4e-2f5d8b0c7a11")

    private fun product(code: String, group: ProductGroup, en: String, hi: String, termMonths: Int? = null) =
        CatalogueProduct(stableId("product:$code"), code, group, en, hi, termMonths)

    val all: List<CatalogueProduct> = listOf(
        product("PLI", ProductGroup.PLI, "Postal Life Insurance (PLI)", "डाक जीवन बीमा (PLI)"),
        product("RPLI", ProductGroup.RPLI, "Rural Postal Life Insurance (RPLI)", "ग्रामीण डाक जीवन बीमा (RPLI)"),
        product("SB", ProductGroup.SAVINGS, "Savings Account", "बचत खाता"),
        product("TD_1Y", ProductGroup.SAVINGS, "Time Deposit - 1 year", "सावधि जमा - 1 वर्ष", 12),
        product("TD_2Y", ProductGroup.SAVINGS, "Time Deposit - 2 years", "सावधि जमा - 2 वर्ष", 24),
        product("TD_3Y", ProductGroup.SAVINGS, "Time Deposit - 3 years", "सावधि जमा - 3 वर्ष", 36),
        product("TD_5Y", ProductGroup.SAVINGS, "Time Deposit - 5 years", "सावधि जमा - 5 वर्ष", 60),
        product("RD_5Y", ProductGroup.SAVINGS, "Recurring Deposit - 5 years", "आवर्ती जमा - 5 वर्ष", 60),
        product("MIS", ProductGroup.SAVINGS, "Monthly Income Scheme", "मासिक आय योजना", 60),
        product("NSC", ProductGroup.SAVINGS, "National Savings Certificate", "राष्ट्रीय बचत पत्र", 60),
        product("KVP", ProductGroup.SAVINGS, "Kisan Vikas Patra", "किसान विकास पत्र", 115),
        product("SCSS", ProductGroup.SAVINGS, "Senior Citizens Savings Scheme", "वरिष्ठ नागरिक बचत योजना", 60),
        product("PPF", ProductGroup.SAVINGS, "Public Provident Fund", "लोक भविष्य निधि", 180),
        product("SSA", ProductGroup.SAVINGS, "Sukanya Samriddhi Account", "सुकन्या समृद्धि खाता", 252),
    )

    fun byCode(code: String): CatalogueProduct? = all.firstOrNull { it.code == code }

    /** Deterministic UUID so the same product has the same id on every install (sync-friendly). */
    fun stableId(key: String): String = UUID.nameUUIDFromBytes(NAMESPACE.toString().plus(key).toByteArray()).toString()
}

object ProductCodes {
    /** True when a rule written for [ruleCode] applies to a product with [productCode]. */
    fun covers(ruleCode: String, productCode: String): Boolean =
        ruleCode == productCode || productCode.startsWith(ruleCode + "_")

    fun isExact(ruleCode: String, productCode: String): Boolean = ruleCode == productCode
}
