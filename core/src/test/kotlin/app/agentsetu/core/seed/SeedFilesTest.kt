package app.agentsetu.core.seed

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the bundled sample rate files: they must parse, be internally consistent and stay marked as samples. */
class SeedFilesTest {

    private val seedDir = File(requireNotNull(System.getProperty("agentsetu.seedDir")) { "agentsetu.seedDir not set" })

    @Test
    fun commissionSeedIsValid() {
        val file = SeedJson.parseCommission(File(seedDir, SeedJson.COMMISSION_FILE).readText())
        assertEquals(emptyList<String>(), SeedValidator.validate(file))
        assertTrue(file.commissionRules.isNotEmpty())
        assertTrue("bundled rules must be samples", file.commissionRules.all { it.isSample && !it.verified })
    }

    @Test
    fun interestSeedIsValid() {
        val file = SeedJson.parseInterest(File(seedDir, SeedJson.INTEREST_FILE).readText())
        assertEquals(emptyList<String>(), SeedValidator.validate(file))
        assertTrue(file.interestRates.isNotEmpty())
        assertTrue("bundled rates must be samples", file.interestRates.all { it.isSample && !it.verified })
    }

    @Test
    fun validatorCatchesBadRules() {
        val good = SeedJson.parseCommission(File(seedDir, SeedJson.COMMISSION_FILE).readText())
        val rule = good.commissionRules.first()
        val bad = good.copy(
            commissionRules = listOf(
                rule,
                rule.copy(staffTypes = listOf("POSTMASTER_GENERAL"), rate = 150.0, verified = true),
            ),
        )
        val problems = SeedValidator.validate(bad)
        assertTrue(problems.any { it.startsWith("duplicate id") })
        assertTrue(problems.any { "unknown staffType" in it })
        assertTrue(problems.any { "outside 0-100" in it })
        assertTrue(problems.any { "cannot be marked verified" in it })
    }
}
