package com.jimenez.ecuafit.ui.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
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

            binding.lyChatCopia.visibility = View.GONE
            binding.plan.text= "Generar nuevo plan"
        }
    }

    private fun generarNuevoPlan() {
        binding.lyChatCopia.visibility = View.VISIBLE

        val remoteConfig = FirebaseRemoteConfig.getInstance()
        remoteConfig.fetchAndActivate().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val valor = remoteConfig.getString("api_key")
                openAI = OpenAI(valor, LoggingConfig(), Timeout(socket = 120.seconds))

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
    override fun onStart() {
        super.onStart()



        binding.lyChatCopia.visibility = View.INVISIBLE
        binding.plan.setOnClickListener {
            val reporteGuardado = leerReporteGuardado(this)

            if (!reporteGuardado.isNullOrEmpty()) {

                AlertDialog.Builder(this)
                    .setTitle("¿Estás seguro?")
                    .setMessage("Ya tienes un plan generado. ¿Deseas reemplazarlo por uno nuevo?")
                    .setPositiveButton("Sí") { _, _ ->
                        binding.chatGPT.text=""
                        generarNuevoPlan()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } else {

                generarNuevoPlan()
            }}

    }


    @OptIn(BetaOpenAI::class)
    suspend fun generaReporte(usuarioDB: UsuarioDB) {
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId("gpt-4o-mini"),
            messages = listOf(

                ChatMessage(
                    role = ChatRole.Assistant,
                    content = "Dame recomedaciones bien detalldas de ejercicios especificos para mejorar mi estado fisico actualmente mido "
                            + usuarioDB.altura + ",peso " + usuarioDB.peso!!.last().toDouble() + ",soy de genero " + usuarioDB.genero + " y tengo " + usuarioDB.edad +
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