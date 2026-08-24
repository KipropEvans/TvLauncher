package com.tv.applelauncher

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.tv.applelauncher.data.ParentalControls
import com.tv.applelauncher.data.ProfileManager
import com.tv.applelauncher.streaming.JellyfinSession

class SettingsActivity : AppCompatActivity() {

    private data class Row(val label: String, val value: () -> String, val onClick: () -> Unit)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        buildRows()
    }

    private fun buildRows() {
        val container = findViewById<LinearLayout>(R.id.settings_rows)
        container.removeAllViews()

        val rows = listOf(
            Row("Switch Profile", { ProfileManager.getCurrent(this).name }) {
                startActivity(android.content.Intent(this, ProfileSwitcherActivity::class.java))
            },
            Row("Parental Controls — Max Rating", {
                listOf("G", "PG", "PG-13", "TV-MA", "Unrestricted")
                    .getOrElse(ParentalControls.getMaxRating(this)) { "Unrestricted" }
            }) { showRatingPicker() },
            Row("Set Parental PIN", {
                if (ParentalControls.hasPin(this)) "PIN set ✓" else "Not set"
            }) { showPinDialog() },
            Row("Jellyfin Server", {
                if (JellyfinSession.isConnected()) JellyfinSession.serverUrl else "Not connected"
            }) {
                startActivity(android.content.Intent(this, JellyfinSetupActivity::class.java))
            },
            Row("Screensaver", { "Apple TV Aerials" }) {
                Toast.makeText(this,
                    "Enable in: Settings → Device Preferences → Screen saver",
                    Toast.LENGTH_LONG).show()
            },
            Row("About", { "Apple TV Launcher v1.0" }) { }
        )

        rows.forEach { row ->
            val view = LayoutInflater.from(this)
                .inflate(R.layout.item_settings_row, container, false) as LinearLayout
            view.findViewById<TextView>(R.id.row_label).text =
                "row.label—{row.label}   —row.label—{row.value()}"
            view.setOnClickListener { row.onClick() }
            view.setOnFocusChangeListener { v, hasFocus ->
                v.animate().scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f).setDuration(150).start()
            }
            container.addView(view)
        }
    }

    private fun showRatingPicker() {
        val options = arrayOf("G", "PG", "PG-13 / TV-14", "TV-MA", "Unrestricted")
        AlertDialog.Builder(this)
            .setTitle("Maximum allowed rating")
            .setItems(options) { _, which ->
                ParentalControls.setMaxRating(this, which)
                buildRows()
            }.show()
    }

    private fun showPinDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "4-digit PIN"
        }
        AlertDialog.Builder(this)
            .setTitle(if (ParentalControls.hasPin(this)) "Change PIN" else "Create PIN")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                if (input.text.length == 4) {
                    ParentalControls.setPin(this, input.text.toString())
                    buildRows()
                } else Toast.makeText(this, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null).show()
    }
}