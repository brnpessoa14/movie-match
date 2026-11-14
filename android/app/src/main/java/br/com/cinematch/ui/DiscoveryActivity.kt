package br.com.cinematch.ui

import android.os.Bundle
import android.view.View
import br.com.cinematch.data.*
import br.com.cinematch.databinding.ActivityDiscoveryBinding

class DiscoveryActivity : BaseActivity() {
    private lateinit var binding: ActivityDiscoveryBinding
    private var current: Movie? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiscoveryBinding.inflate(layoutInflater)
        screen(binding.root, "Descubra seu próximo favorito")
        val actions = listOf(binding.skip to EvaluationStatus.SEM_INTERESSE, binding.dislike to EvaluationStatus.NAO_GOSTEI,
            binding.watch to EvaluationStatus.QUERO_ASSISTIR, binding.like to EvaluationStatus.GOSTEI)
        actions.forEach { (button, status) ->
            button.setOnClickListener {
                val movie = current ?: return@setOnClickListener
                var saved = false
                request {
                    if (!saved) {
                        Repository.api.evaluate(EvaluationInput(profile.id, movie.id, status))
                        saved = true
                        current = null
                        binding.actions.visibility = View.GONE
                        message("Avaliação salva: ${status.label}")
                    }
                    // A tentativa seguinte só recarrega se a avaliação já foi confirmada.
                    display(Repository.api.discovery(profile.id).firstOrNull())
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (requireProfile()) load()
    }

    private fun load() {
        request { display(Repository.api.discovery(profile.id).firstOrNull()) }
    }

    private fun display(movie: Movie?) {
        current = movie
        binding.movie.root.visibility = if (movie == null) View.GONE else View.VISIBLE
        binding.actions.visibility = if (movie == null) View.GONE else View.VISIBLE
        binding.empty.visibility = if (movie == null) View.VISIBLE else View.GONE
        if (movie != null) bindMovie(this, binding.movie, movie)
        binding.scroll.scrollTo(0, 0)
    }
}
