package app.agentsetu.core.report

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorReportTest {
    @Test
    fun masksLongNumbersButKeepsLineNumbers() {
        val trace = "java.lang.IllegalStateException: bad mobile 9876543210\n\tat app.Foo.bar(Foo.kt:1234)"
        val report = ErrorReport.build("0.1.0", "14 (API 34)", "Xiaomi Redmi 12", "28-09-2026 10:00", trace)
        assertFalse(report.contains("9876543210"))
        assertTrue(report.contains("##########"))
        assertTrue("short numbers such as line numbers stay", report.contains("Foo.kt:1234"))
        assertTrue(report.contains("App version: 0.1.0"))
    }
}
