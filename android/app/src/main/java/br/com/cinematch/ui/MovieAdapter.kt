package br.com.cinematch.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.cinematch.data.Evaluation
import br.com.cinematch.data.Movie
import br.com.cinematch.databinding.ItemMovieBinding

data class MovieRow(val movie: Movie, val description: String, val evaluation: Evaluation? = null)

class MovieAdapter(
    private val details: (Movie) -> Unit,
    private val edit: (Evaluation) -> Unit = {},
    private val delete: (Evaluation) -> Unit = {}
) : RecyclerView.Adapter<MovieAdapter.Holder>() {
    private var rows = emptyList<MovieRow>()
    fun submit(items: List<MovieRow>) { rows = items; notifyDataSetChanged() }
    class Holder(val binding: ItemMovieBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = rows.size
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        holder.binding.apply {
            poster.poster(row.movie)
            title.text = row.movie.title
            metadata.text = row.movie.metadata()
            ratings.text = row.movie.ratings()
            reason.text = row.description
            details.setOnClickListener { this@MovieAdapter.details(row.movie) }
            editActions.visibility = if (row.evaluation == null) View.GONE else View.VISIBLE
            edit.setOnClickListener { row.evaluation?.let(this@MovieAdapter.edit) }
            delete.setOnClickListener { row.evaluation?.let(this@MovieAdapter.delete) }
        }
    }
}
