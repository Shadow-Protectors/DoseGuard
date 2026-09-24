package com.doseguard.app

import android.app.Application

/**
 * DoseGuardApp — Application class.
 * Declared in AndroidManifest so Room initializes on first access
 * with the correct application context.
 */
class DoseGuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Room database is initialized lazily on first getDatabase() call.
        // Nothing to do here except be the Context provider.
    }
}
