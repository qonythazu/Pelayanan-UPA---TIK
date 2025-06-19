package com.dicoding.pelayananupa_tik.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.dicoding.pelayananupa_tik.R
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanFragment : BottomSheetDialogFragment() {

    // ==================== BDD CONTEXT ====================
    private data class ScanScenarioContext(
        var userIsAtScanPage: Boolean = false,
        var cameraIsActive: Boolean = false,
        var qrCodeDetected: String? = null,
        var isValidQRCode: Boolean = false,
        var scanResult: ScanResult = ScanResult.PENDING,
        var itemData: ItemData? = null
    )

    private enum class ScanResult {
        PENDING, SUCCESS, FAILED_NOT_FOUND, FAILED_ERROR
    }

    private data class ItemData(
        val nama: String,
        val tanggalMasuk: String,
        val jenisBarang: String,
        val pemilikBarang: String,
        val letakBarang: String,
        val serialNumber: String
    )

    private val scenarioContext = ScanScenarioContext()

    // ==================== EXISTING PROPERTIES ====================
    private lateinit var viewfinder: androidx.camera.view.PreviewView
    private lateinit var overlay: View
    private lateinit var scanLine: View
    private lateinit var btnCancel: Button
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private var isScanning = false
    private var isProcessingResult = false
    private var isCameraSetup = false
    private val firestore = FirebaseFirestore.getInstance()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            setupCamera()
        } else {
            Toast.makeText(requireContext(), "Izin kamera diperlukan untuk scan", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    interface ScanResultListener {
        fun onScanResult(
            nama: String,
            tanggalMasuk: String,
            jenisBarang: String,
            pemilikBarang: String,
            letakBarang: String,
            serialNumber: String
        )
    }

    private var scanResultListener: ScanResultListener? = null

    fun setScanResultListener(listener: ScanResultListener) {
        this.scanResultListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // BDD: GIVEN - User telah login dan berada di halaman scan
        givenUserIsLoggedInAndAtScanPage()

        viewfinder = view.findViewById(R.id.viewfinder)
        overlay = view.findViewById(R.id.overlay_viewfinder)
        scanLine = view.findViewById(R.id.scan_line)
        btnCancel = view.findViewById(R.id.btn_cancel)

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39
            )
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)
        cameraExecutor = Executors.newSingleThreadExecutor()
        btnCancel.setOnClickListener { dismiss() }

        startScanLineAnimation()
        checkCameraPermission()
        Log.d(TAG, "Fragment view created")

        try {
            GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(requireActivity())
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Google Play Services", e)
        }
    }

    // ==================== BDD METHODS ====================

    /**
     * GIVEN: User telah login dan berada di halaman scan
     */
    private fun givenUserIsLoggedInAndAtScanPage() {
        scenarioContext.userIsAtScanPage = true
        scenarioContext.scanResult = ScanResult.PENDING
        Log.d(TAG, "BDD - GIVEN: User is logged in and at scan page")
    }

    /**
     * WHEN: User mengarahkan kamera dan melakukan scan terhadap QR code barang
     */
    private fun whenUserPointsCameraAndScansQRCode(qrCodeValue: String) {
        if (!scenarioContext.userIsAtScanPage || !scenarioContext.cameraIsActive) {
            Log.e(TAG, "BDD - Precondition failed: User is not at scan page or camera is not active")
            return
        }

        scenarioContext.qrCodeDetected = qrCodeValue
        Log.d(TAG, "BDD - WHEN: User points camera and scans QR code: $qrCodeValue")

        // Fetch data dari Firestore untuk validasi
        fetchDataFromFirestoreForValidation(qrCodeValue)
    }

    /**
     * WHEN: User mengarahkan kamera dan melakukan scan terhadap QR code yang tidak valid
     */
    private fun whenUserScansInvalidQRCode(qrCodeValue: String) {
        if (!scenarioContext.userIsAtScanPage || !scenarioContext.cameraIsActive) {
            Log.e(TAG, "BDD - Precondition failed: User is not at scan page or camera is not active")
            return
        }

        scenarioContext.qrCodeDetected = qrCodeValue
        scenarioContext.isValidQRCode = false
        Log.d(TAG, "BDD - WHEN: User scans invalid QR code: $qrCodeValue")

        // Langsung set sebagai tidak valid dan fetch data
        fetchDataFromFirestoreForValidation(qrCodeValue)
    }

    /**
     * THEN: Detail dari barang akan ditampilkan (Skenario 1)
     */
    private fun thenItemDetailsAreDisplayed(itemData: ItemData) {
        if (scenarioContext.isValidQRCode) {
            scenarioContext.scanResult = ScanResult.SUCCESS
            scenarioContext.itemData = itemData

            Log.d(TAG, "BDD - THEN: Item details are displayed for ${itemData.nama}")

            displayItemDetailsAndNavigate(itemData)
        }
    }

    /**
     * THEN: User melihat pesan error "Barang tidak ditemukan" dan kembali ke home (Skenario 2)
     */
    private fun thenUserSeesErrorMessageAndReturnsToHome() {
        if (!scenarioContext.isValidQRCode) {
            scenarioContext.scanResult = ScanResult.FAILED_NOT_FOUND

            Log.d(TAG, "BDD - THEN: User sees error message and returns to home")

            showItemNotFoundMessageAndNavigateToHome()
        }
    }

    /**
     * THEN: User mengalami error teknis saat scan
     */
    private fun thenUserExperiencesTechnicalError(errorMessage: String? = null) {
        scenarioContext.scanResult = ScanResult.FAILED_ERROR
        Log.d(TAG, "BDD - THEN: User experiences technical error during scan")

        showTechnicalErrorMessage(errorMessage)
    }

    // ==================== IMPLEMENTATION METHODS ====================

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "Camera permission already granted")
                setupCamera()
            }
            else -> {
                Log.d(TAG, "Requesting camera permission")
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startScanLineAnimation() {
        scanLine.post {
            val height = overlay.height.toFloat()
            val animation = TranslateAnimation(0f, 0f, 0f, height)
            animation.duration = 2000
            animation.repeatCount = TranslateAnimation.INFINITE
            animation.repeatMode = TranslateAnimation.RESTART
            scanLine.startAnimation(animation)
            Log.d(TAG, "Scan line animation started")
        }
    }

    private fun setupCamera() {
        if (isCameraSetup) {
            Log.d(TAG, "Camera already setup, skipping")
            return
        }

        Log.d(TAG, "Setting up camera")
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(cameraProvider)
                isCameraSetup = true
                scenarioContext.cameraIsActive = true // BDD: Camera is now active
                Log.d(TAG, "Camera provider initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Camera provider setup failed", e)
                thenUserExperiencesTechnicalError("Gagal memulai kamera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        if (!isAdded || isProcessingResult) {
            return
        }

        if (viewfinder.display == null) {
            Log.w(TAG, "Viewfinder display is null, cannot bind camera")
            return
        }

        cameraProvider.unbindAll()
        val preview = Preview.Builder()
            .setTargetRotation(viewfinder.display.rotation)
            .build()
        preview.setSurfaceProvider(viewfinder.surfaceProvider)

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(viewfinder.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            val camera = cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )

            try {
                val cameraControl = camera.cameraControl
                val cameraInfo = camera.cameraInfo
                val exposureState = cameraInfo.exposureState
                if (exposureState.isExposureCompensationSupported) {
                    val exposureCompensation = (exposureState.exposureCompensationRange.upper * 0.3).toInt()
                    cameraControl.setExposureCompensationIndex(exposureCompensation)
                    Log.d(TAG, "Exposure compensation set to: $exposureCompensation")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not set camera controls", e)
            }

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                if (!isAdded || isProcessingResult) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val mediaImage = imageProxy.image
                if (mediaImage != null && !isScanning) {
                    val image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    isScanning = true
                    Log.d(TAG, "Processing image for barcode, rotation: ${imageProxy.imageInfo.rotationDegrees}")

                    barcodeScanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            Log.d(TAG, "Barcodes found: ${barcodes.size}")
                            for (barcode in barcodes) {
                                val rawValue = barcode.rawValue
                                Log.d(TAG, "Raw barcode value: $rawValue")
                                if (!rawValue.isNullOrEmpty() && !isProcessingResult) {
                                    Log.d(TAG, "Barcode detected: $rawValue")
                                    isProcessingResult = true

                                    val activity = activity
                                    if (activity != null && !activity.isFinishing && isAdded) {
                                        activity.runOnUiThread {
                                            // BDD: WHEN - User melakukan scan terhadap QR code
                                            whenUserPointsCameraAndScansQRCode(rawValue.trim())
                                        }
                                    }
                                    break
                                }
                            }
                            isScanning = false
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Barcode scanning failed", e)
                            isScanning = false
                            thenUserExperiencesTechnicalError("Gagal memindai barcode: ${e.message}")
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
            thenUserExperiencesTechnicalError("Gagal mengikat kamera: ${exc.message}")
        }
    }

    private fun fetchDataFromFirestoreForValidation(serialNumber: String) {
        Log.d(TAG, "BDD - Fetching data for validation: $serialNumber")

        if (!isAdded || isProcessingResult.not()) {
            Log.d(TAG, "Fragment not attached or already processing, skipping Firestore fetch")
            return
        }

        firestore.collection("daftar_barang")
            .whereEqualTo("serial_number", serialNumber)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!isAdded || isProcessingResult.not()) {
                    Log.d(TAG, "Fragment no longer attached after Firestore fetch")
                    return@addOnSuccessListener
                }

                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    Log.d(TAG, "Document found via query: ${document.data}")
                    processValidationResult(document.data, serialNumber, true)
                } else {
                    // Fallback: coba dengan document ID
                    firestore.collection("daftar_barang")
                        .document(serialNumber)
                        .get()
                        .addOnSuccessListener fallbackListener@{ document ->
                            if (!isAdded || isProcessingResult.not()) return@fallbackListener

                            if (document.exists()) {
                                Log.d(TAG, "Document found via ID: ${document.data}")
                                processValidationResult(document.data, serialNumber, true)
                            } else {
                                Log.d(TAG, "No document found with serial: $serialNumber")
                                processValidationResult(null, serialNumber, false)
                            }
                        }
                        .addOnFailureListener fallbackFailure@{ e ->
                            if (!isAdded || isProcessingResult.not()) return@fallbackFailure
                            Log.e(TAG, "Firestore document query failed", e)
                            thenUserExperiencesTechnicalError("Gagal mengambil data: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded || isProcessingResult.not()) return@addOnFailureListener

                Log.e(TAG, "Firestore collection query failed", e)
                thenUserExperiencesTechnicalError("Gagal mengambil data: ${e.message}")
            }
    }

    private fun processValidationResult(data: Map<String, Any>?, serialNumber: String, isValid: Boolean) {
        scenarioContext.isValidQRCode = isValid

        if (isValid && data != null) {
            // Parse data menjadi ItemData
            val itemData = parseFirestoreDataToItemData(data, serialNumber)

            // BDD: THEN - Skenario 1: Item details are displayed
            thenItemDetailsAreDisplayed(itemData)
        } else {
            // BDD: THEN - Skenario 2: Error message and return to home
            thenUserSeesErrorMessageAndReturnsToHome()
        }
    }

    private fun parseFirestoreDataToItemData(data: Map<String, Any>, serialNumber: String): ItemData {
        val nama = data["nama_barang"] as? String
            ?: data["nama"] as? String
            ?: data["namaBarang"] as? String
            ?: data["name"] as? String
            ?: "Nama tidak tersedia"

        val tanggalMasuk = try {
            val timestamp = data["tanggal_masuk"] as? com.google.firebase.Timestamp
            if (timestamp != null) {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                sdf.format(timestamp.toDate())
            } else {
                data["tanggal_masuk"] as? String
                    ?: data["tanggalMasuk"] as? String
                    ?: "Tanggal tidak tersedia"
            }
        } catch (e: Exception) {
            "Tanggal tidak tersedia"
        }

        val jenis = data["jenis"] as? String
            ?: data["jenis_barang"] as? String
            ?: data["jenisBarang"] as? String
            ?: data["type"] as? String
            ?: "Jenis tidak tersedia"

        val pemilik = data["pemilik"] as? String
            ?: data["pemilik_barang"] as? String
            ?: data["pemilikBarang"] as? String
            ?: data["owner"] as? String
            ?: "Pemilik tidak tersedia"

        val letakBarang = data["letak_barang"] as? String
            ?: data["letakBarang"] as? String
            ?: data["lokasi"] as? String
            ?: data["location"] as? String
            ?: "Lokasi tidak tersedia"

        val serialNum = data["serial_number"] as? String
            ?: data["serialNumber"] as? String
            ?: serialNumber

        return ItemData(nama, tanggalMasuk, jenis, pemilik, letakBarang, serialNum)
    }

    private fun displayItemDetailsAndNavigate(itemData: ItemData) {
        try {
            scanResultListener?.onScanResult(
                itemData.nama,
                itemData.tanggalMasuk,
                itemData.jenisBarang,
                itemData.pemilikBarang,
                itemData.letakBarang,
                itemData.serialNumber
            )
            dismiss()

            val bundle = Bundle().apply {
                putString("arg_nama_barang", itemData.nama)
                putString("arg_tanggal_masuk", itemData.tanggalMasuk)
                putString("arg_jenis_barang", itemData.jenisBarang)
                putString("arg_pemilik_barang", itemData.pemilikBarang)
                putString("arg_letak_barang", itemData.letakBarang)
                putString("arg_serial_number", itemData.serialNumber)
            }

            val navController = requireActivity().findNavController(R.id.nav_home_fragment)
            navController.navigate(R.id.detailBarangFragment, bundle)

            Log.d(TAG, "BDD - Successfully navigated to item details for: ${itemData.nama}")

        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to DetailBarangFragment", e)
            thenUserExperiencesTechnicalError("Gagal membuka detail: ${e.message}")
            // Reset flags jika navigasi gagal
            isProcessingResult = false
            isScanning = false
        }
    }

    private fun showItemNotFoundMessageAndNavigateToHome() {
        if (!isAdded) return

        activity?.runOnUiThread {
            isProcessingResult = true

            val snackbar = Snackbar.make(
                requireView(),
                "⚠️ Barang Tidak Ditemukan",
                Snackbar.LENGTH_LONG
            )
            snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.red))
            snackbar.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

            snackbar.show()

            Handler(Looper.getMainLooper()).postDelayed({
                navigateToHome()
            }, 2000)
        }
    }

    private fun showTechnicalErrorMessage(message: String?) {
        isProcessingResult = true

        Toast.makeText(
            requireContext(),
            "Gagal melakukan scan: ${message ?: "Unknown error"}",
            Toast.LENGTH_SHORT
        ).show()

        Handler(Looper.getMainLooper()).postDelayed({
            navigateToHome()
        }, 2000)
    }

    private fun navigateToHome() {
        if (!isAdded) return

        try {
            findNavController().navigate(R.id.action_scanFragment_to_homeFragment)
            Log.d(TAG, "BDD - Successfully navigated back to home")
        } catch (e: Exception) {
            Log.e(TAG, "Navigation failed", e)
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Fragment resumed")

        if (!isCameraSetup && !isProcessingResult && isAdded &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED) {

            viewfinder.post {
                if (viewfinder.display != null) {
                    setupCamera()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Fragment paused")
        isScanning = false
        scenarioContext.cameraIsActive = false // BDD: Camera is no longer active
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::cameraProviderFuture.isInitialized) {
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                Log.e(TAG, "Error unbinding camera", e)
            }
        }
        // Reset flags and BDD context
        isProcessingResult = false
        isCameraSetup = false
        isScanning = false
        scenarioContext.cameraIsActive = false
        scenarioContext.userIsAtScanPage = false
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Fragment destroyed, releasing resources")
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }

    companion object {
        private const val TAG = "ScanFragment"

        fun show(fragmentManager: FragmentManager, listener: ScanResultListener? = null): ScanFragment {
            val scanFragment = ScanFragment()
            listener?.let { scanFragment.setScanResultListener(it) }
            scanFragment.show(fragmentManager, "ScanFragment")
            return scanFragment
        }
    }
}