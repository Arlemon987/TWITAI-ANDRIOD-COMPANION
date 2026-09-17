package app.twitai.companion

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
                val credentialManager = CredentialManager.create(context)

                val webClientId =
                    context.getString(R.string.default_web_client_id)

                val googleOption = GetGoogleIdOption.Builder()
                    .setServerClientId(webClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleOption)
                    .build()

                val result = credentialManager.getCredential(
                    activity,
                    request
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

                auth.signInWithCredential(firebaseCredential).awaitUnit()

                Result.success(Unit)

            } catch (e: Exception) {

                Result.failure(e)
            }
        }

    fun signOut() {
        auth.signOut()
    }
}

private suspend fun com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>.awaitUnit() {
    suspendCancellableCoroutine<Unit> { continuation ->

        addOnSuccessListener {
            if (continuation.isActive) {
                continuation.resume(Unit)
            }
        }

        addOnFailureListener { exception ->

            if (continuation.isActive) {
                continuation.resumeWithException(exception)
            }
        }

        addOnCanceledListener {

            if (continuation.isActive) {
                continuation.cancel()
            }
        }
    }
}
