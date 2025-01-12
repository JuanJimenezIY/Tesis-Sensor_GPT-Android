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
    private var agua = 0
    private var vaso = 0

    companion object {
        const val CHANNEL_ID = "ecuafit_channel_id"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAguaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannel()

        val sharedPref = getSharedPreferences("agua", Context.MODE_PRIVATE)
        vaso = sharedPref.getInt("agua", agua)
        Log.d("UCE", vaso.toString())
        binding.totalAgua.text = (vaso * 250).toString() + " ml"
        binding.totalVasos.text = "$vaso"
        binding.botonMas.setOnClickListener { agregarAgua() }
        binding.botonMenos.setOnClickListener { restarAgua() }

        if (vaso < 8) {
            scheduleNotification(100)
        } else {
            cancelScheduledNotification()
        }
        scheduleMidnightJob()

        obtenerPasosYRitmoCardiaco()
        obtenerDuracionSueno()

    }

    fun agregarAgua() {
        vaso += 1
        actualizarAgua()
    }

    fun restarAgua() {
        if (vaso >= 1) {
            vaso -= 1
        }
        actualizarAgua()
    }

    private fun obtenerPasosYRitmoCardiaco() {
        lifecycleScope.launch(Dispatchers.Main) {
            val totalSteps = readTotalSteps()
            binding.pasos.text = "Pasos: $totalSteps/ 8000"
            setProgressBarWithAnimation(binding.progressBar, totalSteps.toInt()) // Actualizar la barra de progreso con animación
            binding.pasosP.text = "${(totalSteps.toDouble() / binding.progressBar.max * 100).toInt()}%"

            // Mostrar notificación de éxito si se alcanzan los 8000 pasos
            mostrarNotificacionExito(totalSteps)

            val heartRate = readLastHeartRate()
            binding.ritmoCardiaco.text = "Ritmo Cardíaco: $heartRate bpm"
        }
    }
    private fun obtenerDuracionSueno() {
        lifecycleScope.launch(Dispatchers.Main) {
            val duracionSueno = readLastSleepDuration()
            binding.duracionSueno.text = "Duración del Sueño: $duracionSueno horas"
        }
    }
    private suspend fun readLastSleepDuration(): Double = withContext(Dispatchers.IO) {
        try {
            val healthConnectClient = HealthConnectClient.getOrCreate(this@AguaActivity)
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.before(Instant.now())
                )
            )
            val lastSleepSession = response.records.maxByOrNull { it.endTime }
            val durationInHours = lastSleepSession?.let {
                val durationInMillis = it.endTime.toEpochMilli() - it.startTime.toEpochMilli()
                durationInMillis / (1000 * 60 * 60).toDouble()
            } ?: 0.0
            Log.d("HealthConnect", "Duración del Sueño obtenida: $durationInHours horas")
            durationInHours
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error al leer la duración del sueño: ${e.message}", e)
            0.0
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
                    timeRangeFilter = TimeRangeFilter.before(Instant.now()) // Todos los ritmos cardíacos hasta el momento actual
                )
            )
            val heartRates = response.records.map { it.samples }.flatten()
            val lastHeartRate = if (heartRates.isNotEmpty()) {
                heartRates.last().beatsPerMinute
            } else {
                0.0
            }
            Log.d("HealthConnect", "Último Ritmo Cardíaco obtenido: $lastHeartRate bpm")
            lastHeartRate
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error al leer ritmo cardíaco: ${e.message}", e)
            0.0
        }
    }

    private fun setProgressBarWithAnimation(progressBar: ProgressBar, progressTo: Int) {
        val animation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, progressTo)
        animation.duration = 500 // Duración de la animación en milisegundos
        animation.interpolator = DecelerateInterpolator()
        animation.start()
    }

    private fun mostrarNotificacionExito(pasos: Long) {
        if (pasos >= 8000) {
            val intent = Intent(this, AguaActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.fitnes_logo) // Cambia esto por el ícono de tu aplicación
                .setContentTitle("¡Éxito!")
                .setContentText("Has alcanzado los 8000 pasos. ¡Sigue así!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)


        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Ecuafit"
            val descriptionText = "Notificaciones de Ecuafit"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun actualizarAgua() {
        binding.totalAgua.text = (vaso * 250).toString() + " ml"
        binding.totalVasos.text = "$vaso"
        val sharedPref = getSharedPreferences("agua", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("agua", vaso).apply()
        }
    }

    private fun scheduleNotification(intervalMillis: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationIntent = Intent(this, BroadcasterNotifications::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val currentTime = System.currentTimeMillis()
        val triggerTime = currentTime + intervalMillis
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            intervalMillis,
            pendingIntent
        )
    }

    private fun scheduleMidnightJob() {
        val componentName = ComponentName(this, ServiceReminder::class.java)
        val jobInfo = JobInfo.Builder(1, componentName)
            .setRequiresDeviceIdle(false)
            .setRequiresCharging(false)
            .setPeriodic(AlarmManager.INTERVAL_DAY) // Repetir cada día
            .build()
        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(jobInfo)
    }

    private fun cancelScheduledNotification() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationIntent = Intent(this, BroadcasterNotifications::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
    }

    // Clase interna para recibir las actualizaciones de pasos


    inner class StepUpdateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val steps = intent.getIntExtra("steps", 0)
            binding.pasos.text = "Pasos: $steps"
            binding.progressBar.progress = steps // Actualizar la barra de progreso
            Log.d("WearConnection", "Pasos recibidos: $steps")
        }
    }
}
