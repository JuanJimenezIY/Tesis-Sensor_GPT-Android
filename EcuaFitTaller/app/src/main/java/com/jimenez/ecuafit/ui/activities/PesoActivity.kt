package com.jimenez.ecuafit.ui.activities

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.jimenez.ecuafit.R
import com.jimenez.ecuafit.data.entities.UsuarioDB
import com.jimenez.ecuafit.databinding.ActivityAguaBinding
import com.jimenez.ecuafit.databinding.ActivityPesoBinding
import com.jimenez.ecuafit.ui.utilities.EcuaFit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PesoActivity : AppCompatActivity() {
    private lateinit var binding:ActivityPesoBinding
    private lateinit var usuario:UsuarioDB
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPesoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lifecycleScope.launch(Dispatchers.IO) {
            usuario = EcuaFit.getDbUsuarioInstance().usuarioDao().getAll()
            withContext(Dispatchers.Main) {
                recoverPeso()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        binding.btnActualizarPSo.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    actualizarPeso()
                    Snackbar.make(binding.peso, "Peso actualizado", Snackbar.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Snackbar.make(binding.peso, "Ocurrió un error", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
    fun recoverPeso(){
            binding.peso.text= Editable.Factory.getInstance().newEditable(usuario.peso[usuario.peso.size-1])


    }
    //acutalizar que el peso ingresado se ingrese en la bsae de datos
    @SuppressLint("SuspiciousIndentation")
    suspend fun actualizarPeso() {
        val nuevoPeso=binding.peso.text.toString()

            withContext(Dispatchers.IO){
                EcuaFit.getDbUsuarioInstance().usuarioDao().update(usuario.peso.plus(nuevoPeso))
            }
    }
}
