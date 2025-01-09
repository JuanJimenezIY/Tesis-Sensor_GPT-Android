package com.jimenez.ecuafit.data.connections

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jimenez.ecuafit.data.dao.UsuarioDAO
import com.jimenez.ecuafit.data.entities.UsuarioDB
import com.jimenez.ecuafit.ui.utilities.Converter

@Database(entities = [UsuarioDB::class],
    version = 1)
@TypeConverters(Converter::class)
abstract class UsuarioConnectionDB:RoomDatabase() {
    abstract fun usuarioDao():UsuarioDAO
}