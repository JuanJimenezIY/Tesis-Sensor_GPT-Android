package com.jimenez.ecuafit.logic

import com.jimenez.ecuafit.data.entities.UsuarioDB
import com.jimenez.ecuafit.ui.utilities.EcuaFit

class UsuarioLogic {

        suspend fun getUsuario(): UsuarioDB {
            return EcuaFit.getDbUsuarioInstance().usuarioDao().getAll()
        }


}