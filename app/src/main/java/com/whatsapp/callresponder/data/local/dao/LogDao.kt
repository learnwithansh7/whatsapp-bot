package com.whatsapp.callresponder.data.local.dao

import androidx.room.*
import com.whatsapp.callresponder.data.local.entity.CallLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM call_logs ORDER BY callTimestamp DESC LIMIT 100")
    fun getAllLogs(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CallLogEntity): Long

    @Query("SELECT * FROM call_logs WHERE callerName = :name ORDER BY callTimestamp DESC LIMIT 1")
    suspend fun getLastLogForCaller(name: String): CallLogEntity?

    @Query("DELETE FROM call_logs")
    suspend fun clearAllLogs()
}
