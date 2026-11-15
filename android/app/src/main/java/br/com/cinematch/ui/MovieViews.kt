package br.com.cinematch.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import br.com.cinematch.R
import br.com.cinematch.data.Movie
import br.com.cinematch.data.Profile
import br.com.cinematch.databinding.MovieCardBinding
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun Movie.metadata() = "$year · ${runtime?.let { "$it min" } ?: "Duração N/D"}\n${genres.joinToString(" · ") { it.name }}"
fun Movie.ratings() = "IMDb ${imdb_rating?.toString() ?: "N/D"}  ·  Rotten Tomatoes ${rotten_tomatoes_rating?.let { "$it%" } ?: "N/D"}"
fun ImageView.poster(movie: Movie) {
    contentDescription = if (movie.poster_url == null) "Ilustração de cinema; cartaz de ${movie.title} indisponível" else "Cartaz de ${movie.title}"
    load(movie.poster_url) { placeholder(R.drawable.poster_placeholder); error(R.drawable.poster_placeholder); crossfade(true) }
}

fun bindMovie(activity: Activity, binding: MovieCardBinding, movie: Movie) {
    binding.poster.poster(movie)
    binding.backdrop.poster(movie)
    binding.title.text = movie.title
    binding.metadata.text = "${movie.year}  ·  ${movie.runtime?.let { "$it min" } ?: "Duração N/D"}"
    binding.genres.text = movie.genres.joinToString("  /  ") { it.name }
    binding.imdb.text = "IMDb  ${movie.imdb_rating ?: "N/D"}"
    binding.rotten.text = "Rotten Tomatoes  ${movie.rotten_tomatoes_rating?.let { "$it%" } ?: "N/D"}"
    binding.plot.text = movie.plot
    binding.plot.maxLines = 3
    binding.expand.text = "Ler sinopse completa"
    binding.expand.setOnClickListener {
        val expand = binding.plot.maxLines == 3
        binding.plot.maxLines = if (expand) Int.MAX_VALUE else 3
        binding.expand.text = if (expand) "Recolher sinopse" else "Ler sinopse completa"
    }
    binding.cast.text = "Elenco: " + movie.cast.joinToString(", ") { it.actor_name }.ifBlank { "N/D" }
    binding.share.setOnClickListener {
        val link = movie.imdb_id?.let { "\nhttps://www.imdb.com/title/$it/" }.orEmpty()
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "${movie.title} (${movie.year})\n${movie.plot}$link\nDescoberto no CineMatch")
        }
        try { Profile.externalFlow = true; activity.startActivity(Intent.createChooser(share, "Compartilhar filme")) }
        catch (_: ActivityNotFoundException) { Profile.externalFlow = false; Toast.makeText(activity, "Nenhum aplicativo de compartilhamento disponível.", Toast.LENGTH_LONG).show() }
    }
}

fun showMovie(activity: Activity, movie: Movie) {
    val binding = MovieCardBinding.inflate(LayoutInflater.from(activity))
    bindMovie(activity, binding, movie)
    val scroll = android.widget.ScrollView(activity).apply { addView(binding.root) }
    MaterialAlertDialogBuilder(activity).setView(scroll).setPositiveButton("Fechar", null).show()
}
