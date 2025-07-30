package com.jimenez.ecuafit.logic

import android.util.Log
import com.jimenez.ecuafit.data.entities.Comida
import com.jimenez.ecuafit.data.entities.ComidaDB
import com.jimenez.ecuafit.ui.utilities.EcuaFit
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.streams.toList
import kotlin.time.Duration.Companion.hours

class ComidaLogicDB {
    suspend fun insertComida(item:Comida,cantidad:Int,fecha:Date){
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val localDateTime = LocalDateTime.now()
        val instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant()
        val date = Date.from(instant)

        var comidaActual=getAllComidaByFecha(date)
        var comidaName=comidaActual.stream().filter { x->x.nombre==item.nombre }.toList()


        var ComidaData=
            ComidaDB(
                calorias = item.calorias*cantidad,
                descripcion = item.descripcion,
                foto = item.foto,
                macronutrientes = item.macronutrientes.stream().map { x->((x.toDouble())*cantidad).toString() }.toList(),
                micronutrientes = item.micronutrientes,
                nombre = item.nombre,
                region = item.region,
                fecha = fecha,
                cantidad = cantidad
            )
        EcuaFit.getDbInstance().comidaDao().insertComida(ComidaData)
    }
    suspend fun getAllComida():List<ComidaDB>{
        return EcuaFit.getDbInstance().comidaDao().getAllComida();
    }
    suspend fun getAllComidaByFecha(fecha:Date):List<ComidaDB>{

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        return EcuaFit.getDbInstance().comidaDao().getAllComidaByDia(dateFormat.format(fecha));
    }
    suspend fun actualizarComida(){
        return
    }
    suspend fun deleteAll(){
        EcuaFit.getDbInstance().comidaDao().deleteAll()
    }
}