package br.com.cinematch.ui

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.doOnLayout
import androidx.core.content.ContextCompat
import br.com.cinematch.R
import br.com.cinematch.data.*
import br.com.cinematch.databinding.ActivityDiscoveryBinding

class DiscoveryActivity : BaseActivity() {
    private lateinit var binding: ActivityDiscoveryBinding
    private var current: Movie? = null
    private var remaining = emptyList<Movie>()
    private var seen = false
    private var remainingCount = 0
    private var pendingRefresh = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiscoveryBinding.inflate(layoutInflater)
        screen(binding.root, "Descobrir")
        binding.introEyebrow.visibility = if (resources.configuration.screenHeightDp < 650) View.GONE else View.VISIBLE
        binding.scroll.addOnLayoutChangeListener { view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                current?.let { fitMovie(this, binding.movie, it, view.width, view.height) }
            }
        }
        contextualError = binding.actionError
        contextualRetry = binding.actionRetry
        seen = savedInstanceState?.getBoolean("seen") ?: false
        binding.viewingMode.check(if (seen) R.id.seen else R.id.unseen)
        binding.viewingMode.addOnButtonCheckedListener { _, id, checked ->
            if (checked) { seen = id == R.id.seen; renderActions() }
        }
        binding.primaryAction.setOnClickListener { rate(if (seen) EvaluationStatus.GOSTEI else EvaluationStatus.QUERO_ASSISTIR) }
        binding.secondaryAction.setOnClickListener { rate(if (seen) EvaluationStatus.NAO_GOSTEI else EvaluationStatus.SEM_INTERESSE) }
        renderActions()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("seen", seen)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        if (requireProfile()) request {
            refresh()
        }
    }

    private fun rate(status: EvaluationStatus) {
        val movie = current ?: return
        if (isBusy) return
        var saved = false
        request {
            if (!saved) {
                Repository.api.evaluate(EvaluationInput(profile.id, movie.id, status))
                saved = true
                pendingRefresh = true
            }
            refresh()
            seen = false
            binding.viewingMode.check(R.id.unseen)
            renderActions()
            if (ValueAnimator.areAnimatorsEnabled()) {
                binding.movie.root.alpha = 0.65f
                binding.movie.root.animate().alpha(1f).setDuration(160).start()
            }
            message("${status.label}. ${current?.title ?: "Fim do catálogo."}")
        }
    }

    private suspend fun refresh() {
        val batch = Repository.discovery(profile.id)
        remaining = batch.movies
        remainingCount = batch.remaining
        pendingRefresh = false
        display(remaining.firstOrNull())
    }

    override fun afterRequest() {
        binding.primaryAction.isEnabled = !pendingRefresh
        binding.secondaryAction.isEnabled = !pendingRefresh
        binding.unseen.isEnabled = !pendingRefresh
        binding.seen.isEnabled = !pendingRefresh
    }

    private fun renderActions() {
        binding.primaryAction.text = if (seen) "Gostei" else "Quero assistir"
        binding.primaryAction.setIconResource(if (seen) R.drawable.ic_like else R.drawable.ic_watch)
        binding.primaryAction.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, if (seen) R.color.surface_raised else R.color.primary))
        val foreground = ContextCompat.getColor(this, if (seen) R.color.text else R.color.on_primary)
        binding.primaryAction.setTextColor(foreground)
        binding.primaryAction.iconTint = ColorStateList.valueOf(foreground)
        binding.secondaryAction.text = if (seen) "Não gostei" else "Não me interessa"
    }

    private fun display(movie: Movie?) {
        current = movie
        binding.counter.text = "$remainingCount filmes"
        binding.movie.root.visibility = if (movie == null) View.GONE else View.VISIBLE
        binding.actions.visibility = if (movie == null) View.GONE else View.VISIBLE
        binding.empty.visibility = if (movie == null) View.VISIBLE else View.GONE
        if (movie != null) {
            binding.introEyebrow.text = movie.discovery_reason ?: getString(R.string.banner_activity_discovery_3)
            binding.introEyebrow.textSize = 12f
            binding.introEyebrow.letterSpacing = 0f
            bindMovie(this, binding.movie, movie)
            binding.scroll.doOnLayout { fitMovie(this, binding.movie, movie, it.width, it.height) }
        }
        binding.scroll.scrollTo(0, 0)
    }
}
