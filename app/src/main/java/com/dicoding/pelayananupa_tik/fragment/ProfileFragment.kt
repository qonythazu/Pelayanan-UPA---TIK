package com.dicoding.pelayananupa_tik.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
        // Set all fields as non-editable initially
        setFieldsEditable(false)
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
            // Populate all fields with user data
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

        // Show different UI based on user type
        if (UserManager.isCurrentUserPredefined()) {
            showPredefinedUserUI()
        } else {
            showGenericUserUI(userData.email)
        }
    }

    private fun showPredefinedUserUI() {
        // For predefined users (ITK students), show complete profile
        // All fields are already populated with actual data
        Log.d(TAG, "Showing predefined user UI")

        // You can add special UI elements here if needed
        // For example, show a badge or special indicator
    }

    private fun showGenericUserUI(email: String) {
        // For generic users, show limited profile
        Log.d(TAG, "Showing generic user UI for: $email")

        // You could show the email somewhere if needed
        // Or add a message indicating limited profile
        Toast.makeText(
            requireContext(),
            "Profil terbatas untuk akun: $email",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun setFieldsEditable(editable: Boolean) {
        with(binding) {
            namaLengkapLayout.editText?.isEnabled = editable
            pekerjaanLayout.editText?.isEnabled = editable
            nimLayout.editText?.isEnabled = editable
            programStudiLayout.editText?.isEnabled = editable
            nomorTeleponLayout.editText?.isEnabled = editable

            namaLengkapLayout.editText?.isFocusable = editable
            pekerjaanLayout.editText?.isFocusable = editable
            nimLayout.editText?.isFocusable = editable
            programStudiLayout.editText?.isFocusable = editable
            nomorTeleponLayout.editText?.isFocusable = editable
        }
    }

    private fun showLoading(show: Boolean) {
        // You can implement loading indicator here
        // For example, show/hide a progress bar or disable fields
        with(binding) {
            namaLengkapLayout.isEnabled = !show
            pekerjaanLayout.isEnabled = !show
            nimLayout.isEnabled = !show
            programStudiLayout.isEnabled = !show
            nomorTeleponLayout.isEnabled = !show
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        Log.e(TAG, "Profile error: $message")
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile data when fragment resumes
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