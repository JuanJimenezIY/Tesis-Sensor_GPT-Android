package com.jimenez.ecuafit.data.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Ejercicio(
    var cuerpo: String,
    val nombre: String,
    val video: String,

): Parcelable