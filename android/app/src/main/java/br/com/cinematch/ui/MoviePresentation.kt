package br.com.cinematch.ui

import android.app.Activity
import android.content.res.Configuration
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import br.com.cinematch.R
import br.com.cinematch.data.Movie
import br.com.cinematch.databinding.MovieCardBinding
import kotlin.math.roundToInt

/** Ocupa a altura disponível com conteúdo legível; ações ficam fora da rolagem. */
fun fitMovie(activity: Activity, binding: MovieCardBinding, movie: Movie, width: Int, height: Int) {
    if (width <= 0 || height <= 0) return
    val resources = activity.resources
    val density = resources.displayMetrics.density
    fun dp(value: Int) = (value * density).roundToInt()
    fun TextView.size(resource: Int) = setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(resource))
    fun measure(): Int {
        binding.root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        return binding.root.measuredHeight
    }
    fun poster(posterWidth: Int) {
        val frame = binding.poster.parent as View
        frame.layoutParams = frame.layoutParams.apply { this.width = posterWidth; this.height = (posterWidth * 1.5f).roundToInt() }
    }
    val expanded = height >= dp(340) && resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT && resources.configuration.fontScale < 1.3f
    if (expanded) {
        binding.title.size(R.dimen.discovery_title_expanded)
        binding.subtitle.size(R.dimen.discovery_metadata_expanded)
        binding.metadata.size(R.dimen.discovery_metadata_expanded)
        binding.genres.size(R.dimen.discovery_secondary_expanded)
        binding.imdb.size(R.dimen.discovery_secondary_expanded)
        binding.rotten.size(R.dimen.discovery_secondary_expanded)
        binding.plot.size(R.dimen.discovery_plot_expanded)
        binding.storyHeading.size(R.dimen.discovery_section_expanded)
        binding.cast.size(R.dimen.discovery_cast_expanded)
        binding.plot.setLineSpacing(dp(4).toFloat(), 1f)
        binding.plot.maxLines = 12
        binding.cast.text = "Elenco · " + movie.cast.joinToString(", ") { it.actor_name }
        binding.cast.visibility = if (movie.cast.isNotEmpty()) View.VISIBLE else View.GONE
        binding.cast.maxLines = 3
        binding.imdb.text = "IMDb ${movie.imdb_rating ?: "N/D"}"
        binding.rotten.text = "Rotten Tomatoes ${movie.rotten_tomatoes_rating?.let { "$it%" } ?: "N/D"}"
        binding.rotten.visibility = View.VISIBLE
        poster(minOf(resources.getDimensionPixelSize(R.dimen.discovery_poster_expanded_max), ((width - dp(42)) * 0.38f).roundToInt()))
        while (measure() > height && binding.plot.maxLines > 3) binding.plot.maxLines--
        if (measure() > height) binding.cast.visibility = View.GONE
        while (measure() > height && binding.plot.maxLines > 1) binding.plot.maxLines--
    }
    if (!expanded || measure() > height) {
        // Composição compacta preservada para títulos longos, fontes ampliadas e horizontal.
        val compact = resources.configuration.screenHeightDp < 650
        val longTitle = compact && movie.title.length > 32
        binding.title.textSize = if (longTitle) 20f else 22f
        binding.subtitle.textSize = 13f
        binding.metadata.textSize = 13f
        binding.genres.textSize = 12f
        binding.imdb.textSize = 12f
        binding.imdb.text = movie.ratings()
        binding.rotten.visibility = View.GONE
        binding.plot.textSize = 14f
        binding.plot.setLineSpacing(dp(2).toFloat(), 1f)
        binding.plot.maxLines = if (compact) 1 else resources.getInteger(R.integer.synopsis_lines)
        binding.storyHeading.textSize = 14f
        binding.cast.visibility = View.GONE
        poster(if (longTitle) dp(64) else resources.getDimensionPixelSize(R.dimen.banner_poster_width))
    }
    binding.root.requestLayout()
}
