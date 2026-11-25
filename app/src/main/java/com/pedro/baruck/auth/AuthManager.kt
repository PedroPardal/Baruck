package com.pedro.baruck.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Login com Google usando o Credential Manager (sem a lib legada do GoogleSignIn).
 * O [webClientId] é o "Web client ID" do OAuth, salvo em strings.xml.
 */
class AuthManager(private val context: Context, private val webClientId: String) {

    private val auth = FirebaseAuth.getInstance()
    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun signInWithGoogle(): Result<Unit> = runCatching {
        val option = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response = CredentialManager.create(context).getCredential(context, request)
        val credential = response.credential
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val firebaseCred = GoogleAuthProvider.getCredential(googleIdToken, null)
        auth.signInWithCredential(firebaseCred).await()
        Unit
    }

    fun signOut() = auth.signOut()
}
