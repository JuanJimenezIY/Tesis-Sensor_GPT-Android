package com.jimenez.ecuafit.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope

import com.anychart.AnyChart
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.charts.Cartesian
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
    private var comidaItems: MutableList<ComidaDB> = mutableListOf();

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
        // line.crosshair().enabled(true)
        line.title("Historial de peso")
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


        //line.tooltip().positionMode(TooltipPositionMode.POINT)
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
        if(peso!=null){
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
        var task=lifecycleScope.launch(Dispatchers.Main) {
            comidaItems = withContext(Dispatchers.IO) {
                return@withContext ComidaLogicDB().getAllComidaByFecha(date)
            } as MutableList<ComidaDB>
           if(comidaItems!=null){
               binding.inicioCalsConsu.text= comidaItems.sumOf { it.calorias }.toString()

           }
            else{
                binding.inicioCalsConsu.text=("0")
           }
        }
    }
    fun calcular(all: UsuarioDB):Double {
        var sum=0.0
        all.peso.forEach {
            sum=sum+(it.toDouble())
        }
        return sum
    }


}