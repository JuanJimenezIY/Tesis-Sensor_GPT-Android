package com.jimenez.ecuafit.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.jimenez.ecuafit.data.entities.Comida
import com.jimenez.ecuafit.databinding.ActivityDetailsComidasItemsBinding
import com.jimenez.ecuafit.logic.ComidaLogicDB
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.RoundingMode
import java.util.Date
import kotlin.streams.toList


class DetailsComidasItems : AppCompatActivity() {
    private lateinit var binding: ActivityDetailsComidasItemsBinding
    private var macroNut = listOf<String>("Grasas        "
                                        , "Proteinas   "
                                        , "Carbs         ")

    private var values= listOf<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsComidasItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()

        val item = intent.getParcelableExtra<Comida>("name")
        if (item != null) {
            var count = 0
            values=values.plus(item.calorias.toString())
            values=values.plus(item.macronutrientes)
            binding.macroList.adapter = ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                item.macronutrientes.stream().map {

                    count++
                    macroNut[count-1] + it + " gr"
                }

                    .toList())
            binding.microList.adapter = ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                item.micronutrientes
            )

            binding.region.text=item.region
            binding.nombreComida.text = item.nombre
            binding.calorias.text = item.calorias.toString()
            Picasso.get().load(item.foto).into(binding.imagenComida)

        }
        binding.btnAddComida.setOnClickListener {
            if (item != null) {
                saveComida(item)
            }
            Snackbar.make(binding.btnAddComida,"Comida agregada",Snackbar.LENGTH_SHORT).show()
        }
        binding.cantidadProcion.addTextChangedListener {
            if(it.toString().isNotEmpty()){
                if (item != null) {
                    var count = 0
                    binding.calorias.text = (Integer.parseInt(values[0]) *Integer.parseInt(it.toString())).toString()
                    binding.macroList.adapter = ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_list_item_1,
                        item.macronutrientes.stream().map {macItem->

                            count++
                            macroNut[count-1] + (macItem.toDouble()*Integer.parseInt(it.toString())).toBigDecimal().setScale(2,RoundingMode.UP).toString() + " gr"
                        }

                            .toList())
                }
            }
            else{
                binding.calorias.text=values[0]
                var count = 0

                if (item != null) {
                    binding.macroList.adapter = ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_list_item_1,
                        item.macronutrientes.stream().map {

                            count++
                            macroNut[count-1] + it + " gr"
                        }

                            .toList())
                }
            }

        }
    }
    fun saveComida(item:Comida):Boolean{
        var d=lifecycleScope.launch(Dispatchers.Main){
            withContext(Dispatchers.IO){
                ComidaLogicDB().insertComida(item, Integer.parseInt(binding.cantidadProcion.text.toString()), Date())

            }
        }

        return d.isCompleted
    }

}