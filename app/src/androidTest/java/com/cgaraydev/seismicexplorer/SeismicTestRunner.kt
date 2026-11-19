package com.cgaraydev.seismicexplorer

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication
import org.maplibre.android.MapLibre

class SeismicTestRunner : AndroidJUnitRunner() {

    /**
     * HiltTestApplication no ejecuta SeismicExplorerApplication.onCreate().
     * MapLibre debe inicializarse en el hilo principal antes de los tests.
     */
    override fun onStart() {
        runOnMainSync {
            MapLibre.getInstance(targetContext.applicationContext)
        }
        super.onStart()
    }

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(
            cl,
            HiltTestApplication::class.java.name,
            context
        )
    }
}