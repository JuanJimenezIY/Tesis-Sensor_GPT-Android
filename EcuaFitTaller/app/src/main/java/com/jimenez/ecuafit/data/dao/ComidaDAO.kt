package com.jimenez.ecuafit.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jimenez.ecuafit.data.entities.ComidaDB

@Dao
interface ComidaDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertComida(c: ComidaDB)
    @Query("select * from ComidaDB")
    fun getAllComida():List<ComidaDB>

    @Query("select * from ComidaDB where nombre=:nombre")
    fun getAllComidaByName(nombre:String):List<ComidaDB>
    @Query("select * from ComidaDB where fecha=:date ")
    fun getAllComidaByDia(date:String):List<ComidaDB>

    @Query("UPDATE ComidaDB SET cantidad=:cantidad WHERE id = :id")
    fun update(cantidad: Int?, id: Int)

    @Query("DELETE FROM ComidaDB")
    fun deleteAll()
    
}