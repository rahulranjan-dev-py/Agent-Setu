package app.agentsetu.core.update

/** How the in-app updater behaves on a slow or patchy network. */
object DownloadPolicy {
    /** Stall (no bytes at all) before one attempt is given up; a slow but steady link never times out. */
    const val STALL_TIMEOUT_MS = 60_000

    /** Automatic reconnects after the first attempt, each resuming where the last stopped. */
    const val MAX_RETRIES = 3

    /** Pause before automatic retry number [retry] (1-based): 2 s, 4 s, 6 s. */
    fun retryDelayMs(retry: Int): Long = 2_000L * retry.coerceIn(1, MAX_RETRIES)

    /**
     * The HTTP Range header to resume a partial file of [downloadedBytes], or null to start afresh.
     * Servers that ignore it answer 200 with the whole file, which the downloader handles by
     * truncating and starting over.
     */
    fun rangeHeader(downloadedBytes: Long): String? = if (downloadedBytes > 0) "bytes=$downloadedBytes-" else null
}
