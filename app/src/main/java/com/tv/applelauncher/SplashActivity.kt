package com.tv.applelauncher

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tv.applelauncher.streaming.JellyfinSession

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.splash_logo)
        val text = findViewById<TextView>(R.id.splash_text)

        logo.scaleX = 0f; logo.scaleY = 0f; logo.alpha = 0f
        text.alpha = 0f

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(logo, "scaleX", 0f, 1f).apply {
                    interpolator = OvershootInterpolator(); duration = 700
                },
                ObjectAnimator.ofFloat(logo, "scaleY", 0f, 1f).apply {
                    interpolator = OvershootInterpolator(); duration = 700
                },
                ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f).apply { duration = 400 },
                ObjectAnimator.ofFloat(text, "alpha", 0f, 1f).apply {
                    startDelay = 500; duration = 600
                }
            )
            start()
        }

        // Decide where to go next
        logo.postDelayed({
            JellyfinSession.load(this)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2200)
    }
}