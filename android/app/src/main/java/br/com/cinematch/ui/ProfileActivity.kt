package br.com.cinematch.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import br.com.cinematch.data.*
import br.com.cinematch.databinding.ActivityProfileBinding
import br.com.cinematch.databinding.DialogProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProfileActivity : BaseActivity() {
    private lateinit var binding: ActivityProfileBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        screen(binding.root, "Meu perfil")
        binding.editProfile.setOnClickListener { if (!isBusy) editProfile() }
        binding.genres.setOnClickListener {
            startActivity(Intent(this, PreferencesActivity::class.java).putExtra("from_profile", true))
        }
        binding.appearance.setOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle("Aparência")
                .setSingleChoiceItems(Appearance.labels, Appearance.selected(this)) { dialog, choice ->
                    dialog.dismiss()
                    Appearance.save(this, choice)
                }.setNegativeButton("Cancelar", null).show()
        }
        binding.accountInfo.setOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle("Sua conta")
                .setMessage("@${profile.username}\nConta nº ${profile.id}\n\nSua senha é protegida. Preferências e avaliações ficam vinculadas à sua conta. Ao sair do aplicativo, será necessário entrar novamente.")
                .setPositiveButton("Fechar", null).show()
        }
        binding.logout.setOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle("Sair da conta?").setMessage("Seus filmes e preferências continuam salvos.")
                .setNegativeButton("Cancelar", null).setPositiveButton("Sair") { _, _ ->
                    request { try { Repository.api.logout() } finally { profile.clear(); openLogin() } }
                }.show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!requireProfile()) return
        render()
        request {
            profile.update(Repository.api.user(profile.id))
            val genres = Repository.api.preferences(profile.id)
            binding.genres.text = "Meus gêneros · ${genres.size}"
            render()
        }
    }

    private fun render() {
        binding.name.text = profile.name
        binding.username.text = "@${profile.username}"
        binding.initials.text = initials(profile.name)
        binding.appearance.text = "Aparência · ${Appearance.labels[Appearance.selected(this)]}"
        refreshAvatar()
    }

    private fun editProfile() {
        val form = DialogProfileBinding.inflate(layoutInflater)
        form.editName.setText(profile.name)
        form.editUsername.setText(profile.username)
        val dialog = MaterialAlertDialogBuilder(this).setTitle("Meus dados").setView(form.root).setNegativeButton("Cancelar", null).create()
        dialog.show()
        form.saveProfile.setOnClickListener {
            if (isBusy) return@setOnClickListener
            val name = form.editName.text?.toString()?.trim().orEmpty()
            val username = form.editUsername.text?.toString()?.trim()?.lowercase().orEmpty()
            form.nameLayout.error = if (name.isBlank()) "Informe seu nome." else null
            form.usernameLayout.error = if (!username.matches(Regex("[a-z0-9_.-]{3,32}"))) "Use de 3 a 32 letras, números, ponto, _ ou -." else null
            if (form.nameLayout.error != null || form.usernameLayout.error != null) return@setOnClickListener
            form.error.visibility = View.GONE
            form.saveProfile.isEnabled = false
            form.editName.isEnabled = false
            form.editUsername.isEnabled = false
            dialog.setCancelable(false)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
            request(onError = {
                form.error.text = it
                form.error.visibility = View.VISIBLE
                form.saveProfile.isEnabled = true
                form.editName.isEnabled = true
                form.editUsername.isEnabled = true
                dialog.setCancelable(true)
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = true
            }) {
                profile.update(Repository.api.saveUser(profile.id, ProfileInput(name, username)))
                render()
                dialog.dismiss()
                message("Dados da conta atualizados.")
            }
        }
    }
}
