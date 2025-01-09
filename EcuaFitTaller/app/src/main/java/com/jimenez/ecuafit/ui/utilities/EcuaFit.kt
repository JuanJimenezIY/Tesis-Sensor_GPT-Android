package com.jimenez.ecuafit.ui.utilities

import android.app.Application
import androidx.room.Room
import com.jimenez.ecuafit.data.connections.ComidaConnectionDB
import com.jimenez.ecuafit.data.connections.UsuarioConnectionDB

class EcuaFit:Application() {
    val name_class:String="Admin"
    override fun onCreate() {
        super.onCreate()
        db= Room.databaseBuilder(applicationContext,ComidaConnectionDB::class.java,"comidaDB").build()
        db2= Room.databaseBuilder(applicationContext,UsuarioConnectionDB::class.java,"usuarioDB").build()


    }

    companion object{
        val name_companion:String="Admin"
        private var db:ComidaConnectionDB ?= null
        private var db2:UsuarioConnectionDB ?= null

        fun getDbInstance():ComidaConnectionDB{
            return db!!
        }
        fun getDbUsuarioInstance():UsuarioConnectionDB{
            return db2!!
        }

    }
}