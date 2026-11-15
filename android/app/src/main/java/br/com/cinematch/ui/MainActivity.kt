package br.com.cinematch.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import br.com.cinematch.data.*
import br.com.cinematch.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private var creatingAccount = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profile.clear()
        binding = ActivityMainBinding.inflate(layoutInflater)
        screen(binding.root, "Entre na sua conta")
        binding.authTabs.addOnButtonCheckedListener { _, id, checked ->
            if (checked) changeMode(id == binding.registerTab.id)
        }
        binding.start.setOnClickListener { authenticate() }
        lifecycleScope.launch {
            val movies = runCatching { Repository.api.movies() }.getOrNull().orEmpty()
            val posters = listOf(binding.heroLeft, binding.heroCenter, binding.heroRight)
            posters.forEachIndexed { index, view -> movies.getOrNull(index)?.let { view.poster(it) } }
        }
    }

    private fun changeMode(register: Boolean) {
        creatingAccount = register
        binding.nameLayout.visibility = if (register) View.VISIBLE else View.GONE
        binding.confirmLayout.visibility = if (register) View.VISIBLE else View.GONE
        binding.welcome.text = if (register) "Seu cinema começa aqui." else "Bom te ver por aqui."
        binding.subtitle.text = if (register) "Crie sua conta e encontre o seu próximo favorito." else "Entre para descobrir histórias que combinam com você."
        binding.start.text = if (register) "Criar minha conta" else "Entrar na minha conta"
        clearErrors()
        shell.errorBox.visibility = View.GONE
    }

    private fun clearErrors() {
        binding.nameLayout.error = null
        binding.usernameLayout.error = null
        binding.passwordLayout.error = null
        binding.confirmLayout.error = null
    }

    private fun authenticate() {
        clearErrors()
        val name = binding.name.text?.toString()?.trim().orEmpty()
        val username = binding.username.text?.toString()?.trim()?.lowercase().orEmpty()
        val password = binding.password.text?.toString().orEmpty()
        var valid = true
        if (creatingAccount && name.isBlank()) { binding.nameLayout.error = "Como podemos chamar você?"; valid = false }
        if (!username.matches(Regex("[a-z0-9_.-]{3,32}"))) { binding.usernameLayout.error = "Use de 3 a 32 letras, números, ponto, _ ou -."; valid = false }
        if (password.length < 8) { binding.passwordLayout.error = "A senha precisa ter pelo menos 8 caracteres."; valid = false }
        if (creatingAccount && password != binding.confirmPassword.text?.toString()) { binding.confirmLayout.error = "As senhas não são iguais."; valid = false }
        if (!valid) return
        val register = creatingAccount
        request {
            val auth = if (register) Repository.api.register(RegisterInput(name, username, password)) else Repository.api.login(LoginInput(username, password))
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@request
            profile.save(auth)
            binding.password.text?.clear()
            binding.confirmPassword.text?.clear()
            startActivity(Intent(this, if (auth.preferences_ready) DiscoveryActivity::class.java else PreferencesActivity::class.java))
            finish()
        }
    }
}
