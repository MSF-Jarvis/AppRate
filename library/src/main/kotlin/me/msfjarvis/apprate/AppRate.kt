@file:Suppress("unused")
package me.msfjarvis.apprate

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.text.format.DateUtils
import android.util.Log
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onCancel

class AppRate(private val hostActivity: Activity) {
    private val preferences: SharedPreferences =
            hostActivity.getSharedPreferences(PrefsContract.SHARED_PREFS_NAME, 0)

    private var minLaunchesUntilPrompt: Long = 0
    private var minDaysUntilPrompt: Long = 0
    private var customDialog: MaterialDialog? = null
    private var positiveActionCallback: () -> Unit = { throw NoCustomCallbackException() }
    private var negativeActionCallback: () -> Unit? = { throw NoCustomCallbackException() }

    private var showIfHasCrashed = true

    /**
     * @param minLaunchesUntilPrompt The minimum number of times the
     * *                               user lunches the application before showing the rate dialog.<br></br>
     * *                               Default value is 0 times.
     * *
     * @return This [AppRate] object to allow chaining.
     */
    fun setMinLaunchesUntilPrompt(minLaunchesUntilPrompt: Long): AppRate {
        this.minLaunchesUntilPrompt = minLaunchesUntilPrompt
        return this
    }

    /**
     * @param minDaysUntilPrompt The minimum number of days before showing the rate dialog.<br></br>
     * *            Default value is 0 days.
     * *
     * @return This [AppRate] object to allow chaining.
     */
    fun setMinDaysUntilPrompt(minDaysUntilPrompt: Long): AppRate {
        this.minDaysUntilPrompt = minDaysUntilPrompt
        return this
    }

    /**
     * @param showIfCrash If `false` the rate dialog will
     * *                    not be shown if the application has crashed once.<br></br>
     * *                    Default value is `false`.
     * *
     * @return This [AppRate] object to allow chaining.
     */
    fun setShowIfAppHasCrashed(showIfCrash: Boolean): AppRate {
        showIfHasCrashed = showIfCrash
        preferences.edit().putBoolean(PrefsContract.PREF_DONT_SHOW_IF_CRASHED, showIfCrash).apply()
        return this
    }

    /**
     * Use this method if you want to customize the style and content of the rate dialog.<br></br>
     * When using the [MaterialDialog] you should use:
     *
     *  * [MaterialDialog.positiveButton] for the **rate** button.
     *  * [MaterialDialog.negativeButton] for the **never rate** button.
     *
     * @param customDialog The custom dialog you want to use as the rate dialog.
     * *
     * @return This [AppRate] object to allow chaining.
     */
    fun setCustomDialog(customDialog: MaterialDialog): AppRate {
        this.customDialog = customDialog
        return this
    }

    /**
     * Display the rate dialog if needed.
     */
    fun init() {

        Log.d(TAG, "Init AppRate")

        if (preferences.getBoolean(PrefsContract.PREF_DONT_SHOW_AGAIN, false) ||
                preferences.getBoolean(PrefsContract.PREF_APP_HAS_CRASHED, false) &&
                !showIfHasCrashed) {
            return
        }

        if (!showIfHasCrashed) {
            initExceptionHandler()
        }

        val editor = preferences.edit()

        // Get and increment launch counter.
        val launchCount = preferences.getLong(PrefsContract.PREF_LAUNCH_COUNT, 0) + 1
        editor.putLong(PrefsContract.PREF_LAUNCH_COUNT, launchCount)

        // Get date of first launch.
        var dateFirstLaunch: Long? = preferences.getLong(PrefsContract.PREF_DATE_FIRST_LAUNCH, 0)
        if (dateFirstLaunch == 0L) {
            dateFirstLaunch = System.currentTimeMillis()
            editor.putLong(PrefsContract.PREF_DATE_FIRST_LAUNCH, dateFirstLaunch)
        }

        // Show the rate dialog if needed.
        if (launchCount >= minLaunchesUntilPrompt) {
            if (System.currentTimeMillis() >= dateFirstLaunch!! + minDaysUntilPrompt * DateUtils.DAY_IN_MILLIS) {
                customDialog?.show() ?: showDefaultDialog()
            }
        }

        editor.apply()
    }

    /**
     * Initialize the [ExceptionHandler].
     */
    private fun initExceptionHandler() {

        Log.d(TAG, "Init AppRate ExceptionHandler")

        val currentHandler = Thread.getDefaultUncaughtExceptionHandler()

        // Don't register again if already registered.
        if (currentHandler !is ExceptionHandler) {

            // Register default exceptions handler.
            Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler(currentHandler, hostActivity))
        }
    }

    /**
     * Shows the default rate dialog.
     */
    private fun showDefaultDialog() {

        Log.d(TAG, "Create default dialog.")

        val title = String.format("Rate %s", getApplicationName(hostActivity.applicationContext))
        val message = String.format("If you enjoy using %s, please take a moment to rate it. Thanks for your support!",
                getApplicationName(hostActivity.applicationContext))
        val rate = "Rate it !"
        val dismiss = "No thanks"

        MaterialDialog(hostActivity)
            .title(text = title)
            .message(text = message)
            .positiveButton(text = rate)
            .negativeButton(text = dismiss)
            .positiveButton { onPositive() }
            .negativeButton { onNegative() }
            .onCancel { onCancel() }
            .show()
    }

    /**
     * Method called when the positiveButton callback
     * is invoked.
     */
    private fun onPositive() {
        try {
            positiveActionCallback()
        } catch (ignored: NoCustomCallbackException) {
            try {
                hostActivity.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + hostActivity.packageName)
                    )
                )
            } catch (ignored: ActivityNotFoundException) {
                Toast.makeText(hostActivity, "No Play Store installed on device", Toast.LENGTH_SHORT).show()
            }
        }

        preferences.edit().putBoolean(PrefsContract.PREF_DONT_SHOW_AGAIN, true).apply()
    }

    /**
     * Method called when the negativeButton callback
     * is invoked.
     */
    private fun onNegative() {
        try {
            negativeActionCallback()
        } catch (ignored: NoCustomCallbackException) {
            preferences.edit().putBoolean(PrefsContract.PREF_DONT_SHOW_AGAIN, true).apply()
        }
    }

    private fun onCancel() {
        preferences.edit()
            .putLong(PrefsContract.PREF_DATE_FIRST_LAUNCH, System.currentTimeMillis())
            .putLong(PrefsContract.PREF_LAUNCH_COUNT, 0)
            .apply()
    }

    /**
     * @param negativeActionCallback  A Kotlin unit invoked when the negative action
     * callback is triggered in the dialog.
     *
     * @return This [AppRate] object to allow chaining.
     */
    public fun setOnNegativeCallback(negativeActionCallback: () -> Unit): AppRate {
        this.negativeActionCallback = negativeActionCallback
        return this
    }

    /**
     * @param positiveActionCallback A Kotlin unit invoked when the positive action
     * callback is triggered in the dialog.
     *
     * @return This [AppRate] object to allow chaining.
     */
    public fun setOnPositiveCallback(positiveActionCallback: () -> Unit): AppRate {
        this.positiveActionCallback = positiveActionCallback
        return this
    }

    class NoCustomCallbackException: Exception()

    companion object {

        private const val TAG = "AppRate"

        /**
         * Reset all the data collected about number of launches and days until first launch.
         * @param context A context.
         */
        fun reset(context: Context) {
            context.getSharedPreferences(PrefsContract.SHARED_PREFS_NAME, 0).edit().clear().apply()
            Log.d(TAG, "Cleared AppRate shared preferences.")
        }

        /**
         * @param context A context of the current application.
         * *
         * @return The application name of the current application.
         */
        private fun getApplicationName(context: Context): String {
            val packageManager = context.packageManager
            val applicationInfo: ApplicationInfo?
            applicationInfo = try {
                packageManager.getApplicationInfo(context.packageName, 0)
            } catch (ignored: NameNotFoundException) {
                null
            }

            return (if (applicationInfo != null)
                packageManager.getApplicationLabel(applicationInfo) else "(unknown)") as String
        }
    }
}
