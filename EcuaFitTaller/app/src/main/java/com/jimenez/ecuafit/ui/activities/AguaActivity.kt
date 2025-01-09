package com.jimenez.ecuafit.ui.activities

import androidx.activity.result.contract.ActivityResultContracts
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
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.type.TimeOfDay
import com.jimenez.ecuafit.databinding.ActivityAguaBinding
import com.jimenez.ecuafit.ui.utilities.BroadcasterNotifications
import com.jimenez.ecuafit.ui.utilities.ServiceReminder

import kotlinx.coroutines.launch

class AguaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAguaBinding
    private var agua = 0
    private var vaso = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAguaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("agua", Context.MODE_PRIVATE)
        vaso = sharedPref.getInt("agua", agua)
        Log.d("UCE", vaso.toString())
        binding.totalAgua.text = (vaso * 250).toString() + " ml"
        binding.totalVasos.text = "$vaso"
        binding.botonMas.setOnClickListener { agregarAgua() }
        binding.botonMenos.setOnClickListener { restarAgua() }

        createNotificationChannel()
        if (vaso < 8) {
            scheduleNotification(100)
        } else {
            cancelScheduledNotification()
        }
        scheduleMidnightJob()

        // Recuperar pasos y ritmo cardíaco desde Health Connect y mostrarlos
        obtenerPasosYRitmoCardiaco()

        // Botón para refrescar manualmente los pasos y el ritmo cardíaco
        binding.botonActualizar.setOnClickListener {
            obtenerPasosYRitmoCardiaco()
        }
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
            binding.pasos.text = "Pasos Totales: $totalSteps"

            val heartRate = readLastHeartRate()
            binding.ritmoCardiaco.text = "Último Ritmo Cardíaco: $heartRate bpm"
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

    private fun actualizarAgua() {
        binding.totalAgua.text = (vaso * 250).toString() + " ml"
        binding.totalVasos.text = "$vaso"
        val sharedPref = getSharedPreferences("agua", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("agua", vaso).apply()
        }
    }

    val CHANNEL: String = "Notificaciones"
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Ecuafit"
            val descriptionText = "Notificaciones recordatorios"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
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
            Log.d("WearConnection", "Pasos recibidos: $steps")
        }
    }
}
