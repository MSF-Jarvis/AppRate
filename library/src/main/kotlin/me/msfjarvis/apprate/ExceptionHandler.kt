package me.msfjarvis.apprate

import java.lang.Thread.UncaughtExceptionHandler

import android.content.Context
import android.content.SharedPreferences

class ExceptionHandler
internal constructor(private val defaultExceptionHandler: UncaughtExceptionHandler, context: Context) : UncaughtExceptionHandler {
    private val preferences: SharedPreferences =
            context.getSharedPreferences(PrefsContract.SHARED_PREFS_NAME, 0)

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        preferences.edit().putBoolean(PrefsContract.PREF_APP_HAS_CRASHED, true).apply()

        // Call the original handler.
        defaultExceptionHandler.uncaughtException(thread, throwable)
    }
}