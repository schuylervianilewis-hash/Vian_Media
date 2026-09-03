package com.example

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestampMs: Long,
    val isError: Boolean,
    val tag: String,
    val message: String,
    val stackTrace: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestampMs))
    
    val formattedString: String
        get() {
            var formatted = "[$formattedTime] "
            if (isError) formatted += "ERROR [$tag]: " else formatted += "[$tag] "
            formatted += message
            if (stackTrace != null) {
                formatted += "\n$stackTrace"
            }
            return formatted
        }
}

object LogKeeper {
    private const val TAG = "LogKeeper"
    private const val PREFS_NAME = "log_keeper_prefs"
    private const val KEY_LOGGER_ENABLED = "logger_enabled"
    private lateinit var prefs: SharedPreferences
    private lateinit var appContext: Context

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()
    
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isEnabled.value = prefs.getBoolean(KEY_LOGGER_ENABLED, true)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logError("CRASH", "Uncaught exception in thread ${thread.name}", throwable)
            dumpCrash(appContext, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        
        log("LogKeeper initialized", "System")
    }

    fun toggleLogger() {
        val newState = !_isEnabled.value
        _isEnabled.value = newState
        if (::prefs.isInitialized) {
            prefs.edit().putBoolean(KEY_LOGGER_ENABLED, newState).apply()
        }
        log("Logger state changed to: $newState", "System")
    }

    fun log(message: String, tag: String = "App") {
        if (!_isEnabled.value) return
        val entry = LogEntry(System.currentTimeMillis(), false, tag, message)
        Log.d(TAG, entry.formattedString)
        val currentList = _logs.value
        val newList = if (currentList.size > 500) currentList.drop(1) + entry else currentList + entry
        _logs.value = newList
    }

    fun logWarn(tag: String, message: String) {
        if (!_isEnabled.value) return
        val entry = LogEntry(System.currentTimeMillis(), false, "WARN/$tag", message)
        Log.w(TAG, entry.formattedString)
        val currentList = _logs.value
        val newList = if (currentList.size > 500) currentList.drop(1) + entry else currentList + entry
        _logs.value = newList
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        if (!_isEnabled.value) return
        val stackTrace = throwable?.let { Log.getStackTraceString(it) }
        val entry = LogEntry(System.currentTimeMillis(), true, tag, message, stackTrace)
        Log.e(TAG, entry.formattedString)
        val currentList = _logs.value
        val newList = if (currentList.size > 500) currentList.drop(1) + entry else currentList + entry
        _logs.value = newList
    }

    private fun writeToDownloads(context: Context, fileName: String, content: String): Boolean {
        var success = false
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { stream ->
                        stream.write(content.toByteArray(Charsets.UTF_8))
                        stream.flush()
                    }
                    success = true
                    Log.d(TAG, "Successfully written to MediaStore.Downloads: $fileName")
                }
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = java.io.File(downloadsDir, fileName)
                file.writeText(content)
                success = true
                Log.d(TAG, "Successfully written to external storage Downloads: ${file.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing to primary Downloads: ${e.message}", e)
        }

        // Failsafe backup to app-specific external files dir (never requires storage permission)
        try {
            val appDownloads = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (appDownloads != null) {
                if (!appDownloads.exists()) appDownloads.mkdirs()
                val backupFile = java.io.File(appDownloads, fileName)
                backupFile.writeText(content)
                Log.d(TAG, "Backup written to app external downloads: ${backupFile.absolutePath}")
                success = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing to backup app downloads: ${e.message}", e)
        }

        return success
    }

    private fun dumpCrash(context: Context, throwable: Throwable) {
        try {
            val dateStr = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())
            val fileName = "Vianbrplay_crash_$dateStr.txt"
            val recentLogs = _logs.value.joinToString("\n") { it.formattedString }
            val crashData = """
                ==============================
                Vianbrplay Crash Dump - $dateStr
                Message: ${throwable.message}
                ==============================
                Stacktrace:
                ${Log.getStackTraceString(throwable)}
                ==============================
                Recent Logs (${_logs.value.size} entries):
                $recentLogs
                ==============================
            """.trimIndent()
            
            val written = writeToDownloads(context, fileName, crashData)
            Log.d(TAG, "Crash dumped: $written ($fileName)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash dump", e)
        }
    }

    fun dumpCurrentLogs(context: Context) {
        if (!_isEnabled.value) return
        try {
            val dateStr = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())
            val fileName = "Vianbrplay_logs_$dateStr.txt"
            val logsData = """
                ==============================
                Vianbrplay Log Export - $dateStr
                Total Entries: ${_logs.value.size}
                ==============================
                ${_logs.value.joinToString("\n") { it.formattedString }}
            """.trimIndent()
            
            val written = writeToDownloads(context, fileName, logsData)
            log("Logs dumped to Downloads: $written ($fileName)", "System")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write logs dump", e)
        }
    }
}
