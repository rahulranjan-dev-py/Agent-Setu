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
    fun inAppInstallNeedsApkLinkUnderOurPrefixAndChecksum() {
        val prefix = "https://example.org/releases/download/"
        val sha = "a".repeat(64)
        val ok = VersionInfo(5, "1.1.1", "https://example.org/releases", sha256 = sha, apkUrl = prefix + "v1.1.1/AgentSetu-v1.1.1.apk")
        assertTrue(UpdateCheck.canInstallInApp(ok, prefix))
        assertFalse(UpdateCheck.canInstallInApp(ok.copy(apkUrl = ""), prefix))
        assertFalse(UpdateCheck.canInstallInApp(ok.copy(apkUrl = "http://example.org/releases/download/a.apk"), prefix))
        assertFalse(UpdateCheck.canInstallInApp(ok.copy(apkUrl = prefix + "a.zip"), prefix))
        assertFalse(UpdateCheck.canInstallInApp(ok.copy(apkUrl = "https://evil.example/releases/download/a.apk"), prefix))
        assertFalse(UpdateCheck.canInstallInApp(ok.copy(sha256 = ""), prefix))
        assertFalse(UpdateCheck.canInstallInApp(ok.copy(sha256 = "xyz"), prefix))
        assertFalse(UpdateCheck.canInstallInApp(ok, "http://example.org/"))
    }

    @Test
    fun publishedVersionFilePointsAtItsOwnRelease() {
        val file = File(requireNotNull(System.getProperty("agentsetu.versionFile")))
        val info = requireNotNull(UpdateCheck.parse(file.readText()))
        val expectedApk = "https://github.com/rahulranjan-dev-py/Agent-Setu/releases/download/v${info.latestVersionName}/AgentSetu-v${info.latestVersionName}.apk"
        assertEquals(expectedApk, info.apkUrl)
        assertTrue("sha256 must be published with apkUrl", Regex("^[0-9a-f]{64}$").matches(info.sha256))
        // The published version can never be ahead of the code being built.
        val gradleFile = File(System.getProperty("agentsetu.appBuildFile") ?: File(file.parentFile.parentFile, "app/build.gradle.kts").path)
        org.junit.Assume.assumeTrue("app/build.gradle.kts not available in this test setup", gradleFile.isFile)
        val builtCode = Regex("versionCode = (\\d+)").find(gradleFile.readText())!!.groupValues[1].toInt()
        assertTrue("version.json (${info.latestVersionCode}) is ahead of build.gradle.kts ($builtCode)", info.latestVersionCode <= builtCode)
    }

    @Test
    fun downloadPolicy() {
        assertEquals(2_000L, DownloadPolicy.retryDelayMs(1))
        assertEquals(6_000L, DownloadPolicy.retryDelayMs(3))
        assertEquals(6_000L, DownloadPolicy.retryDelayMs(9))
        assertEquals("bytes=1024-", DownloadPolicy.rangeHeader(1024))
        assertEquals(null, DownloadPolicy.rangeHeader(0))
    }
}
