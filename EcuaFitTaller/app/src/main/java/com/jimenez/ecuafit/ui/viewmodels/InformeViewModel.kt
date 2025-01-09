package com.jimenez.ecuafit.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.jimenez.ecuafit.data.entities.ComidaDB
import com.jimenez.ecuafit.logic.ComidaLogicDB
import com.jimenez.ecuafit.logic.UsuarioLogic
import com.jimenez.ecuafit.ui.utilities.Calculos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class InformeViewModel : ViewModel() {

    val progressState = MutableLiveData<Boolean>()
    val comidaItemsLiveData: MutableLiveData<List<ComidaDB>> = MutableLiveData()
    val calsTotales: MutableLiveData<String> = MutableLiveData()

    fun loadComidaItems() {
        viewModelScope.launch(Dispatchers.Main) {
            progressState.value = true

            val localDateTime = LocalDateTime.now()
            val instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant()
            val date = Date.from(instant)

            val comidaItems = withContext(Dispatchers.IO) {
                ComidaLogicDB().getAllComidaByFecha(date)
            }

            comidaItemsLiveData.postValue(comidaItems)
            progressState.value = false
        }
    }

    fun calcCalsTotales() {
        viewModelScope.launch(Dispatchers.Main) {
            val a = withContext(Dispatchers.IO) {
               Calculos.calcularTMB(UsuarioLogic().getUsuario()).toString()
            }
            calsTotales.postValue(a)
        }
    }

}
