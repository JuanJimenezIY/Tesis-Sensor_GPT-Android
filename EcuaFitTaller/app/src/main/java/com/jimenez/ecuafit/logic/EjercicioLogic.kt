package com.jimenez.ecuafit.logic

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.jimenez.ecuafit.data.entities.Comida
import com.jimenez.ecuafit.data.entities.Ejercicio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class EjercicioLogic {
    private val db = FirebaseFirestore.getInstance()
    suspend fun getAllEjercicios(): List<Ejercicio> = withContext(Dispatchers.IO) {
        val ejercicioList = mutableListOf<Ejercicio>()

        try {
            val querySnapshot = db.collection("ejercicio").get().await()
            for (document in querySnapshot) {
                val m = Ejercicio(
                    document.getString("cuerpo") ?: "",
                    document.getString("nombre") ?: "",
                    document.getString("video") ?: "",
                )
                ejercicioList.add(m)
            }
        } catch (e: Exception) {

        }

        ejercicioList
    }
}