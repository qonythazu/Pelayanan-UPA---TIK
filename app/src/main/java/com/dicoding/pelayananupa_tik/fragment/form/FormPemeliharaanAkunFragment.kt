package com.dicoding.pelayananupa_tik.fragment.form

import android.app.Activity
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.activity.MainActivity
import com.dicoding.pelayananupa_tik.backend.model.LayananItem
import com.dicoding.pelayananupa_tik.databinding.FragmentFormPemeliharaanAkunBinding
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

class FormPemeliharaanAkunFragment : Fragment() {

    private var _binding: FragmentFormPemeliharaanAkunBinding? = null
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
        _binding = FragmentFormPemeliharaanAkunBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestore = FirebaseFirestore.getInstance()

        checkEditMode()
        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        setupToolbarNavigation(R.id.toolbar)
        setupEditModeUI(isEditMode, binding.textView, binding.btnSubmit, R.string.edit_pemeliharaan_akun)
        setupRadioGroupListeners()
    }

    private fun setupRadioGroupListeners() {
        binding.radioGroupLayanan.setOnCheckedChangeListener { _, _ -> }
        binding.radioGroupJenis.setOnCheckedChangeListener { _, checkedId ->
            binding.textInputLayoutOther.visibility = if (checkedId == R.id.radioOther) {
                View.VISIBLE
            } else {
                View.GONE
            }
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
            radioButton(formData.first, "Harap pilih layanan yang diajukan")
            radioButton(formData.second, "Harap pilih jenis pemeliharaan yang diajukan")
            required(formData.third, binding.namaAkunLayout, "Nama Akun Layanan tidak boleh kosong")
            required(formData.fourth, binding.alasanLayout, "Alasan Pemeliharaan tidak boleh kosong")
            file(selectedPdfUri, requireContext())
        }

        FormUtils.handleFormSubmission(
            isEditMode = isEditMode,
            submitButton = binding.btnSubmit,
            context = requireContext(),
            formData = mapOf(
                "layanan" to formData.first,
                "jenis" to formData.second,
                "akun" to formData.third,
                "alasan" to formData.fourth
            ),
            validationResult = ValidationHelper.validateFormWithRules(
                requireContext(),
                validationRules
            ).isValid,
            onSubmit = { submitNewForm(formData) },
            onUpdate = { updateExistingForm(formData) }
        )
    }

    private fun submitNewForm(formData: Quadruple<String, String, String, String>) {
        val dataToSave = mapOf(
            "judul" to "Form Pemeliharaan Akun",
            "layanan" to formData.first,
            "jenis" to formData.second,
            "akun" to formData.third,
            "alasan" to formData.fourth,
            "filePath" to (savedPdfPath ?: "")
        )

        saveFormToFirestore(
            firestore = firestore,
            collectionName = "form_pemeliharaan",
            formData = dataToSave,
            context = requireContext(),
            onSuccess = {
                clearForm()
                findNavController().navigate(R.id.action_formPemeliharaanAkunFragment_to_historyLayananFragment)
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
                "jenis" to formData.second,
                "akun" to formData.third,
                "alasan" to formData.fourth,
                "filePath" to (savedPdfPath ?: editingItem?.filePath ?: "")
            )

            FormUtils.updateFormInFirestore(
                firestore = firestore,
                collectionName = "form_pemeliharaan",
                documentId = documentId,
                updateData = updateData,
                context = requireContext(),
                onSuccess = {
                    FormUtils.resetButton(binding.btnSubmit, R.string.update, requireContext())
                    FormUtils.handleUpdateNavigation(
                        findNavController(),
                        R.id.action_formPemeliharaanAkunFragment_to_historyLayananFragment
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
                    jenis = args.getString("jenis") ?: "",
                    akun = args.getString("akun") ?: "",
                    alasan = args.getString("alasan") ?: "",
                    filePath = args.getString("filePath") ?: ""
                )
                binding.root.post { populateFormForEdit() }
            }
        }
    }

    private fun populateFormForEdit() {
        editingItem?.let { item ->
            populateLayananRadioButtons(item.layanan)
            populateJenisRadioButtons(item.jenis)
            binding.namaAkunLayout.editText?.setText(item.akun)
            binding.alasanLayout.editText?.setText(item.alasan)
            populateFileData(item.filePath)
        }
    }

    private fun populateLayananRadioButtons(layanan: String) {
        val layananRadioButtons = mapOf(
            "Subdomain" to binding.radioSubDomain,
            "Hosting" to binding.radioHosting,
            "Virtual Private Server (VPS)" to binding.radioVPS,
            "Website" to binding.radioWebsite,
            "Email" to binding.radioEmail,
            "Gerbang ITK" to binding.radioGerbang,
            "Microsoft 365" to binding.radioMic
        )
        layananRadioButtons[layanan]?.isChecked = true
    }

    private fun populateJenisRadioButtons(jenis: String) {
        val jenisRadioButtons = mapOf(
            "Reset Password Akun" to binding.radioReset,
            "Perubahan/Penambahan Data Layanan" to binding.radioPerubahan,
            "Penambahan Penyimpanan" to binding.radioPenambahan
        )

        if (jenisRadioButtons.containsKey(jenis)) {
            jenisRadioButtons[jenis]?.isChecked = true
        } else {
            binding.radioOther.isChecked = true
            binding.textInputLayoutOther.visibility = View.VISIBLE
            binding.editTextOther.setText(jenis)
        }
    }

    private fun populateFileData(filePath: String) {
        if (filePath.isNotEmpty()) {
            val file = File(filePath)
            if (file.exists()) {
                updateFileUI(file.name)
                savedPdfPath = filePath
            }
        }
    }

    private fun getFormData(): Quadruple<String, String, String, String> {
        return Quadruple(
            getSelectedRadioButtonText(binding.radioGroupLayanan),
            getJenisValue(),
            binding.namaAkunLayout.editText?.text.toString().trim(),
            binding.alasanLayout.editText?.text.toString().trim()
        )
    }

    private fun getSelectedRadioButtonText(radioGroup: RadioGroup): String {
        val selectedId = radioGroup.checkedRadioButtonId
        return if (selectedId != -1) {
            view?.findViewById<RadioButton>(selectedId)?.text?.toString() ?: ""
        } else ""
    }

    private fun getJenisValue(): String {
        return when (val selectedId = binding.radioGroupJenis.checkedRadioButtonId) {
            R.id.radioOther -> binding.editTextOther.text.toString().trim()
            -1 -> ""
            else -> view?.findViewById<RadioButton>(selectedId)?.text?.toString() ?: ""
        }
    }

    private fun clearForm() {
        binding.radioGroupLayanan.clearCheck()
        binding.radioGroupJenis.clearCheck()
        binding.textInputLayoutOther.visibility = View.GONE
        binding.editTextOther.text?.clear()
        binding.namaAkunLayout.editText?.text?.clear()
        binding.alasanLayout.editText?.text?.clear()
        resetFileUI()
        selectedPdfUri = null
        savedPdfPath = null
    }

    private fun resetFileUI() {
        binding.tvFileName.text = getString(R.string.no_file_selected)
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