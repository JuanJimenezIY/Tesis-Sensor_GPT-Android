package com.jimenez.ecuafit.ui.utilities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.lifecycleScope
import at.favre.lib.crypto.bcrypt.BCrypt
import com.jimenez.ecuafit.logic.ComidaLogicDB
import com.jimenez.ecuafit.logic.UsuarioLogicDB
import com.jimenez.ecuafit.ui.activities.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionManager {

    companion object {

        fun encryptPass(pass: String): String {

            var bcryptHashString = BCrypt.withDefaults().hashToString(12, pass.toCharArray());

            return bcryptHashString
        }

        public fun comprobar(pass: String, hash: String?): Boolean {
            val hashBytes = hash?.toByteArray() ?: return false
            var result = BCrypt.verifyer().verify(pass.toCharArray(), hashBytes).verified
            return result

        }

        fun guardarSesion(pref: SharedPreferences) {


            val estado = true;
            with(pref.edit()) {
                putBoolean("estado_usu", estado)
                    .apply()
            }


        }

        fun validarSesion(pref: SharedPreferences): Boolean {

            return pref.getBoolean("estado_usu", false)

        }

        suspend fun logOut(sharedPreferences: SharedPreferences) {


            UsuarioLogicDB().deleteAll()
            ComidaLogicDB().deleteAll()


            with(sharedPreferences.edit()) {
                putBoolean("estado_usu", false).apply()
            }

        }
        fun resetAgua(sharedPreferences: SharedPreferences){

            with(sharedPreferences.edit()) {
                putInt("agua", 0)
                    .apply()
            }
        }
    }
}