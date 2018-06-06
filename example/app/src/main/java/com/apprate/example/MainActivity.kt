package com.apprate.example

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.afollestad.materialdialogs.MaterialDialog
import me.msfjarvis.apprate.AppRate
import android.content.DialogInterface
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Display dialog on launch example
        AppRate(this)
            // Not to prompt the user if the application has crashed once.
            // .setShowIfAppHasCrashed(false)
            // When to prompt the user.
            // .setMinDaysUntilPrompt(7)
            // .setMinLaunchesUntilPrompt(20)
            .init()

        // Custom dialog example
        val customDialogBuilder = MaterialDialog.Builder(this)
            .title(R.string.custom_title)
            .content(R.string.custom_content)
            .positiveText(R.string.custom_positive_text)
            .neutralText(R.string.custom_neutral_text)
            .negativeText(R.string.custom_negative_text)

        findViewById<Button>(R.id.button_custom_dialog).setOnClickListener {
            AppRate(this).setCustomDialog(customDialogBuilder).init()
        }

        // Custom click lister example
        findViewById<Button>(R.id.button_custom_click_listener).setOnClickListener {
            AppRate(this).setOnClickListener(DialogInterface.OnClickListener { _, which ->
                Toast.makeText(this, "Custom on click action here!", Toast.LENGTH_SHORT).show()
            }).init()
        }

    }
}
