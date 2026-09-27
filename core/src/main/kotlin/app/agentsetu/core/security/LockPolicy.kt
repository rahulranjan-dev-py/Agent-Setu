package app.agentsetu.core.security

/** When the app locks, and how long it waits after repeated wrong PINs. */
object LockPolicy {
    /** Returning after this long in the background asks for the PIN again. */
    const val BACKGROUND_TIMEOUT_MS = 2 * 60 * 1000L

    const val FREE_ATTEMPTS = 5
    private const val FIRST_WAIT_MS = 30_000L
    private const val MAX_WAIT_MS = 15 * 60 * 1000L

    fun shouldLockOnReturn(backgroundedAtMs: Long?, nowMs: Long): Boolean =
        backgroundedAtMs != null && nowMs - backgroundedAtMs >= BACKGROUND_TIMEOUT_MS

    /** Wait after [failures] consecutive wrong PINs: none for the first 5, then 30 s doubling up to 15 min. */
    fun lockoutMs(failures: Int): Long {
        if (failures < FREE_ATTEMPTS) return 0
        val doublings = (failures - FREE_ATTEMPTS).coerceAtMost(10)
        return (FIRST_WAIT_MS shl doublings).coerceAtMost(MAX_WAIT_MS)
    }
}
