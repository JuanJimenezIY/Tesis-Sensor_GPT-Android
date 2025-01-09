package com.jimenez.ecuafit.ui.viewmodels

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jimenez.ecuafit.data.entities.Ejercicio
import com.jimenez.ecuafit.logic.EjercicioLogic

class EjerciciosViewModel : ViewModel() {
    // Puedes definir propiedades LiveData si lo deseas
    val progressState =  MutableLiveData<Int>()
     val ejercicioItemsLiveData: MutableLiveData<List<Ejercicio>> = MutableLiveData()

    suspend fun loadEjercicios(){
        progressState.postValue(View.VISIBLE)
        val newitems = EjercicioLogic().getAllEjercicios()
        ejercicioItemsLiveData.postValue(newitems)
        progressState.postValue(View.GONE)
    }

    fun filterEjercicios(ejercicioItems: List<Ejercicio>, newText: String): List<Ejercicio> {

        return ejercicioItems.filter { item ->
            item.nombre.lowercase().contains(newText.lowercase())
        }
    }
}
