package com.tv.applelauncher

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.tv.applelauncher.data.ProfileManager
import com.tv.applelauncher.data.UserProfile

class ProfileSwitcherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_switcher)
        renderProfiles()
    }

    private fun renderProfiles() {
        val row = findViewById<LinearLayout>(R.id.profiles_row)
        row.removeAllViews()
        val profiles = ProfileManager.getProfiles(this)

        profiles.forEach { profile ->
            val view = LayoutInflater.from(this)
                .inflate(R.layout.item_profile, row, false) as LinearLayout
            view.findViewById<TextView>(R.id.profile_name).text = profile.name
            view.setOnClickListener { selectProfile(profile) }
            view.setOnFocusChangeListener { v, hasFocus ->
                v.animate().scaleX(if (hasFocus) 1.12f else 1f)
                    .scaleY(if (hasFocus) 1.12f else 1f).setDuration(180).start()
            }
            row.addView(view)
        }

        // "+ Add Profile" tile
        val addTile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(24, 24, 24, 24)
            addView(TextView(this@ProfileSwitcherActivity).apply {
                text = "+"; textSize = 48f; gravity = Gravity.CENTER
                setTextColor(resources.getColor(R.color.light_gray, theme))
            })
            addView(TextView(this@ProfileSwitcherActivity).apply {
                text = "Add Profile"; textSize = 16f; gravity = Gravity.CENTER
                setTextColor(resources.getColor(R.color.light_gray, theme))
            })
            isFocusable = true; isClickable = true
            setOnClickListener { showAddProfileDialog() }
        }
        row.addView(addTile)
    }

    private fun selectProfile(profile: UserProfile) {
        if (!profile.pin.isNullOrBlank()) {
            val input = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER; hint = "Enter PIN"
            }
            AlertDialog.Builder(this).setTitle("Profile locked")
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    if (input.text.toString() == profile.pin) enter(profile)
                    else Toast.makeText(this, "Wrong PIN", Toast.LENGTH_SHORT).show()
                }.setNegativeButton("Cancel", null).show()
        } else enter(profile)
    }

    private fun enter(profile: UserProfile) {
        ProfileManager.setCurrent(this, profile.id)
        startActivity(android.content.Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showAddProfileDialog() {
        val input = EditText(this).apply { hint = "Profile name" }
        AlertDialog.Builder(this).setTitle("New Profile")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                if (input.text.isNotBlank()) {
                    val colors = listOf(
                        0xFF0071E3.toInt(), 0xFFFF9F0A.toInt(), 0xFF30D158.toInt(),
                        0xFFBF5AF2.toInt(), 0xFFFF375F.toInt())
                    ProfileManager.addProfile(this, input.text.toString().trim(),
                        colors.random(), false)
                    renderProfiles()
                }
            }.setNegativeButton("Cancel", null).show()
    }
}