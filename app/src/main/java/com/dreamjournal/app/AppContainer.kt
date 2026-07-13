package com.dreamjournal.app

import android.app.Application
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dreamjournal.app.data.local.DreamDatabase
import com.dreamjournal.app.data.repository.AudioPlayerManager
import com.dreamjournal.app.data.repository.AudioRecorderManager
import com.dreamjournal.app.data.repository.DreamRepository
import com.dreamjournal.app.data.repository.EntryAssetRepository
import com.dreamjournal.app.data.repository.SettingsRepository
import com.dreamjournal.app.data.repository.TxtExportRepository
import com.dreamjournal.app.data.repository.WeatherRepository
import com.dreamjournal.app.domain.ai.AiService

class DreamJournalApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(private val app: Application) {
    private val migration1To2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE dream_entries ADD COLUMN dreamTag TEXT NOT NULL DEFAULT '普通'")
        }
    }

    private val migration2To3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE dream_entries ADD COLUMN recordType TEXT NOT NULL DEFAULT 'DREAM'")
        }
    }

    private val migration3To4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE dream_entries ADD COLUMN photoPaths TEXT NOT NULL DEFAULT '[]'")
            db.execSQL("ALTER TABLE dream_entries ADD COLUMN todoItems TEXT NOT NULL DEFAULT '[]'")
        }
    }

    private val migration4To5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE dream_entries ADD COLUMN extraAudioPaths TEXT NOT NULL DEFAULT '[]'")
        }
    }

    private val migration5To6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE dream_entries ADD COLUMN contentBlocks TEXT NOT NULL DEFAULT '[]'")
            db.execSQL("ALTER TABLE dream_entries ADD COLUMN weatherText TEXT")
        }
    }

    private val migration6To7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE dream_entries ADD COLUMN locationText TEXT")
        }
    }

    private val database: DreamDatabase by lazy {
        Room.databaseBuilder(app, DreamDatabase::class.java, "dream_journal.db")
            .addMigrations(
                migration1To2,
                migration2To3,
                migration3To4,
                migration4To5,
                migration5To6,
                migration6To7
            )
            .fallbackToDestructiveMigration()
            .build()
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(app) }
    val dreamRepository: DreamRepository by lazy {
        DreamRepository(
            dreamEntryDao = database.dreamEntryDao(),
            aiMessageDao = database.aiMessageDao()
        )
    }
    val aiService: AiService by lazy { AiService(settingsRepository) }
    val audioRecorderManager: AudioRecorderManager by lazy { AudioRecorderManager(app) }
    val audioPlayerManager: AudioPlayerManager by lazy { AudioPlayerManager() }
    val txtExportRepository: TxtExportRepository by lazy { TxtExportRepository(app) }
    val entryAssetRepository by lazy { EntryAssetRepository(app) }
    val weatherRepository by lazy { WeatherRepository(app) }
}
