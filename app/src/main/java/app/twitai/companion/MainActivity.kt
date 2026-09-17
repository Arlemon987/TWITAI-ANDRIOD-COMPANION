package app.twitai.companion

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
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

            binding.loginButton.isEnabled = false
            binding.statusText.text = "Opening Google sign-in..."

            lifecycleScope.launch {

                val result = auth.signIn(this@MainActivity)

                binding.loginButton.isEnabled = true

                val error = result.exceptionOrNull()

                if (error != null) {

                    val message =
                        error.message ?: error.javaClass.name

                    binding.statusText.text =
                        "LOGIN ERROR\n\n$message"

                    Toast.makeText(
                        this@MainActivity,
                        "Google login failed",
                        Toast.LENGTH_LONG
                    ).show()

                } else {

                    binding.statusText.text =
                        "Signed in as ${auth.email() ?: "Google account"}"

                    Toast.makeText(
                        this@MainActivity,
                        "Google login successful",
                        Toast.LENGTH_SHORT
                    ).show()
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

        if (::auth.isInitialized) {
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
