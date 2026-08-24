
package com.tv.applelauncher

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tv.applelauncher.streaming.JellyfinApi
import com.tv.applelauncher.streaming.JellyfinSession
import com.tv.applelauncher.streaming.JfLoginRequest
import kotlinx.coroutines.*

class JellyfinSetupActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jellyfin_setup)

        val urlInput = findViewById<EditText>(R.id.jf_url)
        val userInput = findViewById<EditText>(R.id.jf_user)
        val passInput = findViewById<EditText>(R.id.jf_pass)
        val connectBtn = findViewById<Button>(R.id.jf_connect)

        connectBtn.setOnClickListener {
            val url = urlInput.text.toString().trim()
            if (url.isBlank()) return@setOnClickListener

            scope.launch {
                connectBtn.isEnabled = false
                try {
                    val api = withContext(Dispatchers.IO) { JellyfinApi.create(url) }
                    val auth = withContext(Dispatchers.IO) {
                        api.login(
                            authHeader = "MediaBrowser Client=\"${JellyfinApi.CLIENT_NAME}\", " +
                                    "Device=\"Android TV\", DeviceId=\"tv-launcher-001\", Version=\"1.0\"",
                            body = JfLoginRequest(userInput.text.toString(),
                                passInput.text.toString()))
                    }
                    JellyfinSession.serverUrl = url
                    JellyfinSession.accessToken = auth.AccessToken ?: ""
                    JellyfinSession.userId = auth.User?.Id ?: ""
                    JellyfinSession.save(this@JellyfinSetupActivity)

                    Toast.makeText(this@JellyfinSetupActivity,
                        "Connected as ${auth.User?.Name}", Toast.LENGTH_SHORT).show()
                    startActivity(android.content.Intent(
                        this@JellyfinSetupActivity, MainActivity::class.java))
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@JellyfinSetupActivity,
                        "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    connectBtn.isEnabled = true
                }
            }
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}