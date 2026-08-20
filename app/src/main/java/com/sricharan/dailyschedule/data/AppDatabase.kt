package com.sricharan.dailyschedule.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ScheduleItem::class,
        Completion::class,
        DayReflection::class,
        Thought::class,
        SkippedOccurrence::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scheduleDao(): ScheduleDao

    companion object {
        /**
         * v1 -> v2 adds the reflections table. Written as a real migration
         * rather than a destructive fallback so nobody loses a schedule they
         * already built up.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS day_reflections (
                        date TEXT NOT NULL PRIMARY KEY,
                        note TEXT NOT NULL,
                        savedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * v2 -> v3 adds free-written thoughts and the per-day skips that let a
         * repeating item be let go of for a single day without losing the
         * routine itself. Again a real migration — nothing already written down
         * should be thrown away by an update.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS thoughts (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        date TEXT NOT NULL,
                        text TEXT NOT NULL,
                        writtenAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_thoughts_date ON thoughts (date)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS skipped_occurrences (
                        scheduleItemId INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        PRIMARY KEY(scheduleItemId, date)
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daily_schedule.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
