package me.msfjarvis.apprate

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.DialogInterface.OnCancelListener
import android.content.DialogInterface.OnClickListener
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.text.format.DateUtils
import android.util.Log
import android.widget.Toast
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog

class AppRate(private val hostActivity: Activity) : DialogInterface.OnClickListener, OnCancelListener {
    private var clickListener: OnClickListener? = null
    private val preferences: SharedPreferences =
            hostActivity.getSharedPreferences(PrefsContract.SHARED_PREFS_NAME, 0)
    private var dialogBuilder: MaterialDialog.Builder? = null

    private var minLaunchesUntilPrompt: Long = 0
    private var minDaysUntilPrompt: Long = 0

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
        return this
    }

    /**
     * Use this method if you want to customize the style and content of the rate dialog.<br></br>
     * When using the [AlertDialog.Builder] you should use:
     *
     *  * [AlertDialog.Builder.setPositiveButton] for the **rate** button.
     *  * [AlertDialog.Builder.setNeutralButton] for the **rate later** button.
     *  * [AlertDialog.Builder.setNegativeButton] for the **never rate** button.
     *
     * @param customBuilder The custom dialog you want to use as the rate dialog.
     * *
     * @return This [AppRate] object to allow chaining.
     */
    fun setCustomDialog(customBuilder: MaterialDialog.Builder): AppRate {
        dialogBuilder = customBuilder
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

                if (dialogBuilder != null) {
                    showDialog(dialogBuilder!!)
                } else {
                    showDefaultDialog()
                }
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
        val remindLater = "Remind me later"
        val dismiss = "No thanks"

        MaterialDialog.Builder(hostActivity)
                .title(title)
                .content(message)
                .positiveText(rate)
                .neutralText(remindLater)
                .negativeText(dismiss)
                .show()
    }

    /**
     * Show the custom rate dialog.
     */
    private fun showDialog(builder: MaterialDialog.Builder) {

        Log.d(TAG, "Create custom dialog.")

        val dialog = builder.build()
        dialog.show()

        val rate = dialog.getActionButton(DialogAction.POSITIVE)
        val remindLater = dialog.getActionButton(DialogAction.NEUTRAL)
        val dismiss = dialog.getActionButton(DialogAction.NEGATIVE)


        dialog.setActionButton(DialogAction.POSITIVE, rate.toString())
        dialog.setActionButton(DialogAction.NEUTRAL, remindLater.toString())
        dialog.setActionButton(DialogAction.NEGATIVE, dismiss.toString())

        dialog.setOnCancelListener(this)
    }

    override fun onCancel(dialog: DialogInterface) {

        val editor = preferences.edit()
        editor.putLong(PrefsContract.PREF_DATE_FIRST_LAUNCH, System.currentTimeMillis())
        editor.putLong(PrefsContract.PREF_LAUNCH_COUNT, 0)
        editor.apply()
    }

    /**
     * @param onClickListener A listener to be called back on.
     * *
     * @return This [AppRate] object to allow chaining.
     */
    fun setOnClickListener(onClickListener: OnClickListener): AppRate {
        clickListener = onClickListener
        return this
    }

    override fun onClick(dialog: DialogInterface, which: Int) {

        val editor = preferences.edit()

        when (which) {
            DialogInterface.BUTTON_POSITIVE -> {
                try {
                    hostActivity.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=" + hostActivity.packageName)))
                } catch (ignored: ActivityNotFoundException) {
                    Toast.makeText(hostActivity, "No Play Store installed on device", Toast.LENGTH_SHORT).show()
                }

                editor.putBoolean(PrefsContract.PREF_DONT_SHOW_AGAIN, true)
            }

            DialogInterface.BUTTON_NEGATIVE -> editor.putBoolean(PrefsContract.PREF_DONT_SHOW_AGAIN, true)

            DialogInterface.BUTTON_NEUTRAL -> {
                editor.putLong(PrefsContract.PREF_DATE_FIRST_LAUNCH, System.currentTimeMillis())
                editor.putLong(PrefsContract.PREF_LAUNCH_COUNT, 0)
            }

            else -> {
            }
        }

        editor.apply()
        dialog.dismiss()

        if (clickListener != null) {
            clickListener!!.onClick(dialog, which)
        }
    }

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
