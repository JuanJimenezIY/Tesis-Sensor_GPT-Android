package com.jimenez.ecuafit.ui.utilities

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log

class ServiceReminder : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        // Ejecuta tu acción aquí, por ejemplo, mostrar una notificación o realizar una tarea
        Log.d("MidnightJobService", "Acción programada ejecutada a medianoche")
        saveValueToSharedPreferences()
        // Importante: Si tu tarea es asíncrona, asegúrate de llamar a jobFinished
        jobFinished(params, false)
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        // Si se cancela la tarea antes de completarse, regresa true para reprogramarla
        return true
    }
    private fun saveValueToSharedPreferences() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("agua", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putInt("agua", 0)
        editor.apply()
        Log.d("MidnightJobService", "Valor guardado en SharedPrefs")
    }
}
