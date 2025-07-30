package com.jimenez.ecuafit.logic

import com.jimenez.ecuafit.data.entities.Usuario
import com.jimenez.ecuafit.data.entities.UsuarioDB
import com.jimenez.ecuafit.ui.utilities.EcuaFit

class UsuarioLogicDB {
    suspend fun insertUsuario(u:Usuario){
        var usuarioData=UsuarioDB(
             altura=u.altura,
         correo=u.correo,
         edad= u.edad,
         genero=u.genero,
         nombre= u.nombre,
         peso=u.peso
        )
        EcuaFit.getDbUsuarioInstance().usuarioDao().insertUsuario(usuarioData)
    }
    suspend fun getUsuario(correo:String):UsuarioDB{
        return EcuaFit.getDbUsuarioInstance().usuarioDao().getUsuarioByCorreo(correo)
    }
    suspend fun deleteAll(){
        EcuaFit.getDbUsuarioInstance().usuarioDao().deleteAll()

    }



}