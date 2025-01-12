package com.jimenez.ecuafit.ui.fragments

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope

import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.charts.Cartesian
import com.anychart.enums.TooltipPositionMode
import com.anychart.graphics.vector.SolidFill
import com.jimenez.ecuafit.R
import com.jimenez.ecuafit.data.entities.ComidaDB
import com.jimenez.ecuafit.data.entities.UsuarioDB
import com.jimenez.ecuafit.databinding.FragmentInicioBinding
import com.jimenez.ecuafit.logic.ComidaLogicDB
import com.jimenez.ecuafit.ui.activities.PremiumActivity
import com.jimenez.ecuafit.ui.utilities.EcuaFit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class InicioFragment : Fragment() {
    private lateinit var binding: FragmentInicioBinding
    private lateinit var line: Cartesian
    private var comidaItems: MutableList<ComidaDB> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentInicioBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        line = AnyChart.line()
        line.animation(true)
        line.title("Historial de peso")

        // Detectar el modo del sistema y aplicar los colores correspondientes
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> {
                // Modo oscuro
                line.background().fill(SolidFill("#121212", 1.0)) // Fondo del gráfico
                line.xAxis(0).labels().fontColor("#FFFFFF") // Color de las etiquetas del eje X
                line.yAxis(0).labels().fontColor("#FFFFFF") // Color de las etiquetas del eje Y
                line.title().fontColor("white") // Color del título
                line.tooltip().background().fill(SolidFill("#333333", 1.0)) // Fondo del tooltip
                line.tooltip().fontColor("#FFFFFF") // Color del texto del tooltip

            }
            Configuration.UI_MODE_NIGHT_NO -> {
                // Modo claro
                line.background().fill(SolidFill("#FFFFFF", 1.0)) // Fondo del gráfico
                line.xAxis(0).labels().fontColor("#000000") // Color de las etiquetas del eje X
                line.yAxis(0).labels().fontColor("#000000") // Color de las etiquetas del eje Y
                line.title().fontColor("#000000") // Color del título
                line.tooltip().background().fill(SolidFill("#F0F0F0", 1.0)) // Fondo del tooltip
                line.tooltip().fontColor("#000000") // Color del texto del tooltip

            }
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch(Dispatchers.Main) {
            chargeData()
        }
        binding.cardPremium.setOnClickListener {
            val intent = Intent(requireActivity(), PremiumActivity::class.java)
            startActivity(intent)
        }
    }

    suspend fun chargeData() {
        var peso: List<String>? = null
        withContext(Dispatchers.IO) {
            val usuarioDB = EcuaFit.getDbUsuarioInstance().usuarioDao().getAll()
            if (usuarioDB != null) {
                peso = usuarioDB.peso
            }
        }
        Log.d("UCE", peso.toString())
        val data: MutableList<DataEntry> = ArrayList()
        val maxVisible = 10
        if (peso != null) {
            peso!!.takeLast(maxVisible).forEachIndexed { i, u ->
                data.add(ValueDataEntry(i, u.toDouble()))
            }

            if (peso!!.size >= 2) {
                line.data(data)
                binding.anyChartView.setChart(line)
                binding.txtPesoInicio.text =
                    (peso!!.last().toDouble() - peso!![peso!!.size - 2].toDouble()).toString()
            }
        }
        val localDateTime = LocalDateTime.now()
        val instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant()
        val date = Date.from(instant)
        var task = lifecycleScope.launch(Dispatchers.Main) {
            comidaItems = withContext(Dispatchers.IO) {
                return@withContext ComidaLogicDB().getAllComidaByFecha(date)
            } as MutableList<ComidaDB>
            if (comidaItems != null) {
                binding.inicioCalsConsu.text = comidaItems.sumOf { it.calorias }.toString()
            } else {
                binding.inicioCalsConsu.text = "0"
            }
        }
    }

    fun calcular(all: UsuarioDB): Double {
        var sum = 0.0
        all.peso.forEach {
            sum += it.toDouble()
        }
        return sum
    }
}