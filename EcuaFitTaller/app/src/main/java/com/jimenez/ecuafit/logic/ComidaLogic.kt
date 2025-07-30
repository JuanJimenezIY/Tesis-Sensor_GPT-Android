package com.jimenez.ecuafit.logic

import com.google.firebase.firestore.FirebaseFirestore
import com.jimenez.ecuafit.data.entities.Comida
import kotlinx.coroutines.tasks.await

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ComidaLogic {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getAllComida(): List<Comida> = withContext(Dispatchers.IO) {
        val comidaList = mutableListOf<Comida>()

        try {
            val querySnapshot = db.collection("comida").get().await()
            for (document in querySnapshot) {
                val m = Comida(
                    document.getLong("calorias")?.toInt() ?: 0,
                    document.getString("descripcion") ?: "",
                    document.getString("foto") ?: "",
                    (document.get("macronutrientes") ?: "") as List<String>,
                    (document.get("micronutrientes") ?: "") as List<String>,
                    document.getString("nombre") ?: "",
                    document.getString("region") ?: ""
                )
                comidaList.add(m)
            }
        } catch (e: Exception) {

        }

        comidaList
    }
}
