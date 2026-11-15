package br.com.cinematch.data

import android.content.Context

/** Sessão somente em memória. IDs antigos salvos no dispositivo são ignorados. */
class Profile(@Suppress("UNUSED_PARAMETER") context: Context) {
    val id: Int get() = current?.user?.id ?: 0
    val ready: Boolean get() = current?.preferences_ready ?: false
    val name: String get() = current?.user?.name.orEmpty()
    val username: String get() = current?.username.orEmpty()
    fun save(auth: AuthResult) { current = auth }
    fun complete() { current = current?.copy(preferences_ready = true) }
    fun clear() = clearSession()

    companion object {
        @Volatile private var current: AuthResult? = null
        var externalFlow = false
        val accessToken: String? get() = current?.access_token
        fun clearSession() { current = null }
    }
}
