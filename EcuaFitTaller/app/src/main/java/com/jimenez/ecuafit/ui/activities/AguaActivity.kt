
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
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.units.Percentage
import com.google.android.material.snackbar.Snackbar
import com.jimenez.ecuafit.ui.utilities.Calculos.Companion.round
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

        binding.openAI.visibility = View.INVISIBLE
        binding.generar.setOnClickListener {

            if(binding.chatGPT.text!=null){
                binding.chatGPT.text=""
            }
            binding.openAI.visibility = View.VISIBLE
            binding.generar.isEnabled=false
            binding.generar.text = "Cargando..."
            binding.generar.setBackgroundColor(ContextCompat.getColor(this, R.color.black))


            val remoteConfig = FirebaseRemoteConfig.getInstance()
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val valor = remoteConfig.getString("api_key")
                        Log.d("UCE",  valor)

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

        obtenerPasosRitmoYGrasaCorporal()
        obtenerDuracionSueno()
    }



    private fun obtenerPasosRitmoYGrasaCorporal() {
        lifecycleScope.launch(Dispatchers.Main) {
            val totalSteps = readTotalSteps()
            binding.pasos.text = "Pasos: $totalSteps / 8000"
            setProgressBarWithAnimation(binding.progressBar, totalSteps.toInt()) // Actualizar la barra de progreso con animación
            binding.pasosP.text = "${(totalSteps.toDouble() / binding.progressBar.max * 100).toInt()}%"

            // Mostrar mensaje de felicitaciones si se alcanza la meta
            if (totalSteps >= 5000) {
                Snackbar.make(binding.root, "¡Felicitaciones! Has alcanzado tu meta de pasos.", Snackbar.LENGTH_LONG).show()
            }

            val heartRate = readLastHeartRate()
            binding.ritmoCardiaco.text = "Ritmo Cardíaco: $heartRate bpm"

            val bodyFatPercentage =  readBodyFatPercentage()
            if (bodyFatPercentage > 0.0) {
                binding.grasaCorporal.text = "Grasa Corporal: ${"%.1f".format(bodyFatPercentage)}%"
            } else {
                binding.grasaCorporal.text = "Grasa Corporal: Sin datos"
            }



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
        try {
            // Obtener los datos del usuario
            val steps = withContext(Dispatchers.IO) { readTotalSteps() }
            val fatRecord = withContext(Dispatchers.IO) { readBodyFatPercentage()  }
            val heartRate = withContext(Dispatchers.IO) { readLastHeartRate() }
            val (hours, minutes) = withContext(Dispatchers.IO) { readLastSleepDuration() }

            // Crear la solicitud de chat
            val chatCompletionRequest = ChatCompletionRequest(
                model = ModelId("gpt-4o-mini"),
                messages = listOf(
                    ChatMessage(
                        role = ChatRole.Assistant,
                        content = "Dado que el usuario tiene ${usuarioDB.edad} años, esta es su lista de pesos a traves del tiempo ${usuarioDB.peso} dame una recomendacion nutricional comparando los pesos,felicita al usuario si ha bajado de peso; y tomando el ultimo como el actual peso,de genero ${usuarioDB.genero}, mide ${usuarioDB.altura} cm, su frecuencia cardíaca promedio en reposo es $heartRate BPM, su  índice de grasa corporal es $fatRecord y " +
                                "camino  $steps pasos en el día,ademas durmio  $hours horas y $minutes minutos. ¿cuáles serían las mejores recomendaciones personalizadas para mejorar su salud en términos de  dieta dando una dieta especifcia  y consejos de bienestar general?" +
                                "dame la respuesta en un formato de maximo  4 parrafos, donde no haya simbolos en el texto, ademas dame los datos entregados anteriormente y evalua si son buenos o malos detalladamente,esta ultimo paso es obligatorio siempre se debe mostrar los datos tomados y evaluarlos " +
                                "en comparacion con la salud normal que debe tener una persona, comportate como un experto en fitness, si algun parametro es 0.0 ignorarlo y no mencionarlo"

                    )
                )
            )

            // Llamada a la API de OpenAI
            val completion: ChatCompletion = openAI.chatCompletion(chatCompletionRequest)

            // Mostrar la respuesta en el log
            completion.choices.forEach {
                Log.d("UCE", it.message?.content.toString())
            }

            // Actualizar la UI con la respuesta
            binding.chatGPT.text = completion.choices[0].message?.content.toString()


        } catch (e: Exception) {
            // Capturar cualquier excepción y mostrar un mensaje de error
            Log.e("UCE", "Error al generar el reporte: ${e.message}")
            binding.chatGPT.text = "Ocurrió un error al generar el reporte. Intenta nuevamente más tarde."
        }
        binding.openAI.visibility = View.GONE
        binding.generar.isEnabled=true
        binding.generar.text = "Generar de nuevo"
        binding.generar.setBackgroundColor(ContextCompat.getColor(this, R.color.color_boton))
    }
    private suspend fun readBodyFatPercentage(): Double = withContext(Dispatchers.IO) {
        try {
            val healthConnectClient = HealthConnectClient.getOrCreate(this@AguaActivity)
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()

            val response = healthConnectClient.readRecords<BodyFatRecord>(
                ReadRecordsRequest(
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, Instant.now()),

                    ascendingOrder = false
                )
            )

            val valoresValidos = response.records.mapNotNull { it.percentage?.value }
                .filter { it in 3.0..60.0 } // Rango biológicamente razonable

            val promedio = valoresValidos.average().takeIf { !it.isNaN() } ?: 0.0

            promedio

        } catch (e: Exception) {
            Log.e("HealthConnect", "Error al leer porcentaje de grasa corporal: ${e.message}", e)
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
            response.records.sumOf {
                it.count
            }

        } catch (e: Exception) {
            Log.e("HealthConnect", "Error al leer pasos totales: ${e.message}", e)
            0
        }
    }

    private suspend fun readLastHeartRate(): Number = withContext(Dispatchers.IO) {
        try {
            val healthConnectClient = HealthConnectClient.getOrCreate(this@AguaActivity)
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, Instant.now())
                )
            )
            val valoresValidos = response.records
                .flatMap { it.samples } // Extrae todos los samples de todos los registros
                .map { it.beatsPerMinute } // Toma el valor de BPM
                .filter { it.toDouble() in 30.0..220.0 }

            // Calcula el promedio si hay valores válidos, si no, retorna 0.0
            val promedio = valoresValidos.average().takeIf { !it.isNaN() } ?: 0.0
            (round(promedio * 10) / 10)
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
