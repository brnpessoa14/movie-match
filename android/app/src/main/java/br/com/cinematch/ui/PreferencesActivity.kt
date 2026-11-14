package br.com.cinematch.ui

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
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
                startActivity(Intent(this, DiscoveryActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
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
                    textSize = 17f
                    minHeight = (52 * resources.displayMetrics.density).toInt()
                    isChecked = genre.id in selected
                }
                checks[genre.id] = check
                binding.genres.addView(check)
            }
        }
    }
}
