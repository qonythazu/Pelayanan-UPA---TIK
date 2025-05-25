package com.dicoding.pelayananupa_tik.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class UserData(
    val email: String = "",
    val namaLengkap: String = "",
    val pekerjaan: String = "",
    val nim: String = "",
    val programStudi: String = "",
    val nomorTelepon: String = ""
)

object UserManager {
    private const val TAG = "UserManager"
    private const val USERS_COLLECTION = "users"

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val predefinedUsers = mapOf(
        "11201076@student.itk.ac.id" to UserData(
            email = "11201076@student.itk.ac.id",
            namaLengkap = "Putri Qonita Arif",
            pekerjaan = "Mahasiswa",
            nim = "11201076",
            programStudi = "Informatika",
            nomorTelepon = "0852341234"
        ),
        "11201065@student.itk.ac.id" to UserData(
            email = "11201065@student.itk.ac.id",
            namaLengkap = "Muhammad Nasa'i Kairupan",
            pekerjaan = "Mahasiswa",
            nim = "11201065",
            programStudi = "Informatika",
            nomorTelepon = "0853456789"
        )
    )

    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    suspend fun initializeUserData(callback: (Boolean, UserData?) -> Unit) {
        val currentEmail = getCurrentUserEmail()

        if (currentEmail == null) {
            Log.e(TAG, "No user logged in")
            callback(false, null)
            return
        }

        try {
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(currentEmail)
                .get()
                .await()

            if (userDoc.exists()) {
                val userData = userDoc.toObject(UserData::class.java)
                Log.d(TAG, "User data loaded for: $currentEmail")
                callback(true, userData)
            } else {
                val newUserData = createNewUserData(currentEmail)

                firestore.collection(USERS_COLLECTION)
                    .document(currentEmail)
                    .set(newUserData)
                    .await()

                Log.d(TAG, "New user data created for: $currentEmail")
                callback(true, newUserData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing user data", e)
            callback(false, null)
        }
    }

    suspend fun getCurrentUserData(callback: (UserData?) -> Unit) {
        val currentEmail = getCurrentUserEmail()

        if (currentEmail == null) {
            Log.e(TAG, "No user logged in")
            callback(null)
            return
        }

        Log.d(TAG, "Getting user data for: $currentEmail")

        try {
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(currentEmail)
                .get()
                .await()

            if (userDoc.exists()) {
                val userData = userDoc.toObject(UserData::class.java)
                Log.d(TAG, "User data retrieved for: $currentEmail")
                callback(userData)
            } else {
                Log.w(TAG, "User data not found for: $currentEmail, initializing...")
                initializeUserData { success, userData ->
                    callback(if (success) userData else null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user data for: $currentEmail", e)
            callback(null)
        }
    }

    suspend fun updateUserData(userData: UserData, callback: (Boolean) -> Unit) {
        val currentEmail = getCurrentUserEmail()

        if (currentEmail == null) {
            Log.e(TAG, "No user logged in")
            callback(false)
            return
        }

        try {
            firestore.collection(USERS_COLLECTION)
                .document(currentEmail)
                .set(userData)
                .await()

            Log.d(TAG, "User data updated for: $currentEmail")
            callback(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user data", e)
            callback(false)
        }
    }

    private fun createNewUserData(email: String): UserData {
        return if (predefinedUsers.containsKey(email)) {
            predefinedUsers[email]!!
        } else {
            UserData(email = email)
        }
    }

    fun signOut() {
        auth.signOut()
        Log.d(TAG, "User signed out")
    }

    fun isCurrentUserPredefined(): Boolean {
        val currentEmail = getCurrentUserEmail()
        return currentEmail != null && predefinedUsers.containsKey(currentEmail)
    }
}