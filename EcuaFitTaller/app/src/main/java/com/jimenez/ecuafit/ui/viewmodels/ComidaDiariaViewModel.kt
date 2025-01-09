package com.jimenez.ecuafit.ui.viewmodels

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jimenez.ecuafit.data.entities.Comida
import com.jimenez.ecuafit.logic.ComidaLogic

class ComidaDiariaViewModel:ViewModel() {
    val progressState =  MutableLiveData<Int>()
    val comidaItemsLiveData: MutableLiveData<List<Comida>> = MutableLiveData()
    suspend fun loadComidaDB(){
        progressState.postValue(View.VISIBLE)
        val newitems = ComidaLogic().getAllComida()
        comidaItemsLiveData.postValue(newitems)
        progressState.postValue(View.GONE)
    }

    fun filterComida(comidaItems: List<Comida>, newText: String): List<Comida> {

        return comidaItems.filter { item ->
            item.nombre.lowercase().contains(newText.lowercase())
        }
    }
}