package com.dicoding.pelayananupa_tik.fragment.form

import android.app.Activity
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.backend.model.LayananItem
import com.dicoding.pelayananupa_tik.databinding.FragmentFormBantuanOperatorBinding
import com.dicoding.pelayananupa_tik.helper.*
import com.dicoding.pelayananupa_tik.utils.FormUtils
import com.dicoding.pelayananupa_tik.utils.FormUtils.getFileName
import com.dicoding.pelayananupa_tik.utils.FormUtils.isFileValid
import com.dicoding.pelayananupa_tik.utils.FormUtils.openPdfPicker
import com.dicoding.pelayananupa_tik.utils.FormUtils.savePdfLocally
import com.dicoding.pelayananupa_tik.utils.FormUtils.saveFormToFirestore
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupEditModeUI
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupToolbarNavigation
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class FormBantuanOperatorFragment : Fragment() {

    private var _binding: FragmentFormBantuanOperatorBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private var selectedPdfUri: Uri? = null
    private var savedPdfPath: String? = null
    private var isEditMode = false
    private var editingItem: LayananItem? = null

    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handlePdfPickerResult(result)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormBantuanOperatorBinding.inflate(inflater, container, false)
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
        setupEditModeUI(isEditMode, binding.textView, binding.btnSubmit, R.string.edit_bantuan_operator_tik)
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
        binding.btnChooseFile.setOnClickListener {
            openPdfPicker(pdfPickerLauncher)
        }

        binding.btnSubmit.setOnClickListener {
            handleFormSubmission()
        }
    }

    private fun handlePdfPickerResult(result: androidx.activity.result.ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            selectedPdfUri = result.data?.data
            selectedPdfUri?.let { uri ->
                if (isFileValid(requireContext(), uri)) {
                    updateFileSelection(uri)
                }
            }
        }
    }

    private fun updateFileSelection(uri: Uri) {
        val fileName = getFileName(requireContext(), uri)
        binding.tvFileName.text = getString(R.string.file_selected, " $fileName")

        binding.btnChooseFile.apply {
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.primary_blue)
            )
            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            text = getString(R.string.change_image)
            strokeWidth = 0
        }

        savedPdfPath = savePdfLocally(requireContext(), uri)
    }

    private fun handleFormSubmission() {
        val formData = getFormData()
        val validationRules = buildValidation {
            required(formData.first, binding.jumlahLayout, "Jumlah tidak boleh kosong")
            phone(formData.second, binding.kontakLayout)
            required(formData.third, binding.tujuanPeminjamanLayout, "Tujuan tidak boleh kosong")
            fileValidation()
        }

        FormUtils.handleFormSubmission(
            isEditMode = isEditMode,
            submitButton = binding.btnSubmit,
            context = requireContext(),
            formData = mapOf(
                "jumlah" to formData.first,
                "kontak" to formData.second,
                "tujuan" to formData.third,
                "filePath" to (savedPdfPath ?: "")
            ),
            validationResult = ValidationHelper.validateFormWithRules(
                requireContext(),
                validationRules
            ).isValid,
            onSubmit = { submitNewForm(formData) },
            onUpdate = { updateExistingForm(formData) }
        )
    }

    private fun fileValidation(): Boolean {
        return selectedPdfUri?.let { isFileValid(requireContext(), it) } ?: true
    }

    private fun submitNewForm(formData: Triple<String, String, String>) {
        val dataToSave = mapOf(
            "judul" to "Bantuan Operator TIK",
            "jumlah" to formData.first,
            "kontak" to formData.second,
            "tujuan" to formData.third,
            "filePath" to (savedPdfPath ?: "")
        )

        saveFormToFirestore(
            firestore = firestore,
            collectionName = "form_bantuan",
            formData = dataToSave,
            context = requireContext(),
            onSuccess = {
                clearForm()
                findNavController().navigate(R.id.action_formBantuanOperatorFragment_to_historyLayananFragment)
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
                "jumlah" to formData.first,
                "kontak" to formData.second,
                "tujuan" to formData.third,
                "filePath" to (savedPdfPath ?: editingItem?.filePath ?: "")
            )

            FormUtils.updateFormInFirestore(
                firestore = firestore,
                collectionName = "form_bantuan",
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
                    jumlah = args.getString("jumlah") ?: "",
                    kontak = args.getString("kontak") ?: "",
                    tujuan = args.getString("tujuan") ?: "",
                    filePath = args.getString("filePath") ?: ""
                )
                binding.root.post { populateFormForEdit() }
            }
        }
    }

    private fun populateFormForEdit() {
        editingItem?.let { item ->
            binding.jumlahLayout.editText?.setText(item.jumlah)
            binding.kontakLayout.editText?.setText(item.kontak)
            binding.tujuanPeminjamanLayout.editText?.setText(item.tujuan)

            if (item.filePath.isNotEmpty()) {
                val file = File(item.filePath)
                if (file.exists()) {
                    binding.tvFileName.text = getString(R.string.file_selected, " ${file.name}")
                    binding.btnChooseFile.text = getString(R.string.change_file)
                    savedPdfPath = item.filePath
                }
            }
        }
    }

    private fun getFormData(): Triple<String, String, String> {
        return Triple(
            binding.jumlahLayout.editText?.text.toString().trim(),
            binding.kontakLayout.editText?.text.toString().trim(),
            binding.tujuanPeminjamanLayout.editText?.text.toString().trim()
        )
    }

    private fun clearForm() {
        binding.jumlahLayout.editText?.text?.clear()
        binding.kontakLayout.editText?.text?.clear()
        binding.tujuanPeminjamanLayout.editText?.text?.clear()
        resetFileSelection()
    }

    private fun resetFileSelection() {
        binding.tvFileName.text = getString(R.string.no_file_selected)
        binding.btnChooseFile.apply {
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.white)
            )
            setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
            text = getString(R.string.choose_file)
            strokeWidth = 2
            strokeColor = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.primary_blue)
            )
        }
        selectedPdfUri = null
        savedPdfPath = null
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