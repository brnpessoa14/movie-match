package br.com.cinematch.ui

import android.app.Activity
import android.animation.ValueAnimator
import android.content.ActivityNotFoundException
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import br.com.cinematch.R
import br.com.cinematch.data.Movie
import br.com.cinematch.data.Profile
import br.com.cinematch.databinding.MovieCardBinding
import br.com.cinematch.databinding.DialogMovieBinding
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun initials(name: String) = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase().ifBlank { "CM" }
fun Movie.metadata() = "$year · ${runtime?.let { "$it min" } ?: "Duração N/D"}\n${genres.joinToString(" · ") { it.name }}"
fun Movie.ratings() = "IMDb ${imdb_rating?.toString() ?: "N/D"}  ·  RT ${rotten_tomatoes_rating?.let { "$it%" } ?: "N/D"}"
fun ImageView.poster(movie: Movie) {
    contentDescription = if (movie.poster_url == null) "Cartaz de ${movie.title} indisponível" else "Cartaz de ${movie.title}"
    load(movie.poster_url) { placeholder(R.drawable.poster_placeholder); error(R.drawable.poster_placeholder); crossfade(ValueAnimator.areAnimatorsEnabled()) }
}

fun bindMovie(activity: Activity, binding: MovieCardBinding, movie: Movie) {
    binding.poster.poster(movie)
    val parts = movie.title.split(": ", limit = 2)
    binding.title.text = parts.first()
    binding.subtitle.text = parts.getOrNull(1)
    binding.subtitle.visibility = if (parts.size > 1) View.VISIBLE else View.GONE
    binding.bannerEyebrow.visibility = if (activity.resources.configuration.screenHeightDp >= 700) View.VISIBLE else View.GONE
    val compact = activity.resources.configuration.screenHeightDp < 650
    val longTitle = compact && movie.title.length > 32
    binding.title.textSize = if (longTitle) 20f else 22f
    val posterFrame = binding.poster.parent as View
    posterFrame.layoutParams = posterFrame.layoutParams.apply {
        width = if (longTitle) (64 * activity.resources.displayMetrics.density).toInt() else activity.resources.getDimensionPixelSize(R.dimen.banner_poster_width)
        height = if (longTitle) (96 * activity.resources.displayMetrics.density).toInt() else activity.resources.getDimensionPixelSize(R.dimen.banner_poster_height)
    }
    binding.plot.maxLines = if (compact) 1 else activity.resources.getInteger(R.integer.synopsis_lines)
    binding.metadata.text = "${movie.year} · ${movie.runtime?.let { "$it min" } ?: "Duração N/D"}"
    binding.genres.text = movie.genres.joinToString(" · ") { it.name }
    binding.imdb.text = movie.ratings()
    binding.imdb.contentDescription = "IMDb ${movie.imdb_rating ?: "não disponível"}. Rotten Tomatoes ${movie.rotten_tomatoes_rating?.let { "$it por cento" } ?: "não disponível"}"
    binding.plot.text = movie.plot
    binding.expand.setOnClickListener { showMovie(activity, movie) }
}

fun showMovie(activity: Activity, movie: Movie) {
    val binding = DialogMovieBinding.inflate(LayoutInflater.from(activity))
    binding.title.text = movie.title
    binding.poster.poster(movie)
    binding.metadata.text = movie.metadata()
    binding.ratings.text = movie.ratings().replace("RT ", "Rotten Tomatoes ")
    binding.plot.text = movie.plot
    binding.cast.text = "Elenco\n" + movie.cast.joinToString(", ") { it.actor_name }.ifBlank { "Não disponível" }
    binding.share.setOnClickListener {
        val link = movie.imdb_id?.let { "\nhttps://www.imdb.com/title/$it/" }.orEmpty()
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "${movie.title} (${movie.year})\n${movie.plot}$link\nDescoberto no CineMatch")
        }
        try { Profile.externalFlow = true; activity.startActivity(Intent.createChooser(share, "Compartilhar filme")) }
        catch (_: ActivityNotFoundException) {
            Profile.externalFlow = false
            MaterialAlertDialogBuilder(activity).setMessage("Nenhum aplicativo de compartilhamento disponível.").setPositiveButton("Fechar", null).show()
        }
    }
    MaterialAlertDialogBuilder(activity).setView(binding.root).setPositiveButton("Fechar", null).show()
}
