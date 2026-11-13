package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val storeName: String,
    val purpose: String,
    val totalAmountToman: Long,
    val userShareToman: Long,
    val dongAmountToman: Long = 0L,
    val dongFriendName: String = "",
    val weekdayDate: String, // e.g., "جمعه ۱ آبان"
    val timeStr: String,     // e.g., "۱۸:۱۰"
    val cardInfo: String,    // e.g., "کارت بلو ۶۲۱۹"
    val type: String,        // "EXPENSE", "INCOME", "SELF_TRANSFER", "GOLD_SAVE"
    val isUntagged: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    val subtitle: String
        get() = "$weekdayDate • $timeStr • $cardInfo"

    val shortBankName: String
        get() = cardInfo
            .replace("کارت", "")
            .replace(Regex("[۰-۹0-9]+"), "")
            .trim()
            .ifEmpty { "بلو" }

    val simpleSubtitle: String
        get() = "$weekdayDate • $timeStr • $shortBankName"
}

@Entity(tableName = "commitments")
data class CommitmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // "SAYYAD_ISSUED", "SAYYAD_RECEIVED", "LOAN_INSTALLMENT", "DONG"
    val title: String,
    val counterparty: String,
    val amountToman: Long,
    val dueDateStr: String,
    val sayyadId: String = "",
    val statusText: String,
    val isSettled: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val dailySafeBudgetToman: Long = 680_000L,
    val monthlyBalanceToman: Long = 18_450_000L,
    val paydayStartDay: Int = 1,
    val paydayCycleLabel: String = "۱ آبان تا ۳۰ آبان ۱۴۰۳",
    val autoDetectSalary: Boolean = true,
    val goldPrice18kToman: Long = 4_360_000L,
    val goldReserveToman: Long = 48_500_000L,
    val goldGrams: Double = 11.12,
    val dailyAutoSaveEnabled: Boolean = true
)

@Dao
interface OrosiDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    suspend fun getAllTransactionsNow(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(txs: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(tx: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("SELECT * FROM commitments ORDER BY isSettled ASC, timestamp DESC, id DESC")
    fun observeCommitments(): Flow<List<CommitmentEntity>>

    @Query("SELECT * FROM commitments ORDER BY isSettled ASC, timestamp DESC, id DESC")
    suspend fun getAllCommitmentsNow(): List<CommitmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommitment(commitment: CommitmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommitments(commitments: List<CommitmentEntity>)

    @Update
    suspend fun updateCommitment(commitment: CommitmentEntity)

    @Query("DELETE FROM commitments")
    suspend fun clearAllCommitments()

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsNow(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettingsEntity)
}

@Database(
    entities = [TransactionEntity::class, CommitmentEntity::class, AppSettingsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class OrosiDatabase : RoomDatabase() {
    abstract fun orosiDao(): OrosiDao

    companion object {
        @Volatile
        private var INSTANCE: OrosiDatabase? = null

        fun getInstance(context: Context): OrosiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OrosiDatabase::class.java,
                    "orosi_finance_offline.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
