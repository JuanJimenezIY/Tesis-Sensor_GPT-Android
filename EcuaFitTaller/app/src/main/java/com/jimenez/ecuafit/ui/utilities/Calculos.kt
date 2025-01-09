package com.jimenez.ecuafit.ui.utilities

import com.jimenez.ecuafit.data.entities.UsuarioDB
import java.math.RoundingMode

class Calculos {
    fun sumLista(lista:List<Double>):Double{
        return lista.sumOf { it }
    }
    companion object {
        fun round(num: Double): Double {
            return num.toBigDecimal().setScale(0, RoundingMode.UP).toDouble()
        }
        fun calcularTMB(usuario: UsuarioDB): Double {
            if (usuario.genero.contains("masculino")||usuario.genero.contains("Masculino")) {
                return (88.363 + ((13.397 * usuario.peso[usuario.peso.size - 1].toDouble()) + (4.7 * usuario.altura.toDouble()) - (5.66 * usuario.edad.toDouble())))*1.5
            } else {
                return (447.593 + ((9.247 * usuario.peso[usuario.peso.size - 1].toDouble()) + (3.08 * usuario.altura.toDouble()) - (4.68 * usuario.edad.toDouble())))*1.5
            }
        }
        fun calcularPorcentaje(caloriasRestantes:Double,calsTotales:Double):Double{
            return round((100 - ((caloriasRestantes * 100) / calsTotales)))
        }
    }
}