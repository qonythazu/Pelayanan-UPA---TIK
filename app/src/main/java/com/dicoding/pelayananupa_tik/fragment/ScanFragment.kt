package com.dicoding.pelayananupa_tik.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.dicoding.pelayananupa_tik.R
import com.dicoding.pelayananupa_tik.utils.ProgressDialogFragment
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
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
        // Pastikan fragment masih terhubung dengan activity
        if (!isAdded) {
            Log.d(TAG, "Fragment not attached to activity, skipping camera setup")
            return
        }

        Log.d(TAG, "Binding camera use cases")
        cameraProvider.unbindAll()

        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(viewfinder.surfaceProvider)

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            // Bind lifecycles menggunakan viewLifecycleOwner
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                imageAnalysis,
                preview
            )

            // Definisikan analyzer untuk pemindaian barcode
            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                // Periksa apakah fragment masih terpasang ke activity
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
                                if (rawValue != null) {
                                    Log.d(TAG, "Barcode detected: $rawValue")
                                    val valueType = barcode.valueType
                                    Log.d(TAG, "Barcode type: $valueType")
                                    // Gunakan activity yang dikontrol terlebih dahulu
                                    val activity = activity
                                    if (activity != null && !activity.isFinishing && isAdded) {
                                        activity.runOnUiThread {
                                            showLoading()
                                            fetchDataFromFirestore(rawValue)
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
        }
    }

    private fun fetchDataFromFirestore(serialNumber: String) {
        Log.d(TAG, "Fetching data for serial: $serialNumber")

        // Periksa lagi apakah fragment masih terpasang
        if (!isAdded) {
            Log.d(TAG, "Fragment not attached, skipping Firestore fetch")
            hideLoading()
            return
        }

        firestore.collection("daftar_barang")
            .document(serialNumber)
            .get()
            .addOnSuccessListener { document ->
                // Pastikan loading berhenti terlepas dari hasil
                hideLoading()

                // Periksa lagi apakah fragment masih terpasang
                if (!isAdded) {
                    Log.d(TAG, "Fragment no longer attached after Firestore fetch")
                    return@addOnSuccessListener
                }

                if (document.exists()) {
                    Log.d(TAG, "Document found: ${document.data}")

                    // Extract data with fallbacks
                    val nama = document.getString("nama_barang") ?: document.getString("nama") ?: "-"
                    val tanggalMasuk = document.getTimestamp("tanggal_masuk")?.toDate()?.toString() ?: "-"
                    val jenis = document.getString("jenis") ?: "-"
                    val pemilik = document.getString("pemilik") ?: "-"
                    val letakBarang = document.getString("letak_barang") ?: "-"
                    val serialNum = document.getString("serial_number") ?: serialNumber

                    Log.d(TAG, "Data extracted: $nama, $jenis, $serialNum")

                    // Perbaikan: Navigasi dengan NavController
                    try {
                        // Tutup scanner dialog terlebih dahulu
                        dismiss()

                        // Buat bundle data
                        val bundle = Bundle().apply {
                            putString("nama_barang", nama)
                            putString("tanggal_masuk", tanggalMasuk)
                            putString("jenis_barang", jenis)
                            putString("pemilik_barang", pemilik)
                            putString("letak_barang", letakBarang)
                            putString("serial_number", serialNum)
                        }

                        // Gunakan navController dari activity
                        val navController = requireActivity().findNavController(R.id.nav_home_fragment) // Sesuaikan dengan ID NavHostFragment Anda
                        navController.navigate(R.id.detailBarangFragment, bundle)

                        // Log untuk debugging
                        Log.d(TAG, "Navigation attempted to DetailBarangFragment with data")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error navigating to DetailBarangFragment", e)
                        Toast.makeText(
                            requireContext(),
                            "Gagal membuka detail: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.d(TAG, "No document found with serial: $serialNumber")
                    Toast.makeText(
                        requireContext(),
                        "Data barang dengan kode $serialNumber tidak ditemukan",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                // Pastikan loading berhenti
                hideLoading()

                // Periksa apakah fragment masih terpasang
                if (!isAdded) {
                    return@addOnFailureListener
                }

                Log.e(TAG, "Firestore query failed", e)
                Toast.makeText(
                    requireContext(),
                    "Gagal mengambil data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showLoading() {
        if (!isAdded) return

        progressDialog = ProgressDialogFragment()
        val fm = parentFragmentManager
        if (!fm.isDestroyed && !fm.isStateSaved) {
            progressDialog?.show(fm, "loading")
            Log.d(TAG, "Loading dialog shown")
        }
    }

    private fun hideLoading() {
        try {
            if (progressDialog != null && progressDialog?.isAdded == true) {
                progressDialog?.dismissAllowingStateLoss()
                progressDialog = null
            }
            Log.d(TAG, "Loading dialog hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding loading dialog", e)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Fragment paused")
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
                Log.e(TAG, "Failed to rebind camera uses cases on resume", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Pastikan untuk melepaskan resources kamera
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

        fun show(fragmentManager: FragmentManager, listener: ScanResultListener): ScanFragment {
            val scanFragment = ScanFragment()
            scanFragment.setScanResultListener(listener)
            scanFragment.show(fragmentManager, "ScanFragment")
            return scanFragment
        }
    }
}