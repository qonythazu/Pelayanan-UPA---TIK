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
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPengaduanLayananBinding
import com.dicoding.pelayananupa_tik.helper.*
import com.dicoding.pelayananupa_tik.utils.FormUtils
import com.dicoding.pelayananupa_tik.utils.FormUtils.getFileName
import com.dicoding.pelayananupa_tik.utils.FormUtils.isFileValid
import com.dicoding.pelayananupa_tik.utils.FormUtils.openPdfPicker
import com.dicoding.pelayananupa_tik.utils.FormUtils.saveFormToFirestore
import com.dicoding.pelayananupa_tik.utils.FormUtils.savePdfLocally
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupEditModeUI
import com.dicoding.pelayananupa_tik.utils.FormUtils.setupToolbarNavigation
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class FormPengaduanLayananFragment : Fragment() {

    private var _binding: FragmentFormPengaduanLayananBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private var selectedPdfUri: Uri? = null
    private var savedPdfPath: String? = null
    private var isEditMode = false
    private var editingItem: LayananItem? = null

    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handlePdfSelection(result.data?.data)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormPengaduanLayananBinding.inflate(inflater, container, false)
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
        setupEditModeUI(isEditMode, binding.textView, binding.btnSubmit, R.string.edit_pengaduan_layanan)
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
        binding.btnChooseFile.setOnClickListener { openPdfPicker(pdfPickerLauncher) }
        binding.btnSubmit.setOnClickListener { handleFormSubmission() }
    }

    private fun handlePdfSelection(uri: Uri?) {
        selectedPdfUri = uri
        uri?.let {
            if (isFileValid(requireContext(), it)) {
                val fileName = getFileName(requireContext(), it)
                updateFileUI(fileName)
                savedPdfPath = savePdfLocally(requireContext(), it)
            }
        }
    }

    private fun updateFileUI(fileName: String) {
        binding.tvFileName.text = getString(R.string.file_selected, " $fileName")
        binding.btnChooseFile.apply {
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.primary_blue)
            )
            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            text = getString(R.string.change_image)
            strokeWidth = 0
        }
    }

    private fun handleFormSubmission() {
        val formData = getFormData()
        val validationRules = buildValidation {
            required(formData.first, binding.layananLayout, "Layanan tidak boleh kosong")
            phone(formData.second, binding.kontakLayout)
            required(formData.third, binding.keluhanAndaLayout, "Keluhan tidak boleh kosong")
            file(selectedPdfUri, requireContext())
        }

        FormUtils.handleFormSubmission(
            isEditMode = isEditMode,
            submitButton = binding.btnSubmit,
            context = requireContext(),
            formData = mapOf(
                "layanan" to formData.first,
                "kontak" to formData.second,
                "keluhan" to formData.third
            ),
            validationResult = ValidationHelper.validateFormWithRules(
                requireContext(),
                validationRules
            ).isValid,
            onSubmit = { submitNewForm(formData) },
            onUpdate = { updateExistingForm(formData) }
        )
    }

    private fun submitNewForm(formData: Triple<String, String, String>) {
        val dataToSave = mapOf(
            "judul" to "Form Pengaduan Layanan",
            "layanan" to formData.first,
            "kontak" to formData.second,
            "keluhan" to formData.third,
            "filePath" to (savedPdfPath ?: "")
        )

        saveFormToFirestore(
            firestore = firestore,
            collectionName = "form_pengaduan",
            formData = dataToSave,
            context = requireContext(),
            onSuccess = {
                clearForm()
                findNavController().navigate(R.id.action_formPengaduanLayananFragment_to_historyLayananFragment)
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
                "layanan" to formData.first,
                "kontak" to formData.second,
                "keluhan" to formData.third,
                "filePath" to (savedPdfPath ?: editingItem?.filePath ?: "")
            )

            FormUtils.updateFormInFirestore(
                firestore = firestore,
                collectionName = "form_pengaduan",
                documentId = documentId,
                updateData = updateData,
                context = requireContext(),
                onSuccess = {
                    FormUtils.resetButton(binding.btnSubmit, R.string.update, requireContext())
                    FormUtils.handleUpdateNavigation(
                        findNavController(),
                        R.id.action_formPengaduanLayananFragment_to_historyLayananFragment
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
                    kontak = args.getString("kontak") ?: "",
                    keluhan = args.getString("keluhan") ?: "",
                    filePath = args.getString("filePath") ?: ""
                )
                binding.root.post { populateFormForEdit() }
            }
        }
    }

    private fun populateFormForEdit() {
        editingItem?.let { item ->
            binding.layananLayout.editText?.setText(item.layanan)
            binding.kontakLayout.editText?.setText(item.kontak)
            binding.keluhanAndaLayout.editText?.setText(item.keluhan)
            populateFileData(item.filePath)
        }
    }

    private fun populateFileData(filePath: String) {
        if (filePath.isNotEmpty()) {
            val file = File(filePath)
            if (file.exists()) {
                binding.tvFileName.text = getString(R.string.file_selected, " ${file.name}")
                binding.btnChooseFile.text = getString(R.string.change_file)
                savedPdfPath = filePath
            }
        }
    }

    private fun getFormData(): Triple<String, String, String> {
        return Triple(
            binding.layananLayout.editText?.text.toString().trim(),
            binding.kontakLayout.editText?.text.toString().trim(),
            binding.keluhanAndaLayout.editText?.text.toString().trim()
        )
    }

    private fun clearForm() {
        binding.layananLayout.editText?.text?.clear()
        binding.kontakLayout.editText?.text?.clear()
        binding.keluhanAndaLayout.editText?.text?.clear()
        resetFileUI()
        selectedPdfUri = null
        savedPdfPath = null
    }

    private fun resetFileUI() {
        binding.tvFileName.text = ""
        binding.btnChooseFile.apply {
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
            setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
            text = getString(R.string.choose_file)
            strokeWidth = 2
            strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_blue))
        }
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