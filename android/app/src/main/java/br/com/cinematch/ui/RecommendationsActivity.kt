package br.com.cinematch.ui

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.cinematch.data.Repository
import br.com.cinematch.databinding.ActivityListBinding

class RecommendationsActivity : BaseActivity() {
    private lateinit var binding: ActivityListBinding
    private val adapter = MovieAdapter(details = { showMovie(this, it) })
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        screen(binding.root, "Escolhidos para você")
        binding.heading.text = "A sua próxima sessão"
        binding.description.text = "Recomendações que acompanham o seu gosto. Toque em um filme para conhecer a história."
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter
    }
    override fun onResume() {
        super.onResume()
        if (!requireProfile()) return
        request {
            val results = Repository.api.recommendations(profile.id)
            adapter.submit(results.map { MovieRow(it.movie, it.reason) })
            binding.empty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
            binding.empty.text = "Todos os filmes já foram avaliados. Consulte a Minha lista para rever suas escolhas."
        }
    }
}
