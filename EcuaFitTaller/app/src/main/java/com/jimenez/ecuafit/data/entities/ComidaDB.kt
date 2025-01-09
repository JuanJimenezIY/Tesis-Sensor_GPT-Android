package com.jimenez.ecuafit.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
@Entity
data class ComidaDB (@PrimaryKey(autoGenerate = true)val id: Int = 0,val calorias: Int,
                     val descripcion: String,
                     val foto: String,
                     val macronutrientes: List<String>,
                     val micronutrientes:List<String>,
                     val nombre: String,
                     val region: String,
val cantidad:Int,val fecha:Date):Parcelable