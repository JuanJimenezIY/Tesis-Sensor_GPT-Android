package com.jimenez.ecuafit.ui.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.SearchView.OnCloseListener
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.jimenez.ecuafit.data.entities.Ejercicio
import com.jimenez.ecuafit.databinding.ActivityEjerciciosBinding
import com.jimenez.ecuafit.logic.EjercicioLogic
import com.jimenez.ecuafit.ui.adapters.EjercicioAdapter
import com.jimenez.ecuafit.ui.viewmodels.EjerciciosViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.streams.toList


class EjerciciosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEjerciciosBinding
    private lateinit var lmanager: LinearLayoutManager
    private lateinit var searchView: SearchView
    private val ejercicioViewModel by viewModels<EjerciciosViewModel>()

    //  private lateinit var progressBar:ProgressBar
    private var rvAdapter: EjercicioAdapter = EjercicioAdapter(::openVideo)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEjerciciosBinding.inflate(layoutInflater)
        lmanager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        chargeData()
        searchView = binding.searchEjercicios

        setContentView(binding.root)
    }

    fun openVideo(ejercicio: Ejercicio) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ejercicio.video))
        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()

        binding.searchEjercicios.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {

                val filteredList = ejercicioViewModel.filterEjercicios(
                    ejercicioViewModel.ejercicioItemsLiveData.value ?: emptyList(), newText
                )

                rvAdapter.replaceListAdapter(filteredList)



                return true
            }

        })
    }


    private fun chargeData() {
        ejercicioViewModel.progressState.observe(this) {
            binding.lyEjercicioCopia.visibility = it

        }

        ejercicioViewModel.ejercicioItemsLiveData.observe(this, Observer { ejercicioItems ->
            rvAdapter.items = ejercicioItems
            binding.rvEjercicios.apply {
                this.adapter = rvAdapter
                //  this.layoutManager = lmanager
                this.layoutManager = lmanager
            }

        })
        ejercicioViewModel.progressState.observe(this) {
            binding.lyEjercicioCopia.visibility = it

        }
        lifecycleScope.launch(Dispatchers.IO) {
            ejercicioViewModel.loadEjercicios()
        }
    }

}