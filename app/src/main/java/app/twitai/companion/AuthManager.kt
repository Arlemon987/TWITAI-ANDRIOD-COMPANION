package app.twitai.companion

import android.app.Activity
import android.content.Context
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom

class AuthManager(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun email(): String? {
        return auth.currentUser?.email
    }

    suspend fun signIn(activity: Activity): Result<Unit> =
        withContext(Dispatchers.Main) {

            try {

                val credentialManager =
                    CredentialManager.create(activity)

                val nonce = generateNonce()

                val googleOption =
                    GetSignInWithGoogleOption.Builder(
                        Config.GOOGLE_WEB_CLIENT_ID
                    )
                        .setNonce(nonce)
                        .build()

                val request =
                    GetCredentialRequest.Builder()
                        .addCredentialOption(googleOption)
                        .build()

                val result =
                    credentialManager.getCredential(
                        context = activity,
                        request = request
                    )

                val googleCredential =
                    GoogleIdTokenCredential.createFrom(
                        result.credential.data
                    )

                val firebaseCredential =
                    GoogleAuthProvider.getCredential(
                        googleCredential.idToken,
                        null
                    )

                auth.signInWithCredential(firebaseCredential)
                    .awaitUnit()

                Result.success(Unit)

            } catch (e: Exception) {

                android.util.Log.e(
                    "TwitAI_AUTH",
                    "Google login failed",
                    e
                )

                Result.failure(
                    Exception(
                        "${e::class.java.name}\n${e.message ?: "No error message"}",
                        e
                    )
                )
            }
        }

    fun signOut() {
        auth.signOut()
    }

    private fun generateNonce(): String {

        val random = ByteArray(32)

        SecureRandom().nextBytes(random)

        return Base64.encodeToString(
            random,
            Base64.URL_SAFE or
                    Base64.NO_WRAP or
                    Base64.NO_PADDING
        )
    }
}

private suspend fun
        com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>
        .awaitUnit() {

    kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->

        addOnSuccessListener {
            cont.resume(Unit) {}
        }

        addOnFailureListener {
            cont.resumeWith(
                Result.failure(it)
            )
        }
    }
}
