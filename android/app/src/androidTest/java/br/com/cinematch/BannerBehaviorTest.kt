package br.com.cinematch

import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.RootMatchers.isDialog
import br.com.cinematch.data.*
import br.com.cinematch.ui.MainActivity
import br.com.cinematch.ui.DiscoveryActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class BannerBehaviorTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private fun waitFor(id: Int) {
        val until = System.currentTimeMillis() + 25000
        var failure: Throwable? = null
        while (System.currentTimeMillis() < until) {
            try {
                onView(withId(id)).check(matches(isDisplayed()))
                onView(withId(R.id.loading)).check(matches(withEffectiveVisibility(Visibility.INVISIBLE)))
                return
            } catch (e: Throwable) { failure = e; Thread.sleep(120) }
        }
        throw AssertionError("Controle não carregou: $id", failure)
    }
    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        Thread.sleep(250)
        val image = instrumentation.uiAutomation.takeScreenshot()
        val output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let { File(it).apply { mkdirs() } } ?: context.getExternalFilesDir(null)
        FileOutputStream(File(output, "$name.png")).use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        image.recycle()
    }
    private fun fill(id: Int, value: String) { onView(withId(id)).perform(scrollTo(), replaceText(value), closeSoftKeyboard()) }

    @Test fun failureDoesNotAdvanceAndProfilePersists() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        waitFor(R.id.username)
        val username = "banner_" + System.currentTimeMillis().toString().takeLast(9)
        val auth = runBlocking { Repository.api.register(RegisterInput("Teste Banner",username,"CinemaSenha123!")) }
        Profile(context).save(auth)
        runBlocking { Repository.api.savePreferences(auth.user.id,PreferencesInput(listOf(1))) }
        Profile(context).complete()
        scenario.onActivity { it.startActivity(Intent(it,DiscoveryActivity::class.java)); it.finish() }
        waitFor(R.id.primary_action)
        var initialTitle = ""
        onView(withId(R.id.title)).check { v,_ -> initialTitle=(v as android.widget.TextView).text.toString() }
        onView(withId(R.id.seen)).perform(click())
        assertTrue(runBlocking { Repository.api.history(auth.user.id) }.isEmpty())
        // A sessão continua válida, mas um ID diferente deve ser rejeitado pela API real.
        Profile(context).save(auth.copy(user=auth.user.copy(id=auth.user.id+100000),preferences_ready=true))
        onView(withId(R.id.primary_action)).perform(click())
        waitFor(R.id.action_error)
        onView(withId(R.id.title)).check(matches(withText(initialTitle)))
        onView(withId(R.id.primary_action)).check(matches(withText("Gostei")))
        screenshot("banner-api-error")
        Profile(context).save(auth.copy(preferences_ready=true))
        assertTrue(runBlocking { Repository.api.history(auth.user.id) }.isEmpty())
        onView(withId(R.id.action_retry)).perform(click())
        waitFor(R.id.primary_action)
        assertEquals(1,runBlocking { Repository.api.history(auth.user.id) }.size)
        onView(withId(R.id.unseen)).check(matches(isChecked()))
        onView(withId(R.id.primary_action)).check(matches(withText("Quero assistir")))
        onView(withId(R.id.title)).check { v,_ -> assertNotEquals(initialTitle,(v as android.widget.TextView).text.toString()) }
        onView(withId(R.id.primary_action)).perform(object : androidx.test.espresso.ViewAction {
            override fun getConstraints() = isAssignableFrom(View::class.java)
            override fun getDescription() = "Dois toques imediatos na mesma ação"
            override fun perform(controller: androidx.test.espresso.UiController, view: View) {
                view.performClick()
                view.performClick()
                controller.loopMainThreadUntilIdle()
            }
        })
        waitFor(R.id.primary_action)
        assertEquals(2,runBlocking { Repository.api.history(auth.user.id) }.size)
        onView(withId(R.id.account)).perform(click())
        waitFor(R.id.edit_profile)
        onView(withId(R.id.edit_profile)).perform(scrollTo(),click())
        fill(R.id.edit_name,"Pessoa do cinema")
        fill(R.id.edit_username,username+"_novo")
        onView(withId(R.id.save_profile)).perform(scrollTo(),click())
        waitFor(R.id.edit_profile)
        onView(withId(R.id.name)).check(matches(withText("Pessoa do cinema")))
        assertEquals(username+"_novo",runBlocking { Repository.api.user(auth.user.id) }.username)
        screenshot("banner-profile")
        for ((label,file) in listOf("Claro" to "banner-profile-light","Escuro" to "banner-profile-dark")) {
            onView(withId(R.id.appearance)).perform(scrollTo(),click())
            onView(withText(label)).inRoot(isDialog()).perform(click())
            waitFor(R.id.edit_profile)
            assertEquals(auth.user.id,Profile(context).id)
            screenshot(file)
            onView(withId(R.id.nav_discovery)).perform(click())
            waitFor(R.id.primary_action)
            screenshot(file.replace("profile","discovery"))
            onView(withId(R.id.nav_profile)).perform(click())
            waitFor(R.id.edit_profile)
        }
        assertEquals(2,Appearance.selected(context))
        onView(withId(R.id.logout)).perform(scrollTo(),click())
        onView(withText("Sair")).inRoot(isDialog()).perform(click())
        waitFor(R.id.username)
        assertEquals(2,Appearance.selected(context))
        fill(R.id.username,username+"_novo")
        fill(R.id.password,"CinemaSenha123!")
        onView(withId(R.id.start)).perform(scrollTo(),click())
        waitFor(R.id.primary_action)
        assertEquals(auth.user.id,Profile(context).id)
        assertEquals("Pessoa do cinema",Profile(context).name)
        assertEquals(2,runBlocking { Repository.api.history(auth.user.id) }.size)
        onView(withId(R.id.nav_profile)).perform(click())
        waitFor(R.id.appearance)
        onView(withId(R.id.appearance)).perform(scrollTo(),click())
        onView(withText("Sistema")).inRoot(isDialog()).perform(click())
        waitFor(R.id.edit_profile)
        assertEquals(0,Appearance.selected(context))
        scenario.close()
    }
}
