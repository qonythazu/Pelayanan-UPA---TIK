package com.dicoding.pelayananupa_tik.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.databinding.FragmentProfileBinding
import com.dicoding.pelayananupa_tik.utils.UserManager
import com.dicoding.pelayananupa_tik.utils.UserData
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadUserProfile()
    }

    private fun setupUI() {
        setFieldsReadOnly()
        setupPhoneEditButton()
    }

    private fun setupPhoneEditButton() {
        binding.nomorTeleponLayout.setEndIconOnClickListener {
            showEditPhoneDialog()
        }
    }

    private fun showEditPhoneDialog() {
        val editText = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_PHONE
            hint = "Masukkan nomor telepon"
            val currentPhone = binding.nomorTeleponLayout.editText?.text.toString()
            if (currentPhone != "Tidak tersedia") {
                setText(currentPhone)
                setSelection(text.length)
            }
        }

        val errorText = android.widget.TextView(requireContext()).apply {
            text = ""
            setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            textSize = 12f
            visibility = View.GONE
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 0)
            addView(editText)
            addView(errorText)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Nomor Telepon")
            .setMessage("Minimal 10 digit angka")
            .setView(container)
            .setPositiveButton("Simpan", null) // Set null dulu
            .setNegativeButton("Batal", null)
            .create()

        // Override positive button click setelah dialog dibuat
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val newPhone = editText.text.toString().trim()
                if (isValidPhoneNumber(newPhone)) {
                    updatePhoneNumber(newPhone)
                    dialog.dismiss() // Tutup dialog hanya jika valid
                } else {
                    errorText.text = getString(R.string.nomor_telepon_harus_minimal_10_digit_dan_format_valid)
                    errorText.visibility = View.VISIBLE
                    // Dialog tetap terbuka karena tidak ada dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun updatePhoneNumber(newPhone: String) {
        showLoading(true)
        UserManager.updatePhoneNumber(newPhone) { success ->
            activity?.runOnUiThread {
                showLoading(false)
                if (success) {
                    binding.nomorTeleponLayout.editText?.setText(newPhone)
                    Toast.makeText(requireContext(), "Nomor telepon berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Phone number updated to: $newPhone")
                } else {
                    Toast.makeText(requireContext(), "Gagal memperbarui nomor telepon", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Failed to update phone number")
                }
            }
        }
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        // Hapus semua karakter non-digit
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")

        // Cek panjang minimal dan maksimal
        if (digitsOnly.length < 10 || digitsOnly.length > 15) {
            return false
        }

        // Validasi format nomor Indonesia
        return when {
            // Format +62 (kode negara Indonesia)
            digitsOnly.startsWith("62") -> {
                val localNumber = digitsOnly.substring(2)
                isValidIndonesianLocalNumber(localNumber)
            }
            // Format 0 (format lokal Indonesia)
            digitsOnly.startsWith("0") -> {
                val localNumber = digitsOnly.substring(1)
                isValidIndonesianLocalNumber(localNumber)
            }
            // Format tanpa awalan (langsung nomor operator)
            else -> {
                isValidIndonesianLocalNumber(digitsOnly)
            }
        }
    }

    private fun isValidIndonesianLocalNumber(localNumber: String): Boolean {
        // Cek panjang nomor lokal (9-13 digit setelah kode area/operator)
        if (localNumber.length < 9 || localNumber.length > 13) {
            return false
        }

        // Validasi prefix operator Indonesia
        val validPrefixes = listOf(
            // Telkomsel
            "811", "812", "813", "821", "822", "823", "851", "852", "853",
            // Indosat
            "814", "815", "816", "855", "856", "857", "858",
            // XL
            "817", "818", "819", "859", "877", "878",
            // Tri (3)
            "895", "896", "897", "898", "899",
            // Smartfren
            "881", "882", "883", "884", "885", "886", "887", "888", "889",
            // Axis
            "831", "832", "833", "838",
            // Telkom (PSTN)
            "21", "22", "24", "31", "341", "343", "361", "370", "380", "401", "411", "421", "431", "451", "471", "481", "511", "541", "561", "571", "601", "620", "651", "717", "721", "741", "751", "761", "771", "778"
        )

        return validPrefixes.any { prefix -> localNumber.startsWith(prefix) }
    }

    private fun loadUserProfile() {
        if (!UserManager.isUserLoggedIn()) {
            showError("User belum login")
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            UserManager.getCurrentUserData { userData ->
                activity?.runOnUiThread {
                    showLoading(false)

                    if (userData != null) {
                        populateFields(userData)
                        Log.d(TAG, "Profile loaded for user: ${userData.email}")
                    } else {
                        showError("Gagal memuat data profil")
                    }
                }
            }
        }
    }

    private fun populateFields(userData: UserData) {
        with(binding) {
            namaLengkapLayout.editText?.setText(
                userData.namaLengkap.ifEmpty { "Tidak tersedia" }
            )

            pekerjaanLayout.editText?.setText(
                userData.pekerjaan.ifEmpty { "Tidak tersedia" }
            )

            nimLayout.editText?.setText(
                userData.nim.ifEmpty { "Tidak tersedia" }
            )

            programStudiLayout.editText?.setText(
                userData.programStudi.ifEmpty { "Tidak tersedia" }
            )

            nomorTeleponLayout.editText?.setText(
                userData.nomorTelepon.ifEmpty { "Tidak tersedia" }
            )
        }

        if (UserManager.isCurrentUserPredefined()) {
            showPredefinedUserUI()
        } else {
            showGenericUserUI(userData.email)
        }
    }

    private fun showPredefinedUserUI() {
        Log.d(TAG, "Showing predefined user UI")
    }

    private fun showGenericUserUI(email: String) {
        Log.d(TAG, "Showing generic user UI for: $email")

        Toast.makeText(
            requireContext(),
            "Profil terbatas untuk akun: $email",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun setFieldsReadOnly() {
        with(binding) {
            namaLengkapLayout.editText?.apply {
                isEnabled = false
                isFocusable = false
            }
            pekerjaanLayout.editText?.apply {
                isEnabled = false
                isFocusable = false
            }
            nimLayout.editText?.apply {
                isEnabled = false
                isFocusable = false
            }
            programStudiLayout.editText?.apply {
                isEnabled = false
                isFocusable = false
            }
            // Nomor telepon tetap disabled karena kita pakai end icon untuk edit
            nomorTeleponLayout.editText?.apply {
                isEnabled = false
                isFocusable = false
            }
        }
    }

    private fun showLoading(show: Boolean) {
        with(binding) {
            val alpha = if (show) 0.5f else 1.0f
            namaLengkapLayout.alpha = alpha
            pekerjaanLayout.alpha = alpha
            nimLayout.alpha = alpha
            programStudiLayout.alpha = alpha
            nomorTeleponLayout.alpha = alpha
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        Log.e(TAG, "Profile error: $message")
    }

    override fun onResume() {
        super.onResume()
        if (UserManager.isUserLoggedIn()) {
            loadUserProfile()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ProfileFragment"
    }
}