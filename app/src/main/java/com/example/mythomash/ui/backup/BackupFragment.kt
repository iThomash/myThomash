package com.example.mythomash.ui.backup

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mythomash.CronetClient
import com.example.mythomash.databinding.FragmentBackupBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BackupFragment : Fragment() {

    private lateinit var binding: FragmentBackupBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture
    private var selectedImageBytes: ByteArray? = null
    private lateinit var cronetClient: CronetClient
    private lateinit var viewModel: BackupViewModel
    private var isCameraStarted = false

    // Separate permission launchers
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pickImageFromGallery()
        } else {
            Toast.makeText(requireContext(), "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    requireActivity().contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        viewModel.setImage(bitmap)
                        selectedImageBytes = bitmapToBytes(bitmap)
                        binding.previewView.visibility = View.GONE
                        binding.imagePreview.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    Log.e("Gallery", "Error loading image", e)
                    Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        viewModel = ViewModelProvider(this)[BackupViewModel::class.java]
        viewModel.selectedImage.observe(viewLifecycleOwner) { bitmap ->
            binding.imagePreview.setImageBitmap(bitmap)
        }

        viewModel.uploadStatus.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }

        cronetClient = CronetClient(requireContext())
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.buttonCaptureMedia.setOnClickListener {
            if (!isCameraStarted) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startCamera()
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            } else {
                takePhoto()
            }
        }

        binding.buttonSelectFile.setOnClickListener {
            if (hasStoragePermission()) {
                pickImageFromGallery()
            } else {
                storagePermissionLauncher.launch(getStoragePermission())
            }
        }

        binding.buttonUpload.setOnClickListener {
            selectedImageBytes?.let {
                uploadImage(it)
            } ?: Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            getStoragePermission()
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getStoragePermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                imageCapture = ImageCapture.Builder().build()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture
                )

                isCameraStarted = true
                binding.previewView.visibility = View.VISIBLE
                binding.imagePreview.visibility = View.GONE
                Toast.makeText(requireContext(), "Camera ready - tap to capture", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("CameraX", "Use case binding failed", e)
                Toast.makeText(requireContext(), "Camera failed to start", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        if (!isCameraStarted) return

        val photoFile = File.createTempFile(
            "temp_image",
            ".jpg",
            requireContext().cacheDir
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                        viewModel.setImage(bitmap)
                        selectedImageBytes = bitmapToBytes(bitmap)
                        binding.previewView.visibility = View.GONE
                        binding.imagePreview.visibility = View.VISIBLE
                        stopCamera()
                        Toast.makeText(requireContext(), "Photo captured", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("CameraX", "Error saving photo", e)
                        Toast.makeText(requireContext(), "Error saving photo", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
                    Toast.makeText(requireContext(), "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun stopCamera() {
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
                isCameraStarted = false
            }, ContextCompat.getMainExecutor(requireContext()))
        } catch (e: Exception) {
            Log.e("CameraX", "Error stopping camera", e)
        }
    }

    private fun pickImageFromGallery() {
        // Create an explicit intent for image selection
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        // Launch the intent
        galleryLauncher.launch(intent)
    }

    private fun bitmapToBytes(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return stream.toByteArray()
    }

    private fun uploadImage(bytes: ByteArray) {
        val encodedImage = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val serverUrl = "http://192.168.1.66:3000/upload"

        cronetClient.postImageData(
            serverUrl,
            encodedImage,
            object : CronetClient.ResponseCallback {
                override fun onResponse(response: String) {
                    Log.d("Upload", "Server response: $response")
                    viewModel.setUploadStatus("Upload successful")
                }

                override fun onError(error: String) {
                    Log.e("Upload", "Upload failed: $error")
                    viewModel.setUploadStatus("Upload failed: $error")
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        if (isCameraStarted) {
            stopCamera()
        }
    }
}