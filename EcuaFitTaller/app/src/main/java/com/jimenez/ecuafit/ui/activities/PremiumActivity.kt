package com.jimenez.ecuafit.ui.activities

import android.content.Context
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
import com.jimenez.ecuafit.data.entities.UsuarioDB
import com.jimenez.ecuafit.databinding.ActivityPremiumBinding
import com.jimenez.ecuafit.ui.utilities.EcuaFit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

class PremiumActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPremiumBinding
    private lateinit var openAI: OpenAI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)


        FirebaseApp.initializeApp(this)

        val reporteGuardado = leerReporteGuardado(this)
        if (!reporteGuardado.isNullOrEmpty()) {
            binding.chatGPT.text = reporteGuardado
            binding.lyChatCopia.visibility = View.GONE // Oculta el loading si ya hay respuesta
            binding.plan.text= "Generar nuevo plan"
        }
    }

    override fun onStart() {
        super.onStart()



        binding.lyChatCopia.visibility = View.INVISIBLE
        binding.plan.setOnClickListener {

            binding.lyChatCopia.visibility = View.VISIBLE

            val remoteConfig = FirebaseRemoteConfig.getInstance()
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Fetch exitoso y valores activados
                        val valor = remoteConfig.getString("api_key")
                        openAI = OpenAI(
                            valor, LoggingConfig(),Timeout(socket = 120.seconds)

                        )
                        lifecycleScope.launch(Dispatchers.Main) {
                            var user = withContext(Dispatchers.IO) {
                                EcuaFit.getDbUsuarioInstance().usuarioDao().getAll()
                            }
                            generaReporte(user)

                        }
                        // Utiliza el valor en tu app
                    }
                }
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // Intervalo mínimo entre actualizaciones
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)
        }

    }


    @OptIn(BetaOpenAI::class)
    suspend fun generaReporte(usuarioDB: UsuarioDB) {
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId("gpt-4o-mini"),
            messages = listOf(

                ChatMessage(
                    role = ChatRole.Assistant,
//                    content = "Dame recomedaciones bien detalldas dieta y ejercicios especificos para mejorar mi estado fisico actualmente mido "+
//                          "  170 cm,peso 65kg,soy de genero masculino  y tengo 18"+
//                              "de edad calcula mi requerimiento calorico diario con nivel de actividad fisica baja y dame recomendaciones"
                    content = "Dame recomedaciones bien detalldas de ejercicios especificos para mejorar mi estado fisico actualmente mido "
                            + usuarioDB.altura + ",peso " + usuarioDB.peso[0] + ",soy de genero " + usuarioDB.genero + " y tengo " + usuarioDB.edad +
                            " años de edad dame una rutina de ejrcicios  para la semana completa, no pongas simbolos raros como ## o  **, y divide cada dia de la "+
                            "semana claramente "
                )
            )
        )
        val completion: ChatCompletion = openAI.chatCompletion((chatCompletionRequest))
        completion.choices.forEach {

            Log.d("UCE", it.message?.content.toString())
        }
        val resultado = completion.choices[0].message?.content.toString()
        guardarReporteEnPreferencias(this, resultado)

        binding.chatGPT.text=resultado
        binding.lyChatCopia.visibility = View.GONE



    }
    fun guardarReporteEnPreferencias(context: Context, reporte: String) {
        val prefs = context.getSharedPreferences("reportes", Context.MODE_PRIVATE)
        prefs.edit().putString("reporte_guardado", reporte).apply()
    }

    fun leerReporteGuardado(context: Context): String? {
        val prefs = context.getSharedPreferences("reportes", Context.MODE_PRIVATE)
        return prefs.getString("reporte_guardado", null)
    }
}