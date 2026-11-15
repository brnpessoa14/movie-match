package br.com.cinematch

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.recyclerview.widget.RecyclerView
import br.com.cinematch.data.*
import br.com.cinematch.ui.MainActivity
import br.com.cinematch.ui.DiscoveryActivity
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import android.graphics.drawable.BitmapDrawable
import coil.drawable.CrossfadeDrawable

/** Teste real: exige backend ligado em 10.0.2.2:8000. Não usa API simulada. */
@RunWith(AndroidJUnit4::class)
class CineMatchFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private fun waitScreen(id: Int) {
        val until = System.currentTimeMillis() + 25000
        var failure: Throwable? = null
        while (System.currentTimeMillis() < until) {
            try {
                if (id == R.id.username) onView(withId(id)).perform(scrollTo())
                onView(withId(id)).check(matches(isCompletelyDisplayed()))
                onView(withId(R.id.loading)).check(matches(withEffectiveVisibility(Visibility.INVISIBLE)))
                return
            } catch (error: Throwable) { failure = error; Thread.sleep(150) }
        }
        throw AssertionError("Tela não carregou: $id", failure)
    }

    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        Thread.sleep(350) // Aguarda a apresentação do frame após layout/transição.
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?.let { File(it).apply { mkdirs() } } ?: context.getExternalFilesDir(null)
        FileOutputStream(File(output, "$name.png")).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    private fun waitForChooser() {
        val until = System.currentTimeMillis() + 10000
        while (System.currentTimeMillis() < until) {
            val activePackage = instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString()
            if (activePackage == "com.android.intentresolver" || activePackage == "android") return
            Thread.sleep(100)
        }
        throw AssertionError("O seletor de compartilhamento não ficou visível.")
    }

    private fun filter(label: String) {
        onView(withId(R.id.filter)).perform(click())
        onData(org.hamcrest.Matchers.equalTo(label)).perform(click())
    }

    private fun fill(id: Int, text: String) {
        onView(withId(id)).perform(scrollTo(), replaceText(text), androidx.test.espresso.action.ViewActions.closeSoftKeyboard())
    }

    private fun login(username: String, password: String) {
        waitScreen(R.id.username)
        fill(R.id.username, username)
        fill(R.id.password, password)
        onView(withId(R.id.start)).perform(scrollTo(), click())
    }

    private fun logout() {
        onView(withId(R.id.nav_profile)).perform(click())
        waitScreen(R.id.edit_profile)
        onView(withId(R.id.logout)).perform(scrollTo(), click())
        onView(withText("Sair")).inRoot(isDialog()).perform(click())
        waitScreen(R.id.username)
        assertEquals(0, Profile(context).id)
    }

    private fun waitForPoster() {
        val until = System.currentTimeMillis() + 12000
        while (System.currentTimeMillis() < until) {
            try {
                onView(withId(R.id.hero_center)).check { view, _ ->
                    val drawable = (view as android.widget.ImageView).drawable
                    assertTrue(drawable is BitmapDrawable || drawable is CrossfadeDrawable)
                }
                return
            } catch (_: Throwable) { Thread.sleep(200) }
        }
    }

    private fun childClick(id: Int) = object : androidx.test.espresso.ViewAction {
        override fun getConstraints() = isAssignableFrom(View::class.java)
        override fun getDescription() = "Clicar no controle filho $id"
        override fun perform(controller: androidx.test.espresso.UiController, view: View) {
            view.findViewById<View>(id).performClick()
            controller.loopMainThreadUntilIdle()
        }
    }

    @Test fun fullFlowWithRealBackend() {
        // A sessão antiga do protótipo não pode permitir entrada automática.
        context.getSharedPreferences("profile", Context.MODE_PRIVATE).edit().putInt("user_id", 7).putBoolean("ready", true).commit()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        waitScreen(R.id.username)
        assertEquals(0, Profile(context).id)
        waitForPoster()
        screenshot("01-inicio")
        val username = "ui_" + System.currentTimeMillis().toString().takeLast(10)
        val password = "CinemaSenha123!"
        onView(withId(R.id.register_tab)).perform(scrollTo(), click())
        fill(R.id.name, "Pessoa de teste")
        fill(R.id.username, username)
        fill(R.id.password, password)
        fill(R.id.confirm_password, password)
        screenshot("07-cadastro")
        onView(withId(R.id.start)).perform(scrollTo(), click())
        waitScreen(R.id.save)
        screenshot("02-generos")
        onView(withText("Ficção científica")).perform(scrollTo(), click())
        onView(withId(R.id.save)).perform(click())
        waitScreen(R.id.primary_action)
        screenshot("03-descobrir")
        val uid = Profile(context).id
        assertTrue(uid > 0)
        assertEquals("Ficção científica", runBlocking { Repository.api.preferences(uid) }.single().name)
        val buttons = listOf(false to R.id.secondary_action, true to R.id.secondary_action, false to R.id.primary_action, true to R.id.primary_action)
        buttons.forEachIndexed { index, (seen, button) ->
            onView(withId(if (seen) R.id.seen else R.id.unseen)).perform(click())
            assertEquals(index, runBlocking { Repository.api.history(uid) }.size)
            onView(withId(button)).perform(click())
            waitScreen(R.id.primary_action)
            assertEquals(index + 1, runBlocking { Repository.api.history(uid) }.size)
        }
        val history = runBlocking { Repository.api.history(uid) }
        assertEquals(EvaluationStatus.entries.toSet(), history.map { it.status }.toSet())
        val recommended = runBlocking { Repository.api.recommendations(uid) }
        assertTrue(recommended.none { r -> history.any { it.movie_id == r.movie.id } })

        onView(withId(R.id.nav_recommendations)).perform(click())
        waitScreen(R.id.list)
        screenshot("04-recomendacoes")
        onView(withId(R.id.list)).perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, childClick(R.id.details)))
        onView(withId(R.id.share)).inRoot(isDialog()).perform(scrollTo())
        Intents.init()
        try {
            onView(withId(R.id.share)).inRoot(isDialog()).perform(click())
            Intents.intended(hasAction(Intent.ACTION_CHOOSER))
            waitForChooser()
            screenshot("06-compartilhar")
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        } finally { Intents.release() }
        onView(withText("Fechar")).inRoot(isDialog()).perform(click())

        onView(withId(R.id.nav_history)).perform(click())
        waitScreen(R.id.filter)
        filter(EvaluationStatus.QUERO_ASSISTIR.label)
        screenshot("05-minha-lista")
        val watch = history.single { it.status == EvaluationStatus.QUERO_ASSISTIR }
        onView(withId(R.id.list)).perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, childClick(R.id.edit)))
        onView(withText(EvaluationStatus.GOSTEI.label)).inRoot(isDialog()).perform(click())
        waitScreen(R.id.filter)
        assertEquals(EvaluationStatus.GOSTEI, runBlocking { Repository.api.history(uid) }.single { it.id == watch.id }.status)
        filter("Todas as avaliações")
        onView(withId(R.id.list)).perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, childClick(R.id.delete)))
        onView(allOf(withText("Excluir"), withId(android.R.id.button1))).inRoot(isDialog()).perform(click())
        waitScreen(R.id.filter)
        assertEquals(3, runBlocking { Repository.api.history(uid) }.size)
        onView(withId(R.id.nav_profile)).perform(click())
        waitScreen(R.id.genres)
        onView(withId(R.id.genres)).perform(scrollTo(), click())
        waitScreen(R.id.save)
        onView(withText("Comédia")).perform(scrollTo(), click())
        onView(withId(R.id.save)).perform(click())
        waitScreen(R.id.genres)
        assertTrue(runBlocking { Repository.api.preferences(uid) }.any { it.name == "Comédia" })
        logout()
        login(username, "SenhaIncorreta123!")
        waitScreen(R.id.error_text)
        login(username, password)
        waitScreen(R.id.primary_action)
        assertEquals(uid, Profile(context).id)
        assertEquals(3, runBlocking { Repository.api.history(uid) }.size)
        logout()
        onView(withId(R.id.register_tab)).perform(scrollTo(), click())
        fill(R.id.name, "Outra pessoa")
        fill(R.id.username, username + "_b")
        fill(R.id.password, password)
        fill(R.id.confirm_password, password)
        onView(withId(R.id.start)).perform(scrollTo(), click())
        waitScreen(R.id.save)
        assertTrue(Profile(context).id != uid)
        assertTrue(runBlocking { Repository.api.history(Profile(context).id) }.isEmpty())
        onView(withId(R.id.save)).perform(click())
        waitScreen(R.id.primary_action)
        logout()
        scenario.close()
        ActivityScenario.launch(MainActivity::class.java).use {
            waitScreen(R.id.username)
            assertEquals(0, Profile(context).id)
            login(username, password)
            waitScreen(R.id.primary_action)
            assertEquals(uid, Profile(context).id)
            assertEquals(3, runBlocking { Repository.api.history(uid) }.size)
            // Sair para a tela inicial e abrir o app novamente exige credenciais.
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_HOME)
            val until = System.currentTimeMillis() + 5000
            while (Profile(context).id != 0 && System.currentTimeMillis() < until) Thread.sleep(100)
            assertEquals(0, Profile(context).id)
            context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            waitScreen(R.id.username)
            screenshot("08-login-ao-reabrir")
        }
    }

    @Test fun allCatalogTitlesRemainReadable() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        waitScreen(R.id.username)
        val auth = runBlocking { Repository.api.register(RegisterInput("Teste de títulos", "titles_" + System.currentTimeMillis().toString().takeLast(10), "CinemaSenha123!")) }
        Profile(context).save(auth)
        runBlocking { Repository.api.savePreferences(auth.user.id, PreferencesInput(emptyList())) }
        Profile(context).complete()
        val catalog = runBlocking { Repository.api.discovery(auth.user.id) }
        scenario.onActivity { it.startActivity(Intent(it, DiscoveryActivity::class.java)); it.finish() }
        for (movie in catalog) {
            waitScreen(R.id.primary_action)
            onView(withId(R.id.title)).check(matches(withText(movie.title.substringBefore(": "))))
            onView(withId(R.id.title)).check(matches(isCompletelyDisplayed()))
            if (movie.title.contains(": ")) onView(withId(R.id.subtitle)).check(matches(isCompletelyDisplayed()))
            onView(withId(R.id.scroll)).check { view, _ -> assertFalse("Rolagem em ${movie.title}", view.canScrollVertically(1)) }
            if (movie.title.length > 32) screenshot("long-title-${movie.id}")
            onView(withId(R.id.secondary_action)).perform(click())
        }
        waitScreen(R.id.empty)
        assertEquals(catalog.size, runBlocking { Repository.api.history(auth.user.id) }.size)
        logout()
        scenario.close()
    }

    @Test fun responsiveLayouts() {
        val variant = InstrumentationRegistry.getArguments().getString("layoutVariant") ?: "normal"
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        waitScreen(R.id.username)
        waitForPoster()
        screenshot("responsive-$variant-login")
        onView(withId(R.id.start)).perform(scrollTo()).check(matches(isCompletelyDisplayed()))
        val auth = runBlocking { Repository.api.register(RegisterInput("Teste de layout", "layout_" + System.currentTimeMillis().toString().takeLast(10), "CinemaSenha123!")) }
        Profile(context).save(auth)
        runBlocking { Repository.api.savePreferences(auth.user.id, PreferencesInput(listOf(1, 8))) }
        Profile(context).complete()
        scenario.onActivity {
            it.startActivity(Intent(it, DiscoveryActivity::class.java))
            it.finish()
        }
        waitScreen(R.id.primary_action)
        listOf(R.id.unseen, R.id.seen, R.id.primary_action, R.id.secondary_action, R.id.nav_history).forEach { id ->
            onView(withId(id)).check(matches(isCompletelyDisplayed()))
        }
        if (context.resources.configuration.fontScale >= 1.2f) onView(withId(R.id.title)).perform(scrollTo())
        onView(withId(R.id.title)).check(matches(isCompletelyDisplayed()))
        screenshot("responsive-$variant-discovery")
        if (context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT && context.resources.configuration.fontScale < 1.2f) {
            onView(withId(R.id.scroll)).check { view, _ -> assertFalse("Descoberta normal não deve exigir rolagem", view.canScrollVertically(1)) }
        }
        screenshot("responsive-$variant-discovery")
        onView(withId(R.id.nav_recommendations)).perform(click())
        waitScreen(R.id.list)
        onView(withId(R.id.list)).check { view, _ ->
            val manager = (view as RecyclerView).layoutManager as androidx.recyclerview.widget.GridLayoutManager
            assertEquals(if (context.resources.configuration.screenWidthDp >= 720) 2 else 1, manager.spanCount)
        }
        screenshot("responsive-$variant-recommendations")
        logout()
        scenario.close()
    }
}
