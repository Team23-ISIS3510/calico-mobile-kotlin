package com.calico.tutor

import android.app.Application
import com.calico.tutor.data.worker.PendingAvailabilitiesWorker

class CalicoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Programar sincronización periódica de disponibilidades pendientes (Eventual Connectivity)
        PendingAvailabilitiesWorker.schedulePeriodicWork(this)
    }
}
