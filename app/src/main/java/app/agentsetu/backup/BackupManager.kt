package app.agentsetu.backup

import androidx.room.withTransaction
import app.agentsetu.BuildConfig
import app.agentsetu.core.backup.BackupCrypto
import app.agentsetu.core.backup.BackupException
import app.agentsetu.core.model.ReceiptMode
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.CommissionEntryEntity
import app.agentsetu.data.db.CommissionReceiptEntity
import app.agentsetu.data.db.CommissionRuleEntity
import app.agentsetu.data.db.CustomerEntity
import app.agentsetu.data.db.HoldingEntity
import app.agentsetu.data.db.IncentiveStatementEntity
import app.agentsetu.data.db.InterestRateEntity
import app.agentsetu.data.db.LeadEntity
import app.agentsetu.data.db.ProductEntity
import app.agentsetu.data.db.ReminderEntity
import app.agentsetu.data.db.TargetEntity
import app.agentsetu.data.db.UserProfileEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Everything in the database. Rules and rates are included so the user's verified rates move too. */
@Serializable
data class BackupPayload(
    val schemaVersion: Int,
    val appVersion: String,
    val createdAt: Long,
    val profiles: List<UserProfileEntity>,
    val customers: List<CustomerEntity>,
    val products: List<ProductEntity>,
    val leads: List<LeadEntity>,
    val holdings: List<HoldingEntity>,
    val rules: List<CommissionRuleEntity>,
    val entries: List<CommissionEntryEntity>,
    val reminders: List<ReminderEntity>,
    val targets: List<TargetEntity>,
    val interestRates: List<InterestRateEntity>,
    // Schema 3 (1.1.0). Absent in older backups, which restore with empty lists.
    val receipts: List<CommissionReceiptEntity> = emptyList(),
    val statements: List<IncentiveStatementEntity> = emptyList(),
)

sealed interface RestoreResult {
    data class Restored(val customers: Int) : RestoreResult
    data object WrongPasswordOrDamaged : RestoreResult
    data object NotABackup : RestoreResult
    data object FromNewerApp : RestoreResult
}

@Singleton
class BackupManager @Inject constructor(private val db: AgentSetuDatabase) {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun export(password: String): ByteArray = withContext(Dispatchers.IO) {
        val dao = db.backupDao()
        val payload = db.withTransaction {
            BackupPayload(
                schemaVersion = AgentSetuDatabase.VERSION,
                appVersion = BuildConfig.VERSION_NAME,
                createdAt = System.currentTimeMillis(),
                profiles = dao.profiles(),
                customers = dao.customers(),
                products = dao.products(),
                leads = dao.leads(),
                holdings = dao.holdings(),
                rules = dao.rules(),
                entries = dao.entries(),
                reminders = dao.reminders(),
                targets = dao.targets(),
                interestRates = dao.interestRates(),
                receipts = dao.receipts(),
                statements = dao.statements(),
            )
        }
        val plain = json.encodeToString(BackupPayload.serializer(), payload).toByteArray(Charsets.UTF_8)
        BackupCrypto.encrypt(plain, password)
    }

    /** Replaces everything on this phone with the backup's contents, all or nothing. */
    suspend fun restore(file: ByteArray, password: String): RestoreResult = withContext(Dispatchers.IO) {
        val payload = try {
            val plain = BackupCrypto.decrypt(file, password)
            json.decodeFromString(BackupPayload.serializer(), plain.toString(Charsets.UTF_8))
        } catch (e: BackupException) {
            return@withContext when (e.reason) {
                BackupException.Reason.NOT_A_BACKUP -> RestoreResult.NotABackup
                BackupException.Reason.NEWER_FORMAT -> RestoreResult.FromNewerApp
                BackupException.Reason.WRONG_PASSWORD_OR_DAMAGED -> RestoreResult.WrongPasswordOrDamaged
            }
        } catch (e: SerializationException) {
            return@withContext RestoreResult.WrongPasswordOrDamaged
        } catch (e: IllegalArgumentException) {
            // A row that breaks a data rule (e.g. a full account number) is treated as damaged.
            return@withContext RestoreResult.WrongPasswordOrDamaged
        }
        // A newer app's backup may hold data this version cannot keep; older ones are upgraded below.
        if (payload.schemaVersion > AgentSetuDatabase.VERSION) return@withContext RestoreResult.FromNewerApp
        if (payload.schemaVersion < AgentSetuDatabase.OLDEST_RESTORABLE_VERSION) return@withContext RestoreResult.WrongPasswordOrDamaged
        val upgraded = upgrade(payload)

        val dao = db.backupDao()
        db.withTransaction {
            dao.clearReminders(); dao.clearStatements(); dao.clearReceipts(); dao.clearEntries(); dao.clearHoldings()
            dao.clearLeads(); dao.clearCustomers()
            dao.clearTargets(); dao.clearRules(); dao.clearInterestRates(); dao.clearProducts(); dao.clearProfiles()
            dao.insertProfiles(upgraded.profiles)
            dao.insertProducts(upgraded.products)
            dao.insertInterestRates(upgraded.interestRates)
            dao.insertRules(upgraded.rules)
            dao.insertTargets(upgraded.targets)
            dao.insertCustomers(upgraded.customers)
            dao.insertLeads(upgraded.leads)
            dao.insertHoldings(upgraded.holdings)
            dao.insertEntries(upgraded.entries)
            dao.insertReceipts(upgraded.receipts)
            dao.insertStatements(upgraded.statements)
            dao.insertReminders(upgraded.reminders)
        }
        RestoreResult.Restored(customers = upgraded.customers.count { !it.deleted })
    }

    /** Brings an older backup up to the current schema, mirroring Migrations.kt. */
    private fun upgrade(payload: BackupPayload): BackupPayload {
        var p = payload
        if (p.schemaVersion < 3) {
            // 2 -> 3: one receipt per amount marked received in 1.0.x (see Migrations.V3.BACKFILL_RECEIPTS).
            val receipts = p.entries.filter { !it.deleted && (it.receivedPaise ?: 0) > 0 }.map { e ->
                CommissionReceiptEntity(
                    entryId = e.id,
                    amountPaise = e.receivedPaise!!,
                    date = e.receivedDate ?: java.time.LocalDate.parse(e.period + "-01"),
                    mode = ReceiptMode.OTHER,
                    createdAt = e.updatedAt,
                    updatedAt = e.updatedAt,
                )
            }
            p = p.copy(schemaVersion = 3, receipts = receipts, statements = emptyList())
        }
        return p
    }
}
