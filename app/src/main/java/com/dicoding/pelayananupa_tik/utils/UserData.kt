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

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Predefined user data for ITK students
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

    /**
     * Get current logged in user email
     */
    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Initialize user data when they first login
     * This should be called after successful Google Sign In
     */
    suspend fun initializeUserData(callback: (Boolean, UserData?) -> Unit) {
        val currentEmail = getCurrentUserEmail()

        if (currentEmail == null) {
            Log.e(TAG, "No user logged in")
            callback(false, null)
            return
        }

        try {
            // Check if user already exists in Firestore
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(currentEmail)
                .get()
                .await()

            if (userDoc.exists()) {
                // User already exists, get their data
                val userData = userDoc.toObject(UserData::class.java)
                Log.d(TAG, "User data loaded for: $currentEmail")
                callback(true, userData)
            } else {
                // User doesn't exist, create new user data
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

    /**
     * Get user data for current logged in user
     */
    suspend fun getCurrentUserData(callback: (UserData?) -> Unit) {
        val currentEmail = getCurrentUserEmail()

        if (currentEmail == null) {
            Log.e(TAG, "No user logged in")
            callback(null)
            return
        }

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
                Log.w(TAG, "User data not found for: $currentEmail")
                callback(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user data", e)
            callback(null)
        }
    }

    /**
     * Update user data (if needed in the future)
     */
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

    /**
     * Create new user data based on email
     * If email is in predefined list, use complete data
     * Otherwise, create with only email
     */
    private fun createNewUserData(email: String): UserData {
        return if (predefinedUsers.containsKey(email)) {
            // Use predefined data for ITK students
            predefinedUsers[email]!!
        } else {
            // Create empty profile for other users
            UserData(email = email)
        }
    }

    /**
     * Sign out user
     */
    fun signOut() {
        auth.signOut()
        Log.d(TAG, "User signed out")
    }

    /**
     * Check if current user is one of the predefined users
     */
    fun isCurrentUserPredefined(): Boolean {
        val currentEmail = getCurrentUserEmail()
        return currentEmail != null && predefinedUsers.containsKey(currentEmail)
    }

    /**
     * Get user data synchronously (for immediate use, but should be used carefully)
     * Returns null if data is not available immediately
     */
    fun getCurrentUserDataSync(): UserData? {
        val currentEmail = getCurrentUserEmail() ?: return null

        // This is not ideal for production, but can be used for immediate checks
        // Better to use the async version above
        return try {
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(currentEmail)
                .get()
                .result

            if (userDoc.exists()) {
                userDoc.toObject(UserData::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user data sync", e)
            null
        }
    }
}