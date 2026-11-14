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
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

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
                onView(withId(id)).check(matches(isCompletelyDisplayed()))
                onView(withId(R.id.loading)).check(matches(withEffectiveVisibility(Visibility.INVISIBLE)))
                return
            } catch (error: Throwable) { failure = error; Thread.sleep(150) }
        }
        throw AssertionError("Tela não carregou: $id", failure)
    }

    private fun screenshot(name: String) {
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

    private fun childClick(id: Int) = object : androidx.test.espresso.ViewAction {
        override fun getConstraints() = isAssignableFrom(View::class.java)
        override fun getDescription() = "Clicar no controle filho $id"
        override fun perform(controller: androidx.test.espresso.UiController, view: View) {
            view.findViewById<View>(id).performClick()
            controller.loopMainThreadUntilIdle()
        }
    }

    @Test fun fullFlowWithRealBackend() {
        context.getSharedPreferences("profile", Context.MODE_PRIVATE).edit().clear().commit()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        waitScreen(R.id.start)
        screenshot("01-inicio")
        onView(withId(R.id.name)).perform(replaceText("Demonstração AP2"), androidx.test.espresso.action.ViewActions.closeSoftKeyboard())
        onView(withId(R.id.start)).perform(scrollTo(), click())
        waitScreen(R.id.save)
        screenshot("02-generos")
        onView(withText("Ficção científica")).perform(scrollTo(), click())
        onView(withId(R.id.save)).perform(click())
        waitScreen(R.id.like)
        screenshot("03-descobrir")
        val uid = Profile(context).id
        assertTrue(uid > 0)
        assertEquals("Ficção científica", runBlocking { Repository.api.preferences(uid) }.single().name)
        val buttons = listOf(R.id.skip, R.id.dislike, R.id.watch, R.id.like)
        buttons.forEachIndexed { index, button ->
            onView(withId(button)).perform(click())
            waitScreen(R.id.like)
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
        onView(withId(R.id.nav_preferences)).perform(click())
        waitScreen(R.id.save)
        onView(withText("Comédia")).perform(scrollTo(), click())
        onView(withId(R.id.save)).perform(click())
        waitScreen(R.id.like)
        assertTrue(runBlocking { Repository.api.preferences(uid) }.any { it.name == "Comédia" })
        scenario.close()
        ActivityScenario.launch(MainActivity::class.java).use {
            waitScreen(R.id.like)
            assertEquals(uid, Profile(context).id)
            assertEquals(3, runBlocking { Repository.api.history(uid) }.size)
        }
    }
}
