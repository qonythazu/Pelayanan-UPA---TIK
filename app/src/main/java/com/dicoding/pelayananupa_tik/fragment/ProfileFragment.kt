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
        setFieldsReadOnly()
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