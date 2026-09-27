package app.agentsetu.backup

import androidx.room.withTransaction
import app.agentsetu.BuildConfig
import app.agentsetu.core.backup.BackupCrypto
import app.agentsetu.core.backup.BackupException
import app.agentsetu.data.db.AgentSetuDatabase
import app.agentsetu.data.db.CommissionEntryEntity
import app.agentsetu.data.db.CommissionRuleEntity
import app.agentsetu.data.db.CustomerEntity
import app.agentsetu.data.db.HoldingEntity
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
        // Each app version can read backups of its own schema; older ones need a migration first.
        if (payload.schemaVersion > AgentSetuDatabase.VERSION) return@withContext RestoreResult.FromNewerApp
        if (payload.schemaVersion < AgentSetuDatabase.VERSION) return@withContext RestoreResult.WrongPasswordOrDamaged

        val dao = db.backupDao()
        db.withTransaction {
            dao.clearReminders(); dao.clearEntries(); dao.clearHoldings(); dao.clearLeads(); dao.clearCustomers()
            dao.clearTargets(); dao.clearRules(); dao.clearInterestRates(); dao.clearProducts(); dao.clearProfiles()
            dao.insertProfiles(payload.profiles)
            dao.insertProducts(payload.products)
            dao.insertInterestRates(payload.interestRates)
            dao.insertRules(payload.rules)
            dao.insertTargets(payload.targets)
            dao.insertCustomers(payload.customers)
            dao.insertLeads(payload.leads)
            dao.insertHoldings(payload.holdings)
            dao.insertEntries(payload.entries)
            dao.insertReminders(payload.reminders)
        }
        RestoreResult.Restored(customers = payload.customers.count { !it.deleted })
    }
}
