package app.agentsetu.core.calc

import java.time.LocalDate

/**
 * The no-history-loss rule shared by every dated rate: a new value from a later date closes the
 * old row the day before; a new value from the same or an earlier date replaces the old row, which
 * is then soft-deleted (kept, not overwritten).
 */
object DatedRevision {
    sealed interface Old {
        data class CloseOn(val effectiveTo: LocalDate) : Old
        data object SoftDelete : Old
    }

    fun plan(currentFrom: LocalDate, currentTo: LocalDate?, newFrom: LocalDate): Old {
        currentTo?.let { require(!newFrom.isAfter(it)) { "The current value already ended on $it" } }
        return if (newFrom.isAfter(currentFrom)) Old.CloseOn(newFrom.minusDays(1)) else Old.SoftDelete
    }
}
