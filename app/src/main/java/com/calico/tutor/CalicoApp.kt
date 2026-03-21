package com.calico.tutor

import android.app.Application
import com.calico.tutor.di.ServiceLocator
import java.io.PrintWriter
import java.io.StringWriter

class CalicoApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        val telemetryRepository = ServiceLocator.telemetryRepository(applicationContext)

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // Convert stack trace to string
                val stringWriter = StringWriter()
                throwable.printStackTrace(PrintWriter(stringWriter))
                val stackTrace = stringWriter.toString()

                // Send crash report to backend
                telemetryRepository.reportCrash(stackTrace)

                // Give it a moment to send (blocking call)
                Thread.sleep(1000)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Call the default handler to crash the app normally
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
