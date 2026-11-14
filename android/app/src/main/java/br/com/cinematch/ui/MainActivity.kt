package br.com.cinematch.ui

import android.content.Intent
import android.os.Bundle
import br.com.cinematch.data.Repository
import br.com.cinematch.data.UserInput
import br.com.cinematch.databinding.ActivityMainBinding
import retrofit2.HttpException

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        screen(binding.root, "O próximo filme é seu")
        binding.start.setOnClickListener {
            val name = binding.name.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                binding.nameLayout.error = "Informe seu nome para começar."
            } else {
                binding.nameLayout.error = null
                request {
                    // Em retry, reaproveita o ID se a API já confirmou o cadastro.
                    if (profile.id == 0) profile.save(Repository.api.createUser(UserInput(name)))
                    openProfile()
                }
            }
        }
        if (profile.id != 0) request {
            try {
                Repository.api.user(profile.id)
                openProfile()
            } catch (error: HttpException) {
                if (error.code() != 404) throw error
                profile.clear()
                message("O perfil anterior não está nesta base. Informe seu nome para criar um perfil.")
            }
        }
    }

    private fun openProfile() {
        startActivity(Intent(this, if (profile.ready) DiscoveryActivity::class.java else PreferencesActivity::class.java))
        finish()
    }
}
