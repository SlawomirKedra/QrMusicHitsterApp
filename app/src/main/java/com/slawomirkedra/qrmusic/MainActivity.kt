
package com.slawomirkedra.qrmusic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit

class MainActivity : AppCompatActivity() {
    private val spotify = SpotifyController(this)

    private val requestCamera = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showScanner() else Toast.makeText(this, "Potrzebna kamera", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ensureCameraPermission()
    }

    private fun ensureCameraPermission() {
        val ok = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (ok) showScanner() else requestCamera.launch(Manifest.permission.CAMERA)
    }

    private fun showScanner() {
        supportFragmentManager.commit {
            replace(R.id.container, ScannerFragment())
        }
    }

    fun onQrScanned(text: String) {
        val p = parseLink(text)
        if (p.type == "spotify") {
            val subtype = p.subtype ?: "track"
            val id = p.id
            if (id != null) {
                val uri = "spotify:${subtype}:${id}"
                Toast.makeText(this, "Odtwarzam: ${uri}", Toast.LENGTH_SHORT).show()
                spotify.connect(
                    onReady = { spotify.play(uri) },
                    onFail = { Toast.makeText(this, "Nie udało się połączyć ze Spotify", Toast.LENGTH_LONG).show() }
                )
            } else Toast.makeText(this, "Nieznany kod Spotify", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Nieobsługiwany kod", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        spotify.disconnect()
        super.onDestroy()
    }
}
