
package com.slawomirkedra.qrmusic

import android.content.Context
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote

class SpotifyController(private val context: Context) {
    private var appRemote: SpotifyAppRemote? = null

    fun connect(onReady: () -> Unit, onFail: (Throwable) -> Unit) {
        val params = ConnectionParams.Builder("b6ea95f0db0843a7bf3bad35ab78d417")
            .setRedirectUri("qrmusicapp://callback")
            .showAuthView(true)
            .build()
        SpotifyAppRemote.connect(context, params, object : Connector.ConnectionListener {
            override fun onConnected(remote: SpotifyAppRemote) {
                appRemote = remote; onReady()
            }
            override fun onFailure(t: Throwable) { onFail(t) }
        })
    }

    fun play(uri: String) {
        appRemote?.playerApi?.play(uri)
    }

    fun disconnect() {
        appRemote?.let { SpotifyAppRemote.disconnect(it) }
        appRemote = null
    }
}
