package com.example.legacygis


import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.legacygis.ui.theme.LegacyGISTheme
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task


class MainActivity : ComponentActivity() {

    companion object {
        const val TAG = "MainActivityTag"
    }

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var facebookCallbackManager: CallbackManager

    private val resultLauncherGoogle = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.handleLaunchGoogle()
    }

    private fun ActivityResult.handleLaunchGoogle() {
        when (resultCode) {
            RESULT_OK -> {
                // The Task returned from this call is always completed,
                // no need to attach a listener.
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                handleSignInResultGoogle(task)
            }
            RESULT_CANCELED -> {
                displayToast("User dismissed the dialog")
            }
        }
    }

    private fun handleSignInResultGoogle(completedTask: Task<GoogleSignInAccount>) {
        try {
            // This account variable is the same that would be returned by getAccount()
            val account = completedTask.result
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        googleSignInClient = GoogleSignIn.getClient(
            this,
            getGoogleSignInOptions()
        )

        facebookCallbackManager = CallbackManager.Factory.create()
        facebookCallbackManager.registerCallbacks()

        setContent {
            LegacyGISTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {

                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        ButtonRowWithHeader(
                            headerText = "Google",
                            signIn = { googleSignIn() },
                            signOut = { googleSignOut() },
                            displayInfo = { displayGoogleInfo() }
                        )

                        Spacer(modifier = Modifier.padding(16.dp))

                        ButtonRowWithHeader(
                            headerText = "Facebook",
                            signIn = { facebookSignIn() },
                            signOut = { facebookSignOut() },
                            displayInfo = { displayFacebookInfo() }
                        )

                        // TODO: Apple
                        /*Spacer(modifier = Modifier.padding(16.dp))

                        ButtonRowWithHeader(
                            headerText = "Apple",
                            signIn = { appleSignIn() },
                            signOut = { appleSignOut() },
                            displayInfo = { displayAppleInfo() }
                        )*/
                    }
                }
            }
        }
    }

    // Region: Google
    private fun getGoogleSignInOptions() =
        GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_auth_web_client_id))
            .requestEmail()
            .build()

    private fun googleSignIn() {

        // Could check first if already signed in,
        // that way the there's no weird animation thing that goes over the screen
        if (getGoogleAccount() == null) {

            // don't follow deprecated docs, use resultLauncher
            resultLauncherGoogle.launch(googleSignInClient.signInIntent)
        }
    }

    private fun googleSignOut() {
        googleSignInClient.signOut()
    }

    private fun getGoogleAccount() = GoogleSignIn.getLastSignedInAccount(this)

    private fun displayGoogleInfo() {
        val account = getGoogleAccount()
        if (account != null) {
            displayToast("Signed in as: ${account.displayName}")
        } else {
            displayToast("Not logged in")
        }
    }
    // end region

    // Region: Facebook
    private fun CallbackManager.registerCallbacks() {
        LoginManager.getInstance().registerCallback(this, object : FacebookCallback<LoginResult> {
            override fun onCancel() {
                displayToast("Facebook onCancel()")
            }

            override fun onError(error: FacebookException) {
                displayToast("There was an error: ${error.message}")
            }

            override fun onSuccess(result: LoginResult) {
                // This has account info
                val accessToken = result.accessToken
            }
        })
    }


    private fun facebookSignIn() {
        // onActivityResult() is deprecated - this is the workaround
        // https://stackoverflow.com/questions/67297326/how-to-use-facebook-sign-in-callbackmanager-with-onactivityresult-deprecated

        LoginManager.getInstance()
            .logInWithReadPermissions(
                this,
                facebookCallbackManager,        // this line added - not part of docs
                listOf("public_profile"),
            )
    }

    private fun facebookSignOut() {
        LoginManager.getInstance().logOut()
    }

    private fun getFacebookAccount() = AccessToken.getCurrentAccessToken()

    private fun displayFacebookInfo() {
        val accessToken = getFacebookAccount()
        if (accessToken != null && !accessToken.isExpired) {
            displayToast(accessToken.userId)
        } else {
            displayToast("Not logged in")
        }
    }
    // end region

    private fun displayToast(message: String?) {
        Toast.makeText(
                this@MainActivity,
                "$message",
                Toast.LENGTH_SHORT
            ).show()
    }
}

@Composable
private fun ButtonRowWithHeader(
    headerText: String,
    signIn: () -> Unit,
    signOut: () -> Unit,
    displayInfo: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = headerText)
    }

    Row(
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = signIn
        ) {
            Text(text = "Sign In")
        }
        Spacer(modifier = Modifier.padding(8.dp))
        Button(
            onClick = signOut
        ) {
            Text(text = "Sign Out")
        }
        Spacer(modifier = Modifier.padding(8.dp))
        Button(
            onClick = displayInfo
        ) {
            Text(text = "Account info")
        }
    }
}


/** Remnants of trying Google's new Sign-In API - Identity  */

// val signInClient = Identity.getSignInClient(this)

/*try {
    val credential = Identity
        .getSignInClient(this@MainActivity)
        .getSignInCredentialFromIntent(data)
    cred.value = credential
    val name = credential.displayName
} catch (e: ApiException) {
    Log.e(TAG, "Failure after trying to get credential: $e")
}*/


/*private fun getGoogleSignInRequest() =
    GetSignInIntentRequest.builder()
        .setServerClientId(getString(R.string.google_auth_web_client_id))
        .build()*/

/*private fun SignInClient.googleSignIn() {
    this
        .getSignInIntent(getGoogleSignInRequest())
        .addOnSuccessListener { result ->
            try {
                val intentSenderRequest = IntentSenderRequest.Builder(result.intentSender).build()
                resultLauncher.launch(intentSenderRequest)

            } catch (e: IntentSender.SendIntentException) {
                Log.e(TAG, "Failed: $e")
            }
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "Failed: $e")
        }
}

private fun SignInClient.googleSignOut() {
    // val account = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
    val account = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
    Toast
        .makeText(
            this@MainActivity,
            "Name if signed in: ${account?.displayName}",
            Toast.LENGTH_SHORT
        )
        .show()
    signOut()
}*/


/*override fun onStart() {
    super.onStart()
    displayToast("Already signed in if name != null -> ${getGoogleAccount()?.displayName}")
}*/

// LoginManager ->
//  listOf("public_profile", "email", "user_friends"),