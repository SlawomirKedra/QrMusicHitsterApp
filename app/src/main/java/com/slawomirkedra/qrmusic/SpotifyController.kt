package com.slawomirkedra.qrmusic

import android.content.Context
import android.util.Log
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote

class SpotifyController(private val context: Context, private val clientId: String, private val redirectUri: String) {

    private var appRemote: SpotifyAppRemote? = null

    fun ensureConnected(onResult: ((Boolean, String?) -> Unit)? = null) {
        if (appRemote != null && appRemote!!.isConnected) {
            onResult?.invoke(true, null); return
        }
        val params = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()
        SpotifyAppRemote.connect(context, params, object : Connector.ConnectionListener {
            override fun onConnected(remote: SpotifyAppRemote) {
                appRemote = remote
                onResult?.invoke(true, null)
            }
            override fun onFailure(throwable: Throwable) {
                Log.e("Spotify", "Connect fail", throwable)
                onResult?.invoke(false, throwable.message)
            }
        })
    }

    fun playTrack(trackId: String, cb: (Boolean, String?) -> Unit) {
        ensureConnected { ok, msg ->
            if (!ok) { cb(false, msg); return@ensureConnected }
            val uri = "spotify:track:$trackId"
            try {
                appRemote?.playerApi?.play(uri)?.setResultCallback {
                    cb(true, null)
                }?.setErrorCallback {
                    cb(false, it.message)
                }
            } catch (e: Exception) {
                cb(false, e.message)
            }
        }
    }

    fun disconnect() {
        appRemote?.let {
            try { SpotifyAppRemote.disconnect(it) } catch (_: Exception) {}
        }
        appRemote = null
    }
}