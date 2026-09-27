package app.agentsetu.data.repo

import androidx.room.withTransaction
import app.agentsetu.data.db.AgentSetuDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepository @Inject constructor(private val db: AgentSetuDatabase) {

    /**
     * Deletes a customer as the privacy notice promises: their personal details are wiped at once,
     * and their policies, follow-ups and reminders are removed. The row itself stays only as an
     * empty marker (for a later sync); commission already earned stays in the ledger, unnamed.
     */
    suspend fun delete(customerId: String) = db.withTransaction {
        val customer = db.customerDao().get(customerId) ?: return@withTransaction
        val now = System.currentTimeMillis()
        db.customerDao().upsert(
            customer.copy(
                name = "",
                mobile = null,
                village = null,
                dob = null,
                tags = emptyList(),
                notes = "",
                deleted = true,
                updatedAt = now,
            ),
        )
        db.reminderDao().softDeleteForCustomer(customerId, now)
        db.holdingDao().softDeleteForCustomer(customerId, now)
        db.leadDao().softDeleteForCustomer(customerId, now)
    }
}
