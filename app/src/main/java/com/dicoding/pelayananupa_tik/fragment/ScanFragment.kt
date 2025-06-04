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
import com.dicoding.pelayananupa_tik.utils.ProgressDialogFragment
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

    private lateinit var viewfinder: androidx.camera.view.PreviewView
    private lateinit var overlay: View
    private lateinit var scanLine: View
    private lateinit var btnCancel: Button
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private var isScanning = false
    private var progressDialog: ProgressDialogFragment? = null
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
        Log.d(TAG, "Setting up camera")
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(cameraProvider)
                Log.d(TAG, "Camera provider initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Camera provider setup failed", e)
                Toast.makeText(
                    requireContext(),
                    "Gagal memulai kamera: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        if (!isAdded) {
            Log.d(TAG, "Fragment not attached to activity, skipping camera setup")
            return
        }

        Log.d(TAG, "Binding camera use cases")
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
                if (!isAdded) {
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
                                if (!rawValue.isNullOrEmpty()) {
                                    Log.d(TAG, "Barcode detected: $rawValue")

                                    val activity = activity
                                    if (activity != null && !activity.isFinishing && isAdded) {
                                        activity.runOnUiThread {
                                            showLoading()
                                            fetchDataFromFirestore(rawValue.trim())
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
            Toast.makeText(requireContext(), "Gagal mengikat kamera: ${exc.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchDataFromFirestore(serialNumber: String) {
        Log.d(TAG, "Fetching data for serial: $serialNumber")

        if (!isAdded) {
            Log.d(TAG, "Fragment not attached, skipping Firestore fetch")
            hideLoading()
            return
        }

        firestore.collection("daftar_barang")
            .whereEqualTo("serial_number", serialNumber)
            .get()
            .addOnSuccessListener { querySnapshot ->
                hideLoading()

                if (!isAdded) {
                    Log.d(TAG, "Fragment no longer attached after Firestore fetch")
                    return@addOnSuccessListener
                }

                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    Log.d(TAG, "Document found via query: ${document.data}")
                    processDocumentData(document.data, serialNumber)
                } else {
                    // Fallback: coba dengan document ID
                    firestore.collection("daftar_barang")
                        .document(serialNumber)
                        .get()
                        .addOnSuccessListener fallbackListener@{ document ->
                            hideLoading()
                            if (!isAdded) return@fallbackListener

                            if (document.exists()) {
                                Log.d(TAG, "Document found via ID: ${document.data}")
                                processDocumentData(document.data, serialNumber)
                            } else {
                                Log.d(TAG, "No document found with serial: $serialNumber")
                                showNotFoundMessage()
                            }
                        }
                        .addOnFailureListener fallbackFailure@{ e ->
                            hideLoading()
                            if (!isAdded) return@fallbackFailure
                            Log.e(TAG, "Firestore document query failed", e)
                            showErrorMessage(e.message)
                        }
                }
            }
            .addOnFailureListener { e ->
                hideLoading()
                if (!isAdded) return@addOnFailureListener

                Log.e(TAG, "Firestore collection query failed", e)
                showErrorMessage(e.message)
            }
    }

    private fun processDocumentData(data: Map<String, Any>?, serialNumber: String) {
        if (data == null) {
            showNotFoundMessage()
            return
        }
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

        try {
            scanResultListener?.onScanResult(
                nama, tanggalMasuk, jenis, pemilik, letakBarang, serialNum
            )
            dismiss()
            val bundle = Bundle().apply {
                putString("arg_nama_barang", nama)
                putString("arg_tanggal_masuk", tanggalMasuk)
                putString("arg_jenis_barang", jenis)
                putString("arg_pemilik_barang", pemilik)
                putString("arg_letak_barang", letakBarang)
                putString("arg_serial_number", serialNum)
            }
            val navController = requireActivity().findNavController(R.id.nav_home_fragment)
            navController.navigate(R.id.detailBarangFragment, bundle)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to DetailBarangFragment", e)
            Toast.makeText(
                requireContext(),
                "Gagal membuka detail: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showNotFoundMessage() {
        if (!isAdded) return

        activity?.runOnUiThread {
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

    private fun navigateToHome() {
        if (!isAdded) return

        try {
            findNavController().navigate(R.id.action_scanFragment_to_homeFragment)
        } catch (e: Exception) {
            Log.e(TAG, "Navigation failed", e)
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun showErrorMessage(message: String?) {
        Toast.makeText(
            requireContext(),
            "Gagal mengambil data: ${message ?: "Unknown error"}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showLoading() {
        if (!isAdded) return

        try {
            progressDialog = ProgressDialogFragment()
            val fm = parentFragmentManager
            if (!fm.isDestroyed && !fm.isStateSaved) {
                progressDialog?.show(fm, "loading")
                Log.d(TAG, "Loading dialog shown")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing loading dialog", e)
        }
    }

    private fun hideLoading() {
        try {
            progressDialog?.let { dialog ->
                if (dialog.isAdded) {
                    dialog.dismissAllowingStateLoss()
                }
            }
            progressDialog = null
            Log.d(TAG, "Loading dialog hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding loading dialog", e)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Fragment resumed")
        if (::cameraProviderFuture.isInitialized && isAdded &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED) {
            try {
                val cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(cameraProvider)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to rebind camera use cases on resume", e)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Fragment paused")
        isScanning = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Release camera resources
        if (::cameraProviderFuture.isInitialized) {
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                Log.e(TAG, "Error unbinding camera", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Fragment destroyed, releasing resources")
        hideLoading()
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