package com.dicoding.pelayananupa_tik.fragment.form

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.backend.model.LayananItem
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPemasanganPerangkatBinding
import com.dicoding.pelayananupa_tik.helper.*
import com.dicoding.pelayananupa_tik.utils.FormUtils
import com.dicoding.pelayananupa_tik.utils.FormUtils.saveFormToFirestore
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupEditModeUI
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupToolbarNavigation
import com.google.firebase.firestore.FirebaseFirestore

class FormPemasanganPerangkatFragment : Fragment() {

    private var _binding: FragmentFormPemasanganPerangkatBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private var isEditMode = false
    private var editingItem: LayananItem? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormPemasanganPerangkatBinding.inflate(inflater, container, false)
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
        setupEditModeUI(isEditMode, binding.textView, binding.btnSubmit, R.string.edit_pemasangan_perangkat)
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
            required(formData.first, binding.jenisPerangkatLayout, "Jenis perangkat tidak boleh kosong")
            phone(formData.second, binding.kontakLayout)
            required(formData.third, binding.tujuanPemasanganLayout, "Tujuan tidak boleh kosong")
        }

        FormUtils.handleFormSubmission(
            isEditMode = isEditMode,
            submitButton = binding.btnSubmit,
            context = requireContext(),
            formData = mapOf(
                "jenis" to formData.first,
                "kontak" to formData.second,
                "tujuan" to formData.third
            ),
            validationResult = ValidationHelper.validateFormWithRules(
                requireContext(),
                validationRules).isValid,
            onSubmit = { submitNewForm(formData) },
            onUpdate = { updateExistingForm(formData) }
        )
    }

    private fun submitNewForm(formData: Triple<String, String, String>) {
        val dataToSave = mapOf(
            "judul" to "Pemasangan Perangkat",
            "jenis" to formData.first,
            "kontak" to formData.second,
            "tujuan" to formData.third
        )

        saveFormToFirestore(
            firestore = firestore,
            collectionName = "form_pemasangan",
            formData = dataToSave,
            context = requireContext(),
            onSuccess = {
                clearForm()
                findNavController().navigate(R.id.action_formPemasanganPerangkatFragment_to_historyLayananFragment)
            },
            onFailure = {
                FormUtils.resetButton(binding.btnSubmit, R.string.submit, requireContext())
            }
        )
    }

    private fun updateExistingForm(formData: Triple<String, String, String>) {
        FormUtils.handleEditModeError(
            editingItem = editingItem,
            submitButton = binding.btnSubmit,
            context = requireContext()
        ) { documentId ->
            val updateData = mapOf(
                "jenis" to formData.first,
                "kontak" to formData.second,
                "tujuan" to formData.third
            )

            FormUtils.updateFormInFirestore(
                firestore = firestore,
                collectionName = "form_pemasangan",
                documentId = documentId,
                updateData = updateData,
                context = requireContext(),
                onSuccess = {
                    FormUtils.resetButton(binding.btnSubmit, R.string.update, requireContext())
                    FormUtils.handleUpdateNavigation(
                        findNavController(),
                        R.id.action_formBantuanOperatorFragment_to_historyLayananFragment
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
                    jenis = args.getString("jenis") ?: "",
                    kontak = args.getString("kontak") ?: "",
                    tujuan = args.getString("tujuan") ?: ""
                )
                binding.root.post { populateFormForEdit() }
            }
        }
    }

    private fun populateFormForEdit() {
        editingItem?.let { item ->
            binding.jenisPerangkatLayout.editText?.setText(item.jenis)
            binding.kontakLayout.editText?.setText(item.kontak)
            binding.tujuanPemasanganLayout.editText?.setText(item.tujuan)
        }
    }

    private fun getFormData(): Triple<String, String, String> {
        return Triple(
            binding.jenisPerangkatLayout.editText?.text.toString().trim(),
            binding.kontakLayout.editText?.text.toString().trim(),
            binding.tujuanPemasanganLayout.editText?.text.toString().trim()
        )
    }

    private fun clearForm() {
        binding.jenisPerangkatLayout.editText?.text?.clear()
        binding.kontakLayout.editText?.text?.clear()
        binding.tujuanPemasanganLayout.editText?.text?.clear()
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