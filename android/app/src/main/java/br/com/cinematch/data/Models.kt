package br.com.cinematch.data

data class User(val id: Int, val name: String, val username: String? = null)
data class ProfileInput(val name: String, val username: String)
data class UserInput(val name: String)
data class LoginInput(val username: String, val password: String)
data class RegisterInput(val name: String, val username: String, val password: String)
data class AuthResult(val access_token: String, val user: User, val username: String, val preferences_ready: Boolean)
data class Genre(val id: Int, val name: String)
data class PreferencesInput(val genre_ids: List<Int>)
data class CastMember(val actor_name: String, val cast_order: Int)
data class Movie(
    val id: Int, val imdb_id: String?, val title: String, val year: Int,
    val runtime: Int?, val plot: String, val poster_url: String?,
    val imdb_rating: Double?, val rotten_tomatoes_rating: Int?,
    val genres: List<Genre>, val cast: List<CastMember>, val discovery_reason: String? = null
)
data class DiscoveryBatch(val movies: List<Movie>, val remaining: Int, val total: Int)
data class MovieSearchResult(val movies: List<Movie>, val total_results: Int, val page: Int, val cached: Boolean)
enum class EvaluationStatus(val label: String) {
    SEM_INTERESSE("Não me interessa"),
    NAO_GOSTEI("Não gostei"),
    QUERO_ASSISTIR("Quero assistir"),
    GOSTEI("Gostei")
}
data class EvaluationInput(val user_id: Int, val movie_id: Int, val status: EvaluationStatus)
data class EvaluationPatch(val status: EvaluationStatus)
data class Evaluation(val id: Int, val user_id: Int, val movie_id: Int, val status: EvaluationStatus, val movie: Movie)
data class Recommendation(val movie: Movie, val score: Double, val reason: String)
