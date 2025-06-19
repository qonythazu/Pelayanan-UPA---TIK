package com.dicoding.pelayananupa_tik.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private data class LoginScenarioContext(
        var userIsAtLoginPage: Boolean = false,
        var selectedAccount: String? = null,
        var isITKAccount: Boolean = false,
        var loginResult: LoginResult = LoginResult.PENDING
    )

    private enum class LoginResult {
        PENDING, SUCCESS, FAILED_INVALID_ACCOUNT, FAILED_ERROR
    }

    private val scenarioContext = LoginScenarioContext()

    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var auth: FirebaseAuth
    private lateinit var signInLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        givenUserIsAtLoginPage()

        setupAuthComponents()
        setupLoginButton()
    }

    // ==================== BDD METHODS ====================

    /**
     * GIVEN: User berada di halaman login
     */
    private fun givenUserIsAtLoginPage() {
        scenarioContext.userIsAtLoginPage = true
        scenarioContext.loginResult = LoginResult.PENDING
        Log.d(TAG, "BDD - GIVEN: User is at login page")
    }

    /**
     * WHEN: User menekan tombol login dengan google dan memilih akun
     */
    private fun whenUserPressesLoginButtonAndSelectsAccount() {
        if (!scenarioContext.userIsAtLoginPage) {
            Log.e(TAG, "BDD - Precondition failed: User is not at login page")
            return
        }

        Log.d(TAG, "BDD - WHEN: User presses login button and selects Google account")
        signInWithGoogle()
    }

    /**
     * WHEN: User memilih akun ITK
     */
    private fun whenUserSelectsITKAccount(email: String) {
        scenarioContext.selectedAccount = email
        scenarioContext.isITKAccount = isITKEmail(email)
        Log.d(TAG, "BDD - WHEN: User selects account: $email (ITK: ${scenarioContext.isITKAccount})")
    }

    /**
     * THEN: User berhasil login dan berpindah ke halaman beranda (Skenario 1)
     */
    private fun thenUserSuccessfullyLogsInAndNavigatesToHomePage(user: FirebaseUser) {
        if (scenarioContext.isITKAccount) {
            scenarioContext.loginResult = LoginResult.SUCCESS
            Log.d(TAG, "BDD - THEN: User successfully logs in and navigates to home page")

            initializeUserAfterSuccessfulLogin(user)
        }
    }

    /**
     * THEN: User gagal login dan tetap di halaman login dengan pesan error (Skenario 2)
     */
    private fun thenUserFailsToLoginAndStaysAtLoginPageWithError() {
        if (!scenarioContext.isITKAccount) {
            scenarioContext.loginResult = LoginResult.FAILED_INVALID_ACCOUNT
            Log.d(TAG, "BDD - THEN: User fails to login and stays at login page with error")

            showErrorMessageAndStayAtLoginPage()
        }
    }

    /**
     * THEN: User mengalami error teknis saat login
     */
    private fun thenUserExperiencesTechnicalError() {
        scenarioContext.loginResult = LoginResult.FAILED_ERROR
        Log.d(TAG, "BDD - THEN: User experiences technical error during login")
        updateUI(null)
    }

    // ==================== IMPLEMENTATION METHODS ====================

    private fun setupAuthComponents() {
        auth = Firebase.auth
        oneTapClient = Identity.getSignInClient(this)

        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()

        signInLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            handleGoogleSignInResult(result)
        }
    }

    private fun setupLoginButton() {
        val btnLogin = findViewById<Button>(R.id.btn_login)
        btnLogin.setOnClickListener {
            // BDD: WHEN - User menekan tombol login
            whenUserPressesLoginButtonAndSelectsAccount()
        }
    }

    private fun handleGoogleSignInResult(result: androidx.activity.result.ActivityResult) {
        try {
            val googleCredential = oneTapClient.getSignInCredentialFromIntent(result.data)
            val idToken = googleCredential.googleIdToken

            if (idToken != null) {
                val email = googleCredential.id

                // BDD: WHEN - User memilih akun tertentu
                whenUserSelectsITKAccount(email)

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user?.email != null && scenarioContext.isITKAccount) {
                                // BDD: THEN - Skenario 1: Login berhasil
                                thenUserSuccessfullyLogsInAndNavigatesToHomePage(user)
                            } else {
                                // BDD: THEN - Skenario 2: Login gagal karena bukan akun ITK
                                thenUserFailsToLoginAndStaysAtLoginPageWithError()
                            }
                        } else {
                            // BDD: THEN - Login gagal karena error teknis
                            thenUserExperiencesTechnicalError()
                        }
                    }
            } else {
                Log.d(TAG, "No ID token!")
                thenUserExperiencesTechnicalError()
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Sign-in failed", e)
            thenUserExperiencesTechnicalError()
        }
    }

    private fun isITKEmail(email: String): Boolean {
        return email.contains("itk.ac.id")
    }

    private fun initializeUserAfterSuccessfulLogin(user: FirebaseUser) {
        lifecycleScope.launch {
            UserManager.initializeUserData { success, userData ->
                runOnUiThread {
                    if (success && userData != null) {
                        Log.d(TAG, "User data initialized successfully for: ${userData.email}")

                        val welcomeMessage = if (UserManager.isCurrentUserPredefined()) {
                            "Selamat datang, ${userData.namaLengkap}!"
                        } else {
                            "Selamat datang, ${userData.email}!"
                        }

                        Toast.makeText(this@LoginActivity, welcomeMessage, Toast.LENGTH_SHORT).show()
                        updateUI(user)
                    } else {
                        Log.e(TAG, "Failed to initialize user data")
                        Toast.makeText(
                            this@LoginActivity,
                            "Gagal menginisialisasi data pengguna",
                            Toast.LENGTH_SHORT
                        ).show()
                        auth.signOut()
                        updateUI(null)
                    }
                }
            }
        }
    }

    private fun showErrorMessageAndStayAtLoginPage() {
        Toast.makeText(
            this@LoginActivity,
            "Maaf, anda bukan civitas ITK",
            Toast.LENGTH_LONG
        ).show()
        auth.signOut()
        // Tetap di halaman login (tidak navigasi ke halaman lain)
    }

    private fun signInWithGoogle() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this) { result ->
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    signInLauncher.launch(intentSenderRequest)
                } catch (e: Exception) {
                    Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                    thenUserExperiencesTechnicalError()
                }
            }
            .addOnFailureListener(this) { e ->
                Log.d(TAG, e.localizedMessage ?: "Error in sign-in with Google.")
                thenUserExperiencesTechnicalError()
            }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null && UserManager.isUserLoggedIn()) {
            // Validasi email ITK saat app dimulai
            if (isITKEmail(currentUser.email ?: "")) {
                lifecycleScope.launch {
                    UserManager.getCurrentUserData { userData ->
                        runOnUiThread {
                            if (userData != null) {
                                // User sudah login sebelumnya, langsung navigasi
                                scenarioContext.loginResult = LoginResult.SUCCESS
                                updateUI(currentUser)
                            } else {
                                // Initialize user data untuk user yang sudah login
                                whenUserSelectsITKAccount(currentUser.email ?: "")
                                thenUserSuccessfullyLogsInAndNavigatesToHomePage(currentUser)
                            }
                        }
                    }
                }
            } else {
                Log.w(TAG, "Current user email is not ITK email: ${currentUser.email}")
                auth.signOut()
                updateUI(null)
            }
        } else {
            // BDD: GIVEN - User berada di halaman login saat app dimulai
            givenUserIsAtLoginPage()
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            Log.d(TAG, "User is not signed in")
        }
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}