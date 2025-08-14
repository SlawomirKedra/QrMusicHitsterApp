package com.slawomirkedra.qrmusic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var preview: PreviewView
    private lateinit var statusText: TextView
    private lateinit var btnPlayLast: Button

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var lastUrl: String? = null
    private val spotify = SpotifyController(this, "b6ea95f0db0843a7bf3bad35ab78d417", "qrmusicapp://callback")

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else status("Brak uprawnień do kamery.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preview = findViewById(R.id.previewView)
        statusText = findViewById(R.id.statusText)
        btnPlayLast = findViewById(R.id.btnPlayLast)
        btnPlayLast.setOnClickListener { lastUrl?.let { handleScanned(it, fromButton = true) } }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermission.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onStart() {
        super.onStart()
        spotify.ensureConnected()
    }

    override fun onStop() {
        super.onStop()
        spotify.disconnect()
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val previewUse = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(preview.surfaceProvider)
            }
            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            val analysis = ImageAnalysis.Builder().build().apply {
                setAnalyzer(cameraExecutor) { imgProxy ->
                    val mediaImage = imgProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imgProxy.imageInfo.rotationDegrees)
                        BarcodeScanning.getClient().process(image)
                            .addOnSuccessListener { barcodes ->
                                for (b in barcodes) {
                                    val url = b.url?.url ?: b.rawValue
                                    if (!url.isNullOrEmpty()) {
                                        imgProxy.close()
                                        runOnUiThread {
                                            handleScanned(url)
                                        }
                                        return@addOnSuccessListener
                                    }
                                }
                                imgProxy.close()
                            }
                            .addOnFailureListener { imgProxy.close() }
                    } else imgProxy.close()
                }
            }
            provider.unbindAll()
            provider.bindToLifecycle(this, selector, previewUse, analysis)
            status("Skanuję QR…")
        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleScanned(url: String, fromButton: Boolean = false) {
        lastUrl = url
        btnPlayLast.visibility = View.VISIBLE
        when {
            isSpotify(url) -> {
                val trackId = extractSpotifyTrackId(url)
                if (trackId != null) {
                    status("Spotify: odtwarzam utwór…")
                    spotify.playTrack(trackId) { ok, msg ->
                        runOnUiThread {
                            if (!ok) { status("Błąd Spotify: ${msg}"); snack("Błąd Spotify: ${msg}") }
                            else status("Odtwarzanie…")
                        }
                    }
                } else {
                    status("Nie rozpoznano ID utworu w linku Spotify.")
                }
            }
            isYouTube(url) -> {
                status("YouTube: otwieram…")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                try { startActivity(intent) } catch (e: Exception) { snack("Brak aplikacji do odtworzenia YouTube.") }
            }
            else -> status("Zeskanowano: $url")
        }
    }

    private fun isSpotify(url: String) =
        url.contains("open.spotify.com/track") || url.startsWith("spotify:track:")

    private fun extractSpotifyTrackId(url: String): String? {
        return when {
            url.startsWith("spotify:track:") -> url.removePrefix("spotify:track:")
            url.contains("open.spotify.com/track/") -> url.substringAfter("open.spotify.com/track/").takeWhile { it.isLetterOrDigit() }
            else -> null
        }
    }

    private fun isYouTube(url: String) = url.contains("youtu.be/") || url.contains("youtube.com/")

    private fun status(s: String) { statusText.text = s }
    private fun snack(s: String) { Snackbar.make(statusText, s, Snackbar.LENGTH_LONG).show() }
}