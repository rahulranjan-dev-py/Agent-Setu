package app.agentsetu.core.update

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class UpdateCheckTest {
    private val info = VersionInfo(
        latestVersionCode = 5,
        latestVersionName = "1.0.4",
        downloadUrl = "https://github.com/x/y/releases",
        minSupportedVersionCode = 3,
    )

    @Test
    fun decisions() {
        assertEquals(UpdateStatus.Required(info), UpdateCheck.evaluate(2, info))
        assertEquals(UpdateStatus.Available(info), UpdateCheck.evaluate(4, info))
        assertEquals(UpdateStatus.UpToDate, UpdateCheck.evaluate(5, info))
        assertEquals(UpdateStatus.UpToDate, UpdateCheck.evaluate(6, info))
    }

    @Test
    fun rejectsBadFiles() {
        assertNull(UpdateCheck.parse("<html>not found</html>"))
        assertNull(UpdateCheck.parse("""{"latestVersionCode":2,"latestVersionName":"x","downloadUrl":"http://evil"}"""))
        assertNotNull(UpdateCheck.parse("""{"latestVersionCode":2,"latestVersionName":"x","downloadUrl":"https://github.com/a","extra":1}"""))
    }

    @Test
    fun publishedVersionFileIsValid() {
        val file = File(requireNotNull(System.getProperty("agentsetu.versionFile")))
        val parsed = UpdateCheck.parse(file.readText())
        assertNotNull("release/version.json must parse", parsed)
        assertTrue(parsed!!.changelogHi.isNotBlank() && parsed.changelogEn.isNotBlank())
    }

    @Test
    fun inAppInstallNeedsApkLinkAndChecksum() {
        val sha = "a".repeat(64)
        val ok = VersionInfo(5, "1.1.1", "https://example.org/releases", sha256 = sha, apkUrl = "https://example.org/AgentSetu-v1.1.1.apk")
        assertTrue(UpdateCheck.canInstallInApp(ok))
        assertFalse(UpdateCheck.canInstallInApp(ok.copy(apkUrl = "")))
        assertFalse(UpdateCheck.canInstallInApp(ok.copy(apkUrl = "http://example.org/a.apk")))
        assertFalse(UpdateCheck.canInstallInApp(ok.copy(apkUrl = "https://example.org/a.zip")))
        assertFalse(UpdateCheck.canInstallInApp(ok.copy(sha256 = "")))
        assertFalse(UpdateCheck.canInstallInApp(ok.copy(sha256 = "xyz")))
    }
}
