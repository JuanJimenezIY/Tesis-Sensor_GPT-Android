package com.jimenez.ecuafit.ui.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.health.connect.HealthPermissions
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jimenez.ecuafit.databinding.ActivityMainBinding
import com.jimenez.ecuafit.ui.utilities.SessionManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter

import com.jimenez.ecuafit.logic.UsuarioLogicFire

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "UCE"
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPref = getSharedPreferences("sesion", Context.MODE_PRIVATE)


        val permissions = setOf(
            HealthPermissions.READ_STEPS,
            HealthPermissions.READ_HEART_RATE,
            HealthPermissions.READ_SLEEP,
            HealthPermissions.READ_BODY_FAT

        )

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    override fun onStart() {
        super.onStart()
        initClass()
    }

    @SuppressLint("ResourceAsColor")
    private fun initClass() {
        if (SessionManager.validarSesion(sharedPref)) {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogin.setOnClickListener {
            if (binding.txtName.text.isNotEmpty() && binding.txtPassword.text.isNotEmpty()) {
                logIn(binding.txtName.text.toString(), binding.txtPassword.text.toString())
            } else {
                Snackbar.make(binding.txtPassword, "Ingrese todos los campos", Snackbar.LENGTH_SHORT).show()
            }
            binding.btnLogin.isEnabled = true
        }

        val appResultLocal = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultActivity ->
            val sn = Snackbar.make(binding.imageView, "", Snackbar.LENGTH_SHORT)
            var message = ""
            when (resultActivity.resultCode) {
                RESULT_OK -> message = resultActivity.data?.getStringExtra("result").orEmpty()
                RESULT_CANCELED -> message = "Error en el registro"
                else -> message = "Registro dudoso"
            }
            sn.setText(message)
            sn.show()
        }

        binding.registro.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            appResultLocal.launch(intent)
        }
    }

    private fun logIn(email: String, password: String) {
        val con = db.collection("users").document(email).get()
        con.addOnSuccessListener {
            if (con.result.data != null) {
                if (SessionManager.comprobar(password, it.getString("contraseña"))) {
                    SessionManager.guardarSesion(sharedPref)
                    lifecycleScope.launch(Dispatchers.Main) {
                        withContext(Dispatchers.IO) {
                            UsuarioLogicFire().recuperarUsuario(email)
                        }
                    }
                    val intent = Intent(this, MenuActivity::class.java)
                    startActivity(intent)
                } else {
                    Snackbar.make(binding.txtPassword, "Usuario o contraseña incorrectas", Snackbar.LENGTH_SHORT).show()
                }
            } else {
                Snackbar.make(binding.txtPassword, "Usuario no existe", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}
