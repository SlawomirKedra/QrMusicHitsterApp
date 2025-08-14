
package com.slawomirkedra.qrmusic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class ScannerFragment : Fragment() {
    private lateinit var previewView: PreviewView
    private var cameraProvider: ProcessCameraProvider? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_scanner, container, false)
        previewView = root.findViewById(R.id.previewView)
        startCamera()
        return root
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(requireContext())
        providerFuture.addListener({
            cameraProvider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val analyzer = ImageAnalysis.Builder().build().also { ia ->
                ia.setAnalyzer(Executors.newSingleThreadExecutor()) { img -> processFrame(img) }
            }

            val sel = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(viewLifecycleOwner, sel, preview, analyzer)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun processFrame(imageProxy: ImageProxy) {
        val media = imageProxy.image ?: run { imageProxy.close(); return }
        val image = InputImage.fromMediaImage(media, imageProxy.imageInfo.rotationDegrees)
        val opts = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        val scanner = BarcodeScanning.getClient(opts)
        scanner.process(image)
            .addOnSuccessListener { list ->
                val txt = list.firstOrNull()?.rawValue
                if (txt != null) {
                    imageProxy.close()
                    (activity as? MainActivity)?.onQrScanned(txt)
                    cameraProvider?.unbindAll()
                } else imageProxy.close()
            }
            .addOnFailureListener { imageProxy.close() }
    }
}
