package com.whatsapp.callresponder.data.local.dao

import androidx.room.*
import com.whatsapp.callresponder.data.local.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY createdAt DESC")
    fun getAllTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTemplate(): TemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: TemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: TemplateEntity)

    @Query("UPDATE templates SET isActive = 0")
    suspend fun deactivateAll()

    @Transaction
    suspend fun setActiveTemplate(templateId: Long) {
        deactivateAll()
        activateTemplate(templateId)
    }

    @Query("UPDATE templates SET isActive = 1 WHERE id = :templateId")
    suspend fun activateTemplate(templateId: Long)
}
