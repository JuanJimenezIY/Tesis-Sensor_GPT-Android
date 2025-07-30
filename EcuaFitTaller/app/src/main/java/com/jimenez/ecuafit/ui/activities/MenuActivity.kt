package com.jimenez.ecuafit.ui.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI

import com.google.firebase.firestore.FirebaseFirestore
import com.jimenez.ecuafit.R
import com.jimenez.ecuafit.databinding.ActivityMenuBinding
import com.jimenez.ecuafit.ui.fragments.DiarioFragment
import com.jimenez.ecuafit.ui.fragments.InformeFragment
import com.jimenez.ecuafit.ui.fragments.InicioFragment
import com.jimenez.ecuafit.ui.utilities.FragmentsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class MenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMenuBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FragmentsManager().replaceFragment(
            supportFragmentManager,binding.frmContainer.id,InicioFragment()
        )


    }
    override fun onStart() {
        super.onStart()

        initClass()

    }

    private fun initClass(){

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.inicio -> {

                    FragmentsManager().replaceFragment(
                        supportFragmentManager,binding.frmContainer.id,InicioFragment()
                    )
                    true
                }
                R.id.diario -> {

                    FragmentsManager().replaceFragment(
                        supportFragmentManager,binding.frmContainer.id,DiarioFragment()
                    )

                    true
                }
                R.id.informe -> {

                    FragmentsManager().replaceFragment(
                        supportFragmentManager,binding.frmContainer.id,InformeFragment()
                    )

                    true
                }
                else -> false

            }



        }

    }

}