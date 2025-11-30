package com.example.application_gestion_rdv.utils

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes

class GoogleDriveHelper(private val context: Context) {

    private var driveService: Drive? = null
    private var googleSignInClient: GoogleSignInClient

    init {
        // IMPORTANT: Utilisez le Web Client ID (OAuth 2.0 Client ID de type Web)
        // que vous trouvez dans Google Cloud Console > Credentials
        val webClientId = "714664751310-k8t1v0dvapkjbq06ukcevkr6sbto4fqb.apps.googleusercontent.com"
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .requestServerAuthCode(webClientId)
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, signInOptions)
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun handleSignInResult(account: GoogleSignInAccount?) {
        account?.let {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = it.account

            driveService = Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("Gestion RDV")
                .build()
        }
    }

    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && driveService != null
    }

    fun getDriveService(): Drive? {
        if (!isSignedIn()) {
            // Essayer de récupérer le compte existant
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null) {
                handleSignInResult(account)
            }
        }
        return driveService
    }

    fun signOut(onComplete: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener {
            driveService = null
            onComplete()
        }
    }

    companion object {
        const val REQUEST_CODE_SIGN_IN = 1001
    }
}