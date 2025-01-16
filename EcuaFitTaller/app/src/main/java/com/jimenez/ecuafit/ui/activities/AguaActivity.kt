package com.jimenez.ecuafit.ui.activities

import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import android.animation.ObjectAnimator
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.jimenez.ecuafit.R
import com.jimenez.ecuafit.databinding.ActivityAguaBinding
import com.jimenez.ecuafit.ui.utilities.BroadcasterNotifications
import com.jimenez.ecuafit.ui.utilities.ServiceReminder
import kotlinx.coroutines.launch

class AguaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAguaBinding


    companion object {
        const val CHANNEL_ID = "ecuafit_channel_id"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAguaBinding.inflate(layoutInflater)
        setContentView(binding.root)







        obtenerPasosYRitmoCardiaco()
        obtenerDuracionSueno()

    }


    private fun obtenerPasosYRitmoCardiaco() {
        lifecycleScope.launch(Dispatchers.Main) {
            val totalSteps = readTotalSteps()
            binding.pasos.text = "Pasos: $totalSteps/ 8000"
            setProgressBarWithAnimation(binding.progressBar, totalSteps.toInt()) // Actualizar la barra de progreso con animación
            binding.pasosP.text = "${(totalSteps.toDouble() / binding.progressBar.max * 100).toInt()}%"


            val heartRate = readLastHeartRate()
            binding.ritmoCardiaco.text = "Ritmo Cardíaco: $heartRate bpm"
        }
    }
    private fun obtenerDuracionSueno() {
        lifecycleScope.launch(Dispatchers.Main) {
            val (horas, minutos) = readLastSleepDuration()
            binding.duracionSueno.text = "Duración del Sueño: $horas horas y $minutos minutos"
        }
    }

    private suspend fun readLastSleepDuration(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        try {
            val healthConnectClient = HealthConnectClient.getOrCreate(this@AguaActivity)
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.before(Instant.now())
                )
            )
            val lastSleepSession = response.records.maxByOrNull { it.endTime }
            val durationInMillis = lastSleepSession?.let {
                it.endTime.toEpochMilli() - it.startTime.toEpochMilli()
            } ?: 0L
            val durationInHours = (durationInMillis / (1000 * 60 * 60)).toInt()
            val durationInMinutes = ((durationInMillis / (1000 * 60)) % 60).toInt()

            Log.d("HealthConnect", "Duración del Sueño obtenida: $durationInHours horas y $durationInMinutes minutos")
            durationInHours to durationInMinutes
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error al leer la duración del sueño: ${e.message}", e)
            0 to 0
        }
    }

    private suspend fun readTotalSteps(): Long = withContext(Dispatchers.IO) {
        try {
            val healthConnectClient = HealthConnectClient.getOrCreate(this@AguaActivity)
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, Instant.now())
                )
            )
            val totalSteps = response.records.sumOf { it.count }
            Log.d("HealthConnect", "Pasos Totales obtenidos: $totalSteps")
            totalSteps
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error al leer pasos totales: ${e.message}", e)
            0
        }
    }

    private suspend fun readLastHeartRate(): Number = withContext(Dispatchers.IO) {
        try {
            val healthConnectClient = HealthConnectClient.getOrCreate(this@AguaActivity)
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.before(Instant.now())
                )
            )
            val heartRates = response.records.flatMap { it.samples }
            val lastHeartRate = heartRates.lastOrNull()?.beatsPerMinute ?: 0.0
            Log.d("HealthConnect", "Último Ritmo Cardíaco obtenido: $lastHeartRate bpm")
            lastHeartRate
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error al leer ritmo cardíaco: ${e.message}", e)
            0.0
        }
    }

    private fun setProgressBarWithAnimation(progressBar: ProgressBar, progressTo: Int) {
        val animation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, progressTo)
        animation.duration = 5000 // Duración de la animación en milisegundos
        animation.interpolator = DecelerateInterpolator()
        animation.start()
    }




    inner class StepUpdateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val steps = intent.getIntExtra("steps", 0)
            binding.pasos.text = "Pasos: $steps"
            binding.progressBar.progress = steps // Actualizar la barra de progreso
            Log.d("WearConnection", "Pasos recibidos: $steps")
        }
    }
}
