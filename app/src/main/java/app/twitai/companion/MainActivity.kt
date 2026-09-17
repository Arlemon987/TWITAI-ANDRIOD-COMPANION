package app.twitai.companion

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import app.twitai.companion.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseBootstrap.init(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = AuthManager(this)
        refreshUi()

        binding.loginButton.setOnClickListener {
            lifecycleScope.launch {
                binding.loginButton.isEnabled = false
                val result = auth.signIn(this@MainActivity)
                binding.loginButton.isEnabled = true
                result.exceptionOrNull()?.let {
                    binding.statusText.text = "Login failed: ${it.message}"
                }
                refreshUi()
            }
        }

        binding.accessibilityButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        if (::auth.isInitialized) refreshUi()
    }

    private fun refreshUi() {
        if (auth.isLoggedIn()) {
            binding.statusText.text = "Signed in as ${auth.email() ?: "Google account"}"
            binding.loginButton.text = "Sign in with another account"
        } else {
            binding.statusText.text = "Not signed in"
            binding.loginButton.text = "Sign in with Google"
        }
    }
}
