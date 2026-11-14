package br.com.cinematch.data

data class User(val id: Int, val name: String)
data class UserInput(val name: String)
data class Genre(val id: Int, val name: String)
data class PreferencesInput(val genre_ids: List<Int>)
data class CastMember(val actor_name: String, val cast_order: Int)
data class Movie(
    val id: Int, val imdb_id: String?, val title: String, val year: Int,
    val runtime: Int?, val plot: String, val poster_url: String?,
    val imdb_rating: Double?, val rotten_tomatoes_rating: Int?,
    val genres: List<Genre>, val cast: List<CastMember>
)
enum class EvaluationStatus(val label: String) {
    SEM_INTERESSE("Não assisti e não me interessei"),
    NAO_GOSTEI("Assisti e não gostei"),
    QUERO_ASSISTIR("Me interessei e quero assistir"),
    GOSTEI("Assisti e gostei")
}
data class EvaluationInput(val user_id: Int, val movie_id: Int, val status: EvaluationStatus)
data class EvaluationPatch(val status: EvaluationStatus)
data class Evaluation(val id: Int, val user_id: Int, val movie_id: Int, val status: EvaluationStatus, val movie: Movie)
data class Recommendation(val movie: Movie, val score: Double, val reason: String)
