package com.whatsapp.callresponder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.whatsapp.callresponder.data.local.dao.LogDao
import com.whatsapp.callresponder.data.local.dao.SettingsDao
import com.whatsapp.callresponder.data.local.dao.TemplateDao
import com.whatsapp.callresponder.data.local.entity.CallLogEntity
import com.whatsapp.callresponder.data.local.entity.SettingsEntity
import com.whatsapp.callresponder.data.local.entity.TemplateEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TemplateEntity::class, CallLogEntity::class, SettingsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun templateDao(): TemplateDao
    abstract fun logDao(): LogDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "whatsapp_call_responder.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default presets
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            database.templateDao().insertTemplate(
                                TemplateEntity(
                                    title = "I'm Busy / In a Meeting",
                                    content = "Hey {name}, I missed your call at {time}. I am currently busy and will call you back shortly!",
                                    isActive = true
                                )
                            )
                            database.templateDao().insertTemplate(
                                TemplateEntity(
                                    title = "Driving",
                                    content = "Hi {name}, I'm driving right now. I'll get back to you as soon as I arrive at my destination.",
                                    isActive = false
                                )
                            )
                            database.templateDao().insertTemplate(
                                TemplateEntity(
                                    title = "Sleeping / Night",
                                    content = "Hi {name}, I'm currently asleep. I'll check your message and call you back in the morning.",
                                    isActive = false
                                )
                            )
                            database.settingsDao().upsertSettings(
                                SettingsEntity()
                            )
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
