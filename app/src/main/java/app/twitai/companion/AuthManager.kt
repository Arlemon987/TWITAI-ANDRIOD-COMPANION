package app.twitai.companion

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.Base64

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

                /*
                 * Explicit Google Sign-In button flow.
                 *
                 * This is different from GetGoogleIdOption.
                 * Google recommends GetSignInWithGoogleOption
                 * for a dedicated "Sign in with Google" button.
                 */

                val googleOption =
                    GetSignInWithGoogleOption.Builder(
                        serverClientId = Config.GOOGLE_WEB_CLIENT_ID
                    )
                        .setNonce(generateSecureRandomNonce())
                        .build()

                /*
                 * IMPORTANT:
                 * The explicit Google button flow must contain
                 * exactly one GetSignInWithGoogleOption.
                 */

                val request =
                    GetCredentialRequest.Builder()
                        .addCredentialOption(googleOption)
                        .build()

                /*
                 * Use the Activity as the context so Android can
                 * correctly launch the Google system UI.
                 */

                val result =
                    credentialManager.getCredential(
                        context = activity,
                        request = request
                    )

                /*
                 * Convert the returned credential into a
                 * Google ID token credential.
                 */

                val googleCredential =
                    GoogleIdTokenCredential.createFrom(
                        result.credential.data
                    )

                /*
                 * Convert Google ID token into Firebase credential.
                 */

                val firebaseCredential =
                    GoogleAuthProvider.getCredential(
                        googleCredential.idToken,
                        null
                    )

                /*
                 * Sign in to Firebase.
                 */

                auth.signInWithCredential(
                    firebaseCredential
                ).awaitUnit()

                Result.success(Unit)

            } catch (e: Exception) {

                /*
                 * Return the actual error to MainActivity
                 * so it can be displayed instead of silently
                 * resetting the screen.
                 */

                Result.failure(e)
            }
        }

    fun signOut() {
        auth.signOut()
    }

    private fun generateSecureRandomNonce(
        byteLength: Int = 32
    ): String {

        val randomBytes = ByteArray(byteLength)

        SecureRandom.getInstanceStrong()
            .nextBytes(randomBytes)

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(randomBytes)
    }
}

private suspend fun
com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>.awaitUnit() {

    kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->

        addOnSuccessListener {
            continuation.resume(Unit) {}
        }

        addOnFailureListener {
            continuation.resumeWith(
                Result.failure(it)
            )
        }
    }
}
