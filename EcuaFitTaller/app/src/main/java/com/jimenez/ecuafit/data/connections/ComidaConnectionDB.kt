package com.jimenez.ecuafit.data.connections

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jimenez.ecuafit.data.entities.ComidaDB
import com.jimenez.ecuafit.data.dao.ComidaDAO
import com.jimenez.ecuafit.ui.utilities.Converter

@Database(entities = [ComidaDB::class],
    version = 1)
@TypeConverters(Converter::class)
abstract class ComidaConnectionDB:RoomDatabase() {
    abstract fun comidaDao():ComidaDAO
}