package com.sricharan.dailyschedule.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.sricharan.dailyschedule.data.AppDatabase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles fully-offline backup/restore.
 *
 * Flow:
 *  1. createBackupFile()  -> writes a JSON snapshot of the whole DB to app cache
 *  2. shareBackupFile()   -> hands that file to WhatsApp / Drive / anything via a
 *                            normal Android share sheet (uses FileProvider, no
 *                            network call from this app itself)
 *  3. restoreFromUri()    -> user picks a .json file from their file manager /
 *                            wherever the backup was saved, we parse it and
 *                            replace the local DB contents
 */
class BackupManager(private val context: Context) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun createBackupFile(): File {
        val db = AppDatabase.getInstance(context)
        val items = db.scheduleDao().getAllItemsOnce()
        val completions = db.scheduleDao().getAllCompletionsOnce()
        val reflections = db.scheduleDao().getAllReflectionsOnce()
        val thoughts = db.scheduleDao().getAllThoughtsOnce()
        val skips = db.scheduleDao().getAllSkipsOnce()

        val backup = BackupData(
            items = items,
            completions = completions,
            reflections = reflections,
            thoughts = thoughts,
            skips = skips
        )
        val jsonString = json.encodeToString(backup)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "daily_schedule_backup_$timestamp.json"

        val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val file = File(backupDir, fileName)
        file.writeText(jsonString)
        return file
    }

    /** Builds a share Intent so the backup file can go straight to WhatsApp, Drive, email, etc. */
    fun buildShareIntent(file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** Reads a picked backup file (from ACTION_OPEN_DOCUMENT) and replaces local DB contents. */
    suspend fun restoreFromUri(uri: Uri): Result<Int> {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: return Result.failure(IllegalStateException("Could not read file"))

            val backup = json.decodeFromString<BackupData>(jsonString)

            val db = AppDatabase.getInstance(context)
            db.scheduleDao().clearAllCompletions()
            db.scheduleDao().clearAllReflections()
            db.scheduleDao().clearAllThoughts()
            db.scheduleDao().clearAllSkips()
            db.scheduleDao().clearAllItems()
            db.scheduleDao().insertAllItems(backup.items)
            db.scheduleDao().insertAllCompletions(backup.completions)
            db.scheduleDao().insertAllReflections(backup.reflections)
            db.scheduleDao().insertAllThoughts(backup.thoughts)
            db.scheduleDao().insertAllSkips(backup.skips)

            Result.success(backup.items.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
