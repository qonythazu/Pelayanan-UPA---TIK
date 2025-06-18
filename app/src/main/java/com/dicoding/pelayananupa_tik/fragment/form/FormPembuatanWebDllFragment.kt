package com.dicoding.pelayananupa_tik.fragment.form

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.backend.model.LayananItem
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPembuatanWebDllBinding
import com.dicoding.pelayananupa_tik.helper.*
import com.dicoding.pelayananupa_tik.utils.FormUtils
import com.dicoding.pelayananupa_tik.utils.FormUtils.saveFormToFirestore
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupEditModeUI
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupToolbarNavigation
import com.google.firebase.firestore.FirebaseFirestore

class FormPembuatanWebDllFragment : Fragment() {

    private var _binding: FragmentFormPembuatanWebDllBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private var isEditMode = false
    private var editingItem: LayananItem? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormPembuatanWebDllBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestore = FirebaseFirestore.getInstance()

        checkEditMode()
        setupUI()
        loadUserPhoneNumber()
        setupClickListeners()
    }

    private fun setupUI() {
        setupToolbarNavigation(R.id.toolbar)
        setupEditModeUI(isEditMode, binding.textView, binding.btnSubmit, R.string.edit_pembuatan_web_dll)
        setupRadioGroupListener()
    }

    private fun setupRadioGroupListener() {
        binding.radioGroupServices.setOnCheckedChangeListener { _, checkedId ->
            binding.textInputLayoutOther.visibility = if (checkedId == R.id.radioOther) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun loadUserPhoneNumber() {
        FormUtils.loadUserPhoneNumber(
            firestore = firestore,
            isEditMode = isEditMode,
            currentContactText = binding.kontakLayout.editText?.text.toString()
        ) { phoneNumber ->
            binding.kontakLayout.editText?.setText(phoneNumber)
        }
    }

    private fun setupClickListeners() {
        binding.btnSubmit.setOnClickListener {
            handleFormSubmission()
        }
    }

    private fun handleFormSubmission() {
        val formData = getFormData()
        val validationRules = buildValidation {
            radioValidation(formData.first, "Harap pilih layanan yang diajukan")
            required(formData.second, binding.namaLayananLayout, "Nama Layanan tidak boleh kosong")
            phone(formData.third, binding.kontakLayout)
            required(formData.fourth, binding.tujuanPembuatanLayout, "Tujuan tidak boleh kosong")
        }

        FormUtils.handleFormSubmission(
            isEditMode = isEditMode,
            submitButton = binding.btnSubmit,
            context = requireContext(),
            formData = mapOf(
                "layanan" to formData.first,
                "namaLayanan" to formData.second,
                "kontak" to formData.third,
                "tujuan" to formData.fourth
            ),
            validationResult = ValidationHelper.validateFormWithRules(
                requireContext(),
                validationRules
            ).isValid,
            onSubmit = { submitNewForm(formData) },
            onUpdate = { updateExistingForm(formData) }
        )
    }

    private fun radioValidation(layanan: String, errorMessage: String): Boolean {
        return validateRadioButtonField(layanan, errorMessage, requireContext())
    }

    private fun submitNewForm(formData: Quadruple<String, String, String, String>) {
        val dataToSave = mapOf(
            "judul" to "Form Pembuatan Web/DLL",
            "layanan" to formData.first,
            "namaLayanan" to formData.second,
            "kontak" to formData.third,
            "tujuan" to formData.fourth
        )

        saveFormToFirestore(
            firestore = firestore,
            collectionName = "form_pembuatan",
            formData = dataToSave,
            context = requireContext(),
            onSuccess = {
                clearForm()
                findNavController().navigate(R.id.action_formPembuatanWebDllFragment_to_historyLayananFragment)
            },
            onFailure = {
                FormUtils.resetButton(binding.btnSubmit, R.string.submit, requireContext())
            }
        )
    }

    private fun updateExistingForm(formData: Quadruple<String, String, String, String>) {
        FormUtils.handleEditModeError(
            editingItem = editingItem,
            submitButton = binding.btnSubmit,
            context = requireContext()
        ) { documentId ->
            val updateData = mapOf(
                "layanan" to formData.first,
                "namaLayanan" to formData.second,
                "kontak" to formData.third,
                "tujuan" to formData.fourth
            )

            FormUtils.updateFormInFirestore(
                firestore = firestore,
                collectionName = "form_pembuatan",
                documentId = documentId,
                updateData = updateData,
                context = requireContext(),
                onSuccess = {
                    FormUtils.resetButton(binding.btnSubmit, R.string.update, requireContext())
                    FormUtils.handleUpdateNavigation(
                        findNavController(),
                        R.id.action_formPembuatanWebDllFragment_to_historyLayananFragment
                    )
                },
                onFailure = {
                    FormUtils.resetButton(binding.btnSubmit, R.string.update, requireContext())
                }
            )
        }
    }

    private fun checkEditMode() {
        arguments?.let { args ->
            val documentId = args.getString("documentId")
            if (!documentId.isNullOrEmpty()) {
                isEditMode = true
                editingItem = LayananItem(
                    documentId = documentId,
                    layanan = args.getString("layanan") ?: "",
                    namaLayanan = args.getString("namaLayanan") ?: "",
                    kontak = args.getString("kontak") ?: "",
                    tujuan = args.getString("tujuan") ?: ""
                )
                binding.root.post { populateFormForEdit() }
            }
        }
    }

    private fun populateFormForEdit() {
        editingItem?.let { item ->
            setupRadioButtonSelection(item.layanan)
            binding.namaLayananLayout.editText?.setText(item.namaLayanan)
            binding.kontakLayout.editText?.setText(item.kontak)
            binding.tujuanPembuatanLayout.editText?.setText(item.tujuan)
        }
    }

    private fun setupRadioButtonSelection(layanan: String) {
        val layananRadioButtons = mapOf(
            "Subdomain" to binding.radioSubDomain,
            "Hosting" to binding.radioHosting,
            "Virtual Private Server (VPS)" to binding.radioVPS,
            "Website" to binding.radioWebsite,
            "Email" to binding.radioEmail
        )

        if (layananRadioButtons.containsKey(layanan)) {
            layananRadioButtons[layanan]?.isChecked = true
        } else {
            binding.radioOther.isChecked = true
            binding.textInputLayoutOther.visibility = View.VISIBLE
            binding.editTextOther.setText(layanan)
        }
    }

    private fun getFormData(): Quadruple<String, String, String, String> {
        val selectedRadioButtonLayanan = binding.radioGroupServices.checkedRadioButtonId

        val layanan = when {
            selectedRadioButtonLayanan == R.id.radioOther -> binding.editTextOther.text.toString().trim()
            selectedRadioButtonLayanan != -1 -> view?.findViewById<RadioButton>(selectedRadioButtonLayanan)?.text?.toString() ?: ""
            else -> ""
        }

        return Quadruple(
            layanan,
            binding.namaLayananLayout.editText?.text.toString().trim(),
            binding.kontakLayout.editText?.text.toString().trim(),
            binding.tujuanPembuatanLayout.editText?.text.toString().trim()
        )
    }

    private fun clearForm() {
        binding.radioGroupServices.clearCheck()
        binding.textInputLayoutOther.visibility = View.GONE
        binding.editTextOther.text?.clear()
        binding.namaLayananLayout.editText?.text?.clear()
        binding.kontakLayout.editText?.text?.clear()
        binding.tujuanPembuatanLayout.editText?.text?.clear()
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.hideBottomNavigation()
        (activity as? MainActivity)?.hideToolbar()
    }

    override fun onPause() {
        super.onPause()
        (activity as? MainActivity)?.showBottomNavigation()
        (activity as? MainActivity)?.showToolbar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}