package br.com.cinematch.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import br.com.cinematch.R
import br.com.cinematch.data.Profile
import br.com.cinematch.data.Repository
import br.com.cinematch.databinding.ActivityBaseBinding
import android.widget.Toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import com.google.android.material.dialog.MaterialAlertDialogBuilder

abstract class BaseActivity : AppCompatActivity() {
    protected lateinit var shell: ActivityBaseBinding
    protected val profile by lazy { Profile(this) }
    private var busy = false
    private var retryAction: (() -> Unit)? = null
    private var feedbackToast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shell = ActivityBaseBinding.inflate(layoutInflater)
        setContentView(shell.root)
        ViewCompat.setOnApplyWindowInsetsListener(shell.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        shell.retry.setOnClickListener { retryAction?.invoke() }
        shell.toolbar.setOnMenuItemClickListener {
            if (busy) return@setOnMenuItemClickListener false
            MaterialAlertDialogBuilder(this).setTitle(profile.name)
                .setMessage("@${profile.username}\n\nSeu histórico fica salvo na sua conta.")
                .setNegativeButton("Continuar", null)
                .setPositiveButton("Sair da conta") { _, _ ->
                    request {
                        try { Repository.api.logout() }
                        finally { profile.clear(); openLogin() }
                    }
                }.show()
            true
        }
        val selected = when (this) {
            is DiscoveryActivity -> R.id.nav_discovery
            is RecommendationsActivity -> R.id.nav_recommendations
            is HistoryActivity -> R.id.nav_history
            else -> R.id.nav_preferences
        }
        shell.navigation.menu.findItem(selected).isChecked = true
        shell.navigation.setOnItemSelectedListener { item ->
            if (busy || item.itemId == selected) return@setOnItemSelectedListener false
            val destination = when (item.itemId) {
                R.id.nav_discovery -> DiscoveryActivity::class.java
                R.id.nav_recommendations -> RecommendationsActivity::class.java
                R.id.nav_history -> HistoryActivity::class.java
                else -> PreferencesActivity::class.java
            }
            startActivity(Intent(this, destination).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
            false
        }
    }

    protected fun screen(view: View, subtitle: String) {
        shell.content.addView(view)
        shell.toolbar.contentDescription = "CineMatch · $subtitle"
        if (this is MainActivity) shell.toolbar.visibility = View.GONE
        shell.navContainer.visibility = if (this is MainActivity || !profile.ready) View.GONE else View.VISIBLE
    }

    protected fun requireProfile(): Boolean {
        if (profile.id != 0) return true
        openLogin()
        return false
    }

    private fun openLogin() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    override fun onStart() {
        super.onStart()
        if (this !is MainActivity && profile.id == 0) shell.content.visibility = View.INVISIBLE
    }

    override fun onResume() {
        super.onResume()
        Profile.externalFlow = false
        if (this !is MainActivity) requireProfile()
    }

    protected fun message(text: String) {
        feedbackToast?.cancel()
        feedbackToast = Toast.makeText(this, text, Toast.LENGTH_SHORT).also { it.show() }
    }

    override fun onStop() {
        feedbackToast?.cancel()
        super.onStop()
    }

    // LifecycleScope cancela atualizações quando a Activity é destruída.
    protected fun request(action: suspend () -> Unit) {
        if (busy) return
        retryAction = { request(action) }
        busy = true
        shell.errorBox.visibility = View.GONE
        shell.loading.visibility = View.VISIBLE
        enableActions(shell.content, false)
        lifecycleScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (error is HttpException && error.code() == 401 && this@BaseActivity !is MainActivity) {
                    profile.clear()
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) openLogin()
                    return@launch
                }
                shell.errorText.text = when (error) {
                    is HttpException -> {
                        val detail = runCatching { JSONObject(error.response()?.errorBody()?.string().orEmpty()).optString("detail") }.getOrNull()
                        when {
                            error.code() == 404 -> "Este item não está disponível. Atualize a tela e tente novamente."
                            !detail.isNullOrBlank() && !detail.startsWith("[") -> detail
                            else -> "A API não concluiu a operação (HTTP ${error.code()}). Tente novamente."
                        }
                    }
                    is IOException -> "Não foi possível conectar à API. Verifique se o backend está ligado e tente novamente."
                    else -> "Não foi possível concluir a operação. Tente novamente."
                }
                shell.errorBox.visibility = View.VISIBLE
            } finally {
                busy = false
                shell.loading.visibility = View.INVISIBLE
                enableActions(shell.content, true)
            }
        }
    }

    private fun enableActions(view: View, enabled: Boolean) {
        if (view is android.widget.Button || view is android.widget.EditText || view is android.widget.Spinner) view.isEnabled = enabled
        if (view is ViewGroup) for (i in 0 until view.childCount) enableActions(view.getChildAt(i), enabled)
    }
}
