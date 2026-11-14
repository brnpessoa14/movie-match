package br.com.cinematch.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.cinematch.R
import br.com.cinematch.data.*
import br.com.cinematch.databinding.ActivityListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HistoryActivity : BaseActivity() {
    private lateinit var binding: ActivityListBinding
    private var evaluations = emptyList<Evaluation>()
    private val statuses = listOf(EvaluationStatus.QUERO_ASSISTIR, EvaluationStatus.GOSTEI, EvaluationStatus.NAO_GOSTEI, EvaluationStatus.SEM_INTERESSE)
    private val adapter = MovieAdapter(details = { showMovie(this, it) }, edit = { edit(it) }, delete = { delete(it) })
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        screen(binding.root, "Sua história com o cinema")
        binding.heading.text = "Minha lista"
        binding.description.text = "Os que você quer ver. Os que ficaram na memória. Suas escolhas estão aqui."
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter
        binding.filter.visibility = View.VISIBLE
        binding.filter.adapter = ArrayAdapter(this, R.layout.item_status, listOf("Todas as avaliações") + statuses.map { it.label })
        binding.filter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { render() }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }
    override fun onResume() { super.onResume(); if (requireProfile()) request { reload() } }
    private suspend fun reload() { evaluations = Repository.api.history(profile.id); render() }
    private fun render() {
        val status = statuses.getOrNull(binding.filter.selectedItemPosition - 1)
        val filtered = evaluations.filter { status == null || it.status == status }.sortedBy { statuses.indexOf(it.status) }
        adapter.submit(filtered.map { MovieRow(it.movie, it.status.label, it) })
        binding.empty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }
    private fun edit(evaluation: Evaluation) {
        MaterialAlertDialogBuilder(this).setTitle("Como foi ${evaluation.movie.title}?")
            .setItems(statuses.map { it.label }.toTypedArray()) { _, which ->
                var saved = false
                request {
                    if (!saved) {
                        Repository.api.update(evaluation.id, profile.id, EvaluationPatch(statuses[which]))
                        saved = true
                    }
                    reload()
                    message("Avaliação atualizada.")
                }
            }.setNegativeButton("Cancelar", null).show()
    }
    private fun delete(evaluation: Evaluation) {
        MaterialAlertDialogBuilder(this).setTitle("Excluir avaliação?")
            .setMessage("${evaluation.movie.title} voltará a aparecer no catálogo.")
            .setNegativeButton("Cancelar", null).setPositiveButton("Excluir") { _, _ ->
                var deleted = false
                request {
                    if (!deleted) { Repository.deleteEvaluation(evaluation.id, profile.id); deleted = true }
                    reload()
                    message("Avaliação excluída.")
                }
            }.show()
    }
}
