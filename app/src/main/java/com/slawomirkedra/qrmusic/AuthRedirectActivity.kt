package com.slawomirkedra.qrmusic

import android.app.Activity
import android.os.Bundle

// Pusta aktywność do redirectu (zamyka się od razu).
class AuthRedirectActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}