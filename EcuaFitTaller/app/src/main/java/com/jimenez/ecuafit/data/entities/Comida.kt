package com.jimenez.ecuafit.data.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Comida(
    var calorias: Int,
    val descripcion: String,
    val foto: String,
    val macronutrientes: List<String>,
    val micronutrientes:List<String>,
    val nombre: String,
    val region: String
):Parcelable