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
import kotlinx.coroutines.withContext

class AuthManager(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()

    fun isLoggedIn(): Boolean = auth.currentUser != null
    fun email(): String? = auth.currentUser?.email

    suspend fun signIn(activity: Activity): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val credentialManager = CredentialManager.create(context)

            val googleOption = GetGoogleIdOption.Builder()
                .setServerClientId(Config.GOOGLE_WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()

            val result = credentialManager.getCredential(activity, request)
            val googleCredential = GoogleIdTokenCredential
                .createFrom(result.credential.data)

            val firebaseCredential =
                GoogleAuthProvider.getCredential(googleCredential.idToken, null)

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
    kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
        addOnSuccessListener { cont.resume(Unit) {} }
        addOnFailureListener { cont.resumeWith(Result.failure(it)) }
    }
}
