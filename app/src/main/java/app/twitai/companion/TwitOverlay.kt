package app.twitai.companion

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.app.AlertDialog
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import app.twitai.companion.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: AuthManager

    private var loginInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseBootstrap.init(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = AuthManager(this)

        refreshUi()

        binding.loginButton.setOnClickListener {

            if (loginInProgress) return@setOnClickListener

            loginInProgress = true

            binding.loginButton.isEnabled = false
            binding.statusText.text = "Opening Google sign-in..."

            lifecycleScope.launch {

                val result = auth.signIn(this@MainActivity)

                loginInProgress = false
                binding.loginButton.isEnabled = true

                val error = result.exceptionOrNull()

                if (error != null) {

                    val fullError =
                        error.message
                            ?: error.toString()

                    binding.statusText.text =
                        "Google login failed"

                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Google Login Error")
                        .setMessage(fullError)
                        .setPositiveButton("OK", null)
                        .show()

                } else {

                    binding.statusText.text =
                        "Signed in as ${auth.email() ?: "Google account"}"

                    binding.loginButton.text =
                        "Sign in with another account"
                }
            }
        }

        binding.accessibilityButton.setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            )
        }
    }

    override fun onResume() {
        super.onResume()

        if (::auth.isInitialized && !loginInProgress) {
            refreshUi()
        }
    }

    private fun refreshUi() {

        if (auth.isLoggedIn()) {

            binding.statusText.text =
                "Signed in as ${auth.email() ?: "Google account"}"

            binding.loginButton.text =
                "Sign in with another account"

        } else {

            binding.statusText.text =
                "Not signed in"

            binding.loginButton.text =
                "Sign in with Google"
        }
    }
}
