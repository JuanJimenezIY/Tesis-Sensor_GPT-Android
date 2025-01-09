package com.jimenez.ecuafit.data.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize

data class Usuario(var altura: String,
                   val correo: String,
                   val edad: String,
                   val genero:String,
                   val nombre: String,
                   val peso: List<String>): Parcelable {
}