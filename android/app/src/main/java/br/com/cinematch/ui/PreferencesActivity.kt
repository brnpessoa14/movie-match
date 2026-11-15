package br.com.cinematch.ui

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.GridLayout
import androidx.core.content.ContextCompat
import br.com.cinematch.R
import br.com.cinematch.data.PreferencesInput
import br.com.cinematch.data.Repository
import br.com.cinematch.databinding.ActivityPreferencesBinding
import com.google.android.material.checkbox.MaterialCheckBox

class PreferencesActivity : BaseActivity() {
    private lateinit var binding: ActivityPreferencesBinding
    private val checks = mutableMapOf<Int, CheckBox>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        screen(binding.root, "Seu gosto, suas regras")
        if (!requireProfile()) return
        binding.save.setOnClickListener {
            if (checks.isEmpty()) return@setOnClickListener
            request {
                Repository.api.savePreferences(profile.id, PreferencesInput(checks.filterValues { it.isChecked }.keys.toList()))
                profile.complete()
                if (!intent.getBooleanExtra("from_profile", false)) {
                    startActivity(Intent(this, DiscoveryActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                }
                finish()
            }
        }
        request {
            val genres = Repository.api.genres()
            val selected = Repository.api.preferences(profile.id).map { it.id }.toSet()
            binding.genres.removeAllViews()
            checks.clear()
            for (genre in genres) {
                val check = MaterialCheckBox(this).apply {
                    text = genre.name
                    setTextColor(ContextCompat.getColor(this@PreferencesActivity, R.color.text))
                    textSize = 14f
                    minHeight = (72 * resources.displayMetrics.density).toInt()
                    val pad = (10 * resources.displayMetrics.density).toInt()
                    setPadding(pad, pad, pad, pad)
                    setBackgroundResource(R.drawable.genre_selected)
                    buttonTintList = ContextCompat.getColorStateList(this@PreferencesActivity, R.color.nav_tint)
                    isChecked = genre.id in selected
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = GridLayout.LayoutParams.WRAP_CONTENT
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        val gap = (5 * resources.displayMetrics.density).toInt()
                        setMargins(gap, gap, gap, gap)
                    }
                    setOnCheckedChangeListener { _, _ -> updateCount() }
                }
                checks[genre.id] = check
                binding.genres.addView(check)
            }
            updateCount()
        }
    }

    private fun updateCount() {
        val count = checks.values.count { it.isChecked }
        binding.selectionCount.text = when (count) {
            0 -> "Escolha quantos gêneros quiser"
            1 -> "1 gênero selecionado"
            else -> "$count gêneros selecionados"
        }
    }
}
