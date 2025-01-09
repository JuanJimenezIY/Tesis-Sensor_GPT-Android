package com.jimenez.ecuafit.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.jimenez.ecuafit.R
import com.jimenez.ecuafit.data.entities.ComidaDB
import com.jimenez.ecuafit.data.entities.Usuario
import com.jimenez.ecuafit.data.entities.UsuarioDB
import com.jimenez.ecuafit.databinding.ActivityAguaBinding
import com.jimenez.ecuafit.databinding.ActivityPesoBinding
import com.jimenez.ecuafit.databinding.FragmentInformeBinding
import com.jimenez.ecuafit.logic.ComidaLogicDB
import com.jimenez.ecuafit.logic.UsuarioLogic
import com.jimenez.ecuafit.ui.activities.AguaActivity
import com.jimenez.ecuafit.ui.activities.ComidaDiariaActivity
import com.jimenez.ecuafit.ui.activities.EjerciciosActivity
import com.jimenez.ecuafit.ui.activities.MainActivity
import com.jimenez.ecuafit.ui.activities.PesoActivity
import com.jimenez.ecuafit.ui.activities.RegistroActivity
import com.jimenez.ecuafit.ui.utilities.Calculos
import com.jimenez.ecuafit.ui.utilities.EcuaFit
import com.jimenez.ecuafit.ui.utilities.SessionManager
import com.jimenez.ecuafit.ui.viewmodels.InformeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale


class InformeFragment : Fragment() {

    private lateinit var binding: FragmentInformeBinding
    private var comidaItems: MutableList<ComidaDB> = mutableListOf();
    private val viewModel by viewModels<InformeViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentInformeBinding.inflate(layoutInflater)

        observeData()

    }
    private fun observeData() {
        viewModel.loadComidaItems()
        viewModel.calcCalsTotales()

        viewModel.comidaItemsLiveData.observe(this, Observer { comidaItems ->
            updateUI(comidaItems)
        })

    }
    private fun updateUI(comidaItems: List<ComidaDB>) {
        var sumaCalorias = comidaItems.sumOf { it.calorias }
        var sumaGrasas = comidaItems.sumOf { it.macronutrientes[0].toDouble() }
        var sumaProteinas = comidaItems.sumOf { it.macronutrientes[1].toDouble() }
        var sumaCarbs = comidaItems.sumOf { it.macronutrientes[2].toDouble() }

        binding.calsConsumidas.text = "Consumidas: $sumaCalorias kcals"
        binding.proteinasCons.text = Calculos.round(sumaProteinas).toInt().toString()
        binding.carbsCons.text = Calculos.round(sumaCarbs).toInt().toString()
        binding.grasaCons.text = Calculos.round(sumaGrasas).toInt().toString()

        //val calsTotales=viewModel.calsTotales.value

        viewModel.calsTotales.observe(viewLifecycleOwner, Observer { calsTotales ->
            val caloriasRestantes = calsTotales.toDouble() - sumaCalorias
            binding.calsRestantes.text = String.format("%.2f", caloriasRestantes)
            binding.procentaje.text = Calculos.calcularPorcentaje(caloriasRestantes, calsTotales.toDouble()).toInt().toString()

            calcularReqDiario(calsTotales)
        })

    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        //var comidaDiaria=
        // Configurar el click listener para el CardView de Peso
        binding.pesoText.setOnClickListener {
            val intent = Intent(requireContext(), PesoActivity::class.java)
            startActivity(intent)
        }
        binding.infDiario.setOnClickListener {
            val intent = Intent(requireContext(), ComidaDiariaActivity::class.java)
            startActivity(intent)
        }
        binding.logOut.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO){
                SessionManager.logOut(requireContext().getSharedPreferences("sesion",Context.MODE_PRIVATE))
                val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
            }
            SessionManager.resetAgua(requireContext().getSharedPreferences("agua",Context.MODE_PRIVATE))
        }
        binding.cardEjercicios.setOnClickListener {
            val intent = Intent(requireContext(), EjerciciosActivity::class.java)
            startActivity(intent)

        }

    }





    fun calcularReqDiario(calsTotales: String) {
        binding.proteinasRes.text = Calculos.round((calsTotales.toDouble() * 0.30) / 4).toInt().toString()

        binding.carbsRes.text = Calculos.round((calsTotales.toDouble() * 0.45) / 4).toInt().toString()
        binding.grasaRes.text = Calculos.round((calsTotales.toDouble() * 0.25) / 9).toInt().toString()
    }


}