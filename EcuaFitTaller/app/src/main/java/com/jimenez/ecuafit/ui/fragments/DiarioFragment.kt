package com.jimenez.ecuafit.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlin.collections.filter
import com.jimenez.ecuafit.data.entities.Comida
import com.jimenez.ecuafit.databinding.FragmentDiarioBinding
import com.jimenez.ecuafit.logic.ComidaLogic
import com.jimenez.ecuafit.logic.ComidaLogicDB
import com.jimenez.ecuafit.ui.activities.AguaActivity
import com.jimenez.ecuafit.ui.activities.DetailsComidasItems
import com.jimenez.ecuafit.ui.adapters.ComidaAdapter
import com.jimenez.ecuafit.ui.utilities.EcuaFit
import com.jimenez.ecuafit.ui.viewmodels.ComidaViewModel
import com.jimenez.ecuafit.ui.viewmodels.EjerciciosViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.streams.toList


class DiarioFragment : Fragment() {
    private lateinit var binding: FragmentDiarioBinding
    private lateinit var lmanager: LinearLayoutManager
    private val comidaViewModel by viewModels<ComidaViewModel>()

    //  private lateinit var progressBar:ProgressBar
    private var rvAdapter: ComidaAdapter = ComidaAdapter(::sendComidaItem, ::saveComida)
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        chargeData()


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        binding = FragmentDiarioBinding.inflate(layoutInflater, container, false)
        lmanager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
        binding.rvComidas.addOnScrollListener(

            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        val v = lmanager.childCount
                        val p = lmanager.findFirstVisibleItemPosition()
                        val t = lmanager.itemCount
                    }
                }
            }
        )
        searchView = binding.searchComida
        //progressBar = binding.progressBar
        // Inflate the layout for this fragment
        binding.agua.setOnClickListener {
            val intent = Intent(requireContext(), AguaActivity::class.java)
            startActivity(intent)

        }
        return binding.root
    }


    fun saveComida(item: Comida): Boolean {
        Log.d("UCE", item.nombre)
        var d = lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                ComidaLogicDB().insertComida(item, 1, Date())

            }
        }

        return d.isCompleted
    }

    override fun onStart() {
        super.onStart()
        binding.searchComida.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {

                val filteredList = comidaViewModel.filterComida(
                    comidaViewModel.comidaItemsLiveData.value ?: emptyList(), newText
                )

                rvAdapter.replaceListAdapter(filteredList)



                return true
            }

        })
    }

    fun sendComidaItem(item: Comida) {

        val i = Intent(requireActivity(), DetailsComidasItems::class.java)
        i.putExtra("name", item)
        startActivity(i)
    }

    private fun chargeData() {
        comidaViewModel.progressState.observe(this) {
            binding.lyMainCopia.visibility = it

        }

        comidaViewModel.comidaItemsLiveData.observe(this, Observer { comidaItems ->
            rvAdapter.items = comidaItems
            binding.rvComidas.apply {
                this.adapter = rvAdapter
                //  this.layoutManager = lmanager
                this.layoutManager = lmanager
            }

        })
        comidaViewModel.progressState.observe(this) {
            binding.lyMainCopia.visibility = it

        }
        lifecycleScope.launch(Dispatchers.IO) {
            comidaViewModel.loadComida()
        }
    }
}