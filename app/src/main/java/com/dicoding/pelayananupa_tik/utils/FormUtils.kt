package com.dicoding.pelayananupa_tik.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.navigation.NavController
import com.dicoding.pelayananupa_tik.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024

object FormUtils {

    fun openPdfPicker(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        launcher.launch(Intent.createChooser(intent, "Pilih File PDF"))
    }

    fun isFileValid(context: Context, uri: Uri): Boolean {
        return if (!isFileSizeValid(context, uri)) {
            Toast.makeText(
                context,
                "File terlalu besar! Maksimal ukuran file adalah 2MB.",
                Toast.LENGTH_LONG
            ).show()
            false
        } else true
    }

    private fun isFileSizeValid(context: Context, uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val fileSize = inputStream?.available() ?: 0
            inputStream?.close()
            fileSize <= MAX_FILE_SIZE_BYTES
        } catch (e: Exception) {
            false
        }
    }

    fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result ?: "unknown_file.pdf"
    }

    fun savePdfLocally(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val fileName = getFileName(context, uri)
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            Toast.makeText(context, "File berhasil disimpan", Toast.LENGTH_SHORT).show()
            file.absolutePath
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal menyimpan file: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun getCurrentFormattedDate(): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date(System.currentTimeMillis()))
    }

    fun resetButton(button: MaterialButton, textResId: Int, context: Context) {
        button.isEnabled = true
        button.text = context.getString(textResId)
    }

    private fun setSubmitButton(button: MaterialButton, textResId: Int, context: Context) {
        button.isEnabled = false
        button.text = context.getString(textResId)
    }

    fun loadUserPhoneNumber(
        firestore: FirebaseFirestore,
        isEditMode: Boolean,
        currentContactText: String?,
        onPhoneNumberLoaded: (String) -> Unit
    ) {
        val userEmail = UserManager.getCurrentUserEmail()
        if (!userEmail.isNullOrEmpty()) {
            firestore.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val userDocument = documents.first()
                        val nomorTelepon = userDocument.getString("nomorTelepon")

                        // Only auto-fill if not in edit mode or field is empty
                        if (!isEditMode || currentContactText?.trim().isNullOrEmpty()) {
                            nomorTelepon?.let { phoneNumber ->
                                if (phoneNumber.isNotEmpty()) {
                                    onPhoneNumberLoaded(phoneNumber)
                                }
                            }
                        }
                    }
                }
        }
    }

    fun saveFormToFirestore(
        firestore: FirebaseFirestore,
        collectionName: String,
        formData: Map<String, Any>,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val userEmail = UserManager.getCurrentUserEmail()
        val formattedDate = getCurrentFormattedDate()

        val dataToSave = formData.toMutableMap().apply {
            userEmail?.let { put("userEmail", it) }
            put("status", "draft")
            put("timestamp", formattedDate)
        }

        firestore.collection(collectionName)
            .add(dataToSave)
            .addOnSuccessListener {
                Toast.makeText(context, "Pengaduan berhasil dikirim", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal mengirim pengaduan", Toast.LENGTH_SHORT).show()
                onFailure()
            }
    }

    fun updateFormInFirestore(
        firestore: FirebaseFirestore,
        collectionName: String,
        documentId: String,
        updateData: Map<String, Any>,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (documentId.isEmpty()) {
            Toast.makeText(context, "Error: Document ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        val formattedDate = getCurrentFormattedDate()
        val dataToUpdate = updateData.toMutableMap().apply {
            put("lastUpdated", formattedDate)
        }

        firestore.collection(collectionName)
            .document(documentId)
            .update(dataToUpdate)
            .addOnSuccessListener {
                Toast.makeText(context, "Data berhasil diupdate", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
            .addOnFailureListener { exception ->
                val errorMessage = "Gagal mengupdate data: ${exception.message}"
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                onFailure(errorMessage)
            }
    }

    fun handleUpdateNavigation(
        navController: NavController,
        fallbackActionId: Int
    ) {
        navController.previousBackStackEntry?.savedStateHandle?.set("data_updated", true)
        try {
            navController.navigateUp()
        } catch (e: Exception) {
            navController.navigate(fallbackActionId)
        }
    }

    fun handleFormSubmission(
        isEditMode: Boolean,
        submitButton: MaterialButton,
        context: Context,
        formData: Map<String, Any>,
        validationResult: Boolean,
        onSubmit: () -> Unit,
        onUpdate: () -> Unit
    ) {
        setSubmitButton(
            submitButton,
            if (isEditMode) R.string.updating else R.string.submitting,
            context
        )

        if (!validationResult) {
            resetButton(
                submitButton,
                if (isEditMode) R.string.update else R.string.submit,
                context
            )
            return
        }

        if (isEditMode) {
            onUpdate()
        } else {
            onSubmit()
        }
    }

    fun handleEditModeError(
        editingItem: Any?,
        submitButton: MaterialButton,
        context: Context,
        onValidItem: (String) -> Unit
    ) {
        when {
            editingItem == null -> {
                resetButton(submitButton, R.string.update, context)
                Toast.makeText(context, "Error: Data item tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
            // For LayananItem, check if documentId is available
            editingItem is com.dicoding.pelayananupa_tik.backend.model.LayananItem &&
                    editingItem.documentId.isEmpty() -> {
                resetButton(submitButton, R.string.update, context)
                Toast.makeText(context, "Error: Document ID tidak valid", Toast.LENGTH_SHORT).show()
            }
            editingItem is com.dicoding.pelayananupa_tik.backend.model.LayananItem -> {
                onValidItem(editingItem.documentId)
            }
        }
    }

    // Extension functions
    fun androidx.fragment.app.Fragment.setupToolbarNavigation(toolbarId: Int) {
        view?.findViewById<androidx.appcompat.widget.Toolbar>(toolbarId)?.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    fun androidx.fragment.app.Fragment.setupEditModeUI(
        isEditMode: Boolean,
        titleTextView: android.widget.TextView,
        submitButton: android.widget.Button,
        editTitleResId: Int
    ) {
        if (isEditMode) {
            titleTextView.text = getString(editTitleResId)
            submitButton.text = getString(R.string.update)
        }
    }
}