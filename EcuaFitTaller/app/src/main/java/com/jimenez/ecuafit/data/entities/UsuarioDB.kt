package com.jimenez.ecuafit.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class UsuarioDB(@PrimaryKey(autoGenerate = true)val id: Int = 0,var altura: String,
                     val correo: String,
                     val edad: String,
                     val genero:String,
                     val nombre: String,
                     val peso: List<String>) : Parcelable
