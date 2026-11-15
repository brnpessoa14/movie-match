package br.com.cinematch.ui

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.cinematch.data.Repository
import br.com.cinematch.databinding.ActivityCatalogBinding

class CatalogActivity : BaseActivity() {
    private lateinit var binding: ActivityCatalogBinding
    private val adapter = MovieAdapter(details = { showMovie(this, it) })
    private var page = 1
    private var lastQuery = ""
    private var total = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCatalogBinding.inflate(layoutInflater)
        screen(binding.root, "Buscar filmes")
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter
        binding.search.setOnClickListener { search(1) }
        binding.query.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH) { search(1); true } else false
        }
        binding.previous.setOnClickListener { search(page - 1, lastQuery) }
        binding.next.setOnClickListener { search(page + 1, lastQuery) }
        savedInstanceState?.getString("query")?.takeIf { it.isNotBlank() }?.let {
            binding.query.setText(it)
            if (requireProfile()) search(savedInstanceState.getInt("page", 1), it)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("query", lastQuery)
        outState.putInt("page", page)
        super.onSaveInstanceState(outState)
    }

    private fun search(targetPage: Int, query: String = binding.query.text?.toString()?.trim().orEmpty()) {
        if (query.length < 2) { binding.queryLayout.error = "Digite pelo menos dois caracteres."; return }
        binding.queryLayout.error = null
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(binding.query.windowToken, 0)
        request {
            val result = Repository.api.search(query, targetPage)
            lastQuery = query
            page = result.page
            total = result.total_results
            adapter.submit(result.movies.map { MovieRow(it, "Encontrado na OMDb") })
            binding.resultCount.text = if (total == 0) "Nenhum filme encontrado. Tente outro título." else "$total resultados para “$query”"
            binding.page.text = "$page / ${((total + 9) / 10).coerceIn(1, 100)}"
            binding.pages.visibility = if (total > 10) View.VISIBLE else View.GONE
            binding.list.scrollToPosition(0)
        }
    }

    override fun afterRequest() {
        binding.previous.isEnabled = page > 1
        binding.next.isEnabled = page * 10 < total && page < 100
    }
}
