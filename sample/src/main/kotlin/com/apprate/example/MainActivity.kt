package com.apprate.example

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.android.synthetic.main.activity_main.*
import me.msfjarvis.apprate.AppRate

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Display dialog on launch example
        AppRate(this)
                // Not to prompt the user if the application has crashed once.
                .setShowIfAppHasCrashed(false)
                // When to prompt the user.
                .setMinDaysUntilPrompt(7)
                .setMinLaunchesUntilPrompt(20)
                .init()

        // Custom dialog example
        val customDialog = MaterialDialog(this)
                .title(R.string.custom_title)
                .message(R.string.custom_content)
                .positiveButton(R.string.custom_positive_text)
                .negativeButton(R.string.custom_negative_text)

        button_custom_dialog.setOnClickListener {
            AppRate(this).setCustomDialog(customDialog).init()
        }

        // Custom click lister example
        button_custom_click_listener.setOnClickListener {
            AppRate(this).setOnPositiveCallback {
                Toast.makeText(this, "Custom on click action here!", Toast.LENGTH_SHORT).show()
            }.init()
        }

    }
}
