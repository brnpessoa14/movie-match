package br.com.cinematch.data

import br.com.cinematch.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface Api {
    @POST("auth/login") suspend fun login(@Body input: LoginInput): AuthResult
    @POST("auth/register") suspend fun register(@Body input: RegisterInput): AuthResult
    @POST("auth/logout") suspend fun logout(): retrofit2.Response<Unit>
    @GET("movies") suspend fun movies(): List<Movie>
    @GET("users/{id}") suspend fun user(@Path("id") id: Int): User
    @PATCH("users/{id}") suspend fun saveUser(@Path("id") id: Int, @Body input: ProfileInput): User
    @GET("genres") suspend fun genres(): List<Genre>
    @GET("users/{id}/preferences") suspend fun preferences(@Path("id") id: Int): List<Genre>
    @PUT("users/{id}/preferences") suspend fun savePreferences(@Path("id") id: Int, @Body input: PreferencesInput): List<Genre>
    @GET("movies/discovery") suspend fun discovery(@Query("user_id") userId: Int): List<Movie>
    @POST("evaluations") suspend fun evaluate(@Body input: EvaluationInput): Evaluation
    @GET("users/{id}/evaluations") suspend fun history(@Path("id") id: Int): List<Evaluation>
    @PATCH("evaluations/{id}") suspend fun update(@Path("id") id: Int, @Query("user_id") userId: Int, @Body input: EvaluationPatch): Evaluation
    @DELETE("evaluations/{id}") suspend fun delete(@Path("id") id: Int, @Query("user_id") userId: Int): retrofit2.Response<Unit>
    @GET("users/{id}/recommendations") suspend fun recommendations(@Path("id") id: Int): List<Recommendation>
}

object Repository {
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder().apply {
                Profile.accessToken?.let { header("Authorization", "Bearer $it") }
            }.build()
            chain.proceed(request)
        }
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS).build()
    val api: Api = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL)
        .client(client).addConverterFactory(GsonConverterFactory.create()).build().create(Api::class.java)

    suspend fun deleteEvaluation(id: Int, userId: Int) {
        val response = api.delete(id, userId)
        if (!response.isSuccessful) throw retrofit2.HttpException(response)
    }
}
