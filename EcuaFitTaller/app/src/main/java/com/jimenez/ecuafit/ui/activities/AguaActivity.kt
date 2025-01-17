
package com.jimenez.ecuafit.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.jimenez.ecuafit.R
import com.jimenez.ecuafit.data.entities.UsuarioDB
import com.jimenez.ecuafit.databinding.ActivityAguaBinding
import com.jimenez.ecuafit.ui.utilities.EcuaFit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
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
import android.content.pm.PackageManager
import android.os.Build
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.snackbar.Snackbar
import kotlin.time.Duration.Companion.seconds

class AguaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAguaBinding
    private lateinit var openAI: OpenAI

    companion object {
        const val CHANNEL_ID = "ecuafit_channel_id"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAguaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseApp.initializeApp(this)

        binding.generar.setOnClickListener {


            val remoteConfig = FirebaseRemoteConfig.getInstance()
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val valor = remoteConfig.getString("api_key")
                        openAI = OpenAI(
                            valor, LoggingConfig(), Timeout(socket = 120.seconds)
                        )
                        lifecycleScope.launch(Dispatchers.Main) {
                            val user = withContext(Dispatchers.IO) {
                                EcuaFit.getDbUsuarioInstance().usuarioDao().getAll()
                            }
                            generaReporte(user)
                        }
                    }
                }
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)
        }

        obtenerPasosYRitmoCardiaco()
        obtenerDuracionSueno()
    }

    private fun obtenerPasosYRitmoCardiaco() {
        lifecycleScope.launch(Dispatchers.Main) {
            val totalSteps = readTotalSteps()
            binding.pasos.text = "Pasos: $totalSteps / 8000"
            setProgressBarWithAnimation(binding.progressBar, totalSteps.toInt()) // Actualizar la barra de progreso con animación
            binding.pasosP.text = "${(totalSteps.toDouble() / binding.progressBar.max * 100).toInt()}%"

            // Mostrar mensaje de felicitaciones si se alcanza la meta
            if (totalSteps >= 8000) {
                Snackbar.make(binding.root, "¡Felicitaciones! Has alcanzado tu meta de pasos.", Snackbar.LENGTH_LONG).show()
            }

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

    @OptIn(BetaOpenAI::class)
    private suspend fun generaReporte(usuarioDB: UsuarioDB) {
        val steps = withContext(Dispatchers.IO) { readTotalSteps() }
        val heartRate = withContext(Dispatchers.IO) { readLastHeartRate() }
        val (hours, minutes) = withContext(Dispatchers.IO) { readLastSleepDuration() }

        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId("gpt-4o-mini"),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.Assistant,
                    content = "Dame recomedaciones bien detalladas de dieta y ejercicios para mejorar mi estado físico. " +
                            "Actualmente mido ${usuarioDB.altura}, peso ${usuarioDB.peso[0]}, soy de género ${usuarioDB.genero} " +
                            "y tengo ${usuarioDB.edad} años. He dado $steps pasos hoy, mi último ritmo cardíaco " +
                            "fue de $heartRate bpm y he dormido $hours horas y $minutes minutos. " +
                            "dame recomendaciones de salud en base a los pasos que di en el dia y como esto repercute en mi salud" +
                            "y cuanto he dormido hoy dime si estoy bien segun mi edad." +
                            "dame la respuesta sin poner simbolos de # ni otros parecidos, pon los titulos en negrita, necesito e texto mas limpio posible."
                )
            )
        )

        val completion: ChatCompletion = openAI.chatCompletion(chatCompletionRequest)
        completion.choices.forEach {
            Log.d("UCE", it.message?.content.toString())
        }

        binding.openAI.text = completion.choices[0].message?.content.toString()

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
            response.records.sumOf { it.count }
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
            heartRates.lastOrNull()?.beatsPerMinute ?: 0.0
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error al leer ritmo cardíaco: ${e.message}", e)
            0.0
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

            durationInHours to durationInMinutes
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error al leer la duración del sueño: ${e.message}", e)
            0 to 0
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
