package com.jimenez.ecuafit.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import at.favre.lib.crypto.bcrypt.BCrypt
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.jimenez.ecuafit.databinding.ActivityRegistroBinding
import com.jimenez.ecuafit.ui.utilities.SessionManager

class RegistroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistroBinding
    private val db = FirebaseFirestore.getInstance()


    private val TAG = "UCE"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()


        binding.buttonRegister.setOnClickListener {

            if (validar()) {
                val res =
                    db.collection("users").document(binding.editTextEmail.text.toString()).set(
                        hashMapOf(
                            "altura" to binding.editTextAltura.text.toString(),
                            "correo" to binding.editTextEmail.text.toString(),
                            "edad" to binding.editTextEdad.text.toString(),
                            "genero" to binding.editTextGenero.text.toString(),
                            "nombre" to binding.editTextName.text.toString(),
                            "peso" to listOf(binding.editTextPeso.text.toString()),
                            "contraseña" to SessionManager.encryptPass(binding.editTextContraseA.text.toString())
                        )
                    )
                res.addOnCompleteListener {
                    if (it.isSuccessful) {
                        Snackbar.make(
                            binding.textViewEmailLabel,
                            "Registro completo",
                            Snackbar.LENGTH_SHORT
                        ).show()

                    }
                    val i = Intent()
                    i.putExtra("result", "Resultado Exitoso")
                    setResult(RESULT_OK, i)
                    finish()
                }
            }


        }
    }


    fun validar(): Boolean {
        val campos = arrayOf(
            binding.editTextEmail,
            binding.editTextAltura,
            binding.editTextEdad,
            binding.editTextGenero,
            binding.editTextContraseA,
            binding.editTextPeso,
            binding.editTextName
        )

        var todosValidos = true

        campos.forEach { campo ->
            val texto = campo.text.toString()
            if (texto.isEmpty()) {
                campo.error = "Este campo es obligatorio"
                todosValidos = false
            } else {
                campo.error = null
            }
        }

        return todosValidos
    }

}