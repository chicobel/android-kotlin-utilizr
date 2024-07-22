package com.protectednet.utilizr

import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.protectednet.utilizr.GetText.L
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers
import org.junit.Assert.assertEquals

import org.junit.Test
import org.junit.runner.RunWith

//import org.junit.Assert.*
import org.junit.Before
import java.util.Locale

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.protectednet.utilizr.test", appContext.packageName)
    }

    // TODO change the locale to that problematic one in some tests

    @OptIn(ExperimentalUnsignedTypes::class)
    @Before
    fun indexMoFiles() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        /* A new asset folder was created and the mo files copied over from the main source set's asset folder on 19th July 2024 to facilitate testing
        * It would be good to periodically copy the latest mo files if the latest translations need to be used for testing
        */
        val assets = appContext.assets

        val moFiles = assets.list("locales")
        if (moFiles != null) {
            for (mo in moFiles) {
                val language = mo.substring(0, 2)
                try {
                    val moStream = assets.open("locales/$mo")
                    val data = moStream.readBytes()
                    moStream.close()
                    L.indexMoFile(language, data)
                } catch (e: Exception) {
                    Log.d("InstrumentTest", e.message ?: "")
                }
            }
        }
    }


    @Test
    fun t_englishTextWithNoPlaceholders_germanResult() {
        L.setLanguage("de")
        val expected = "Ihr Konto wird aktualisiert, bitte erlauben Sie bis zu 30 Min."
        val actual = L.t("Your Account is being upgraded, please allow up to 30 minutes")
        assertThat(actual, Matchers.equalTo(expected))
    }

    @Test
    fun t_englishTextWithOnePlaceHolder_germanResult() {
        L.setLanguage("de")
        val expected = "Durch Weitermachen stimmen Sie unserer termsTEST und conditionsTEST zu"
        val actual = L.t("By continuing, you agree to our {0} and {1}", "termsTEST", "conditionsTEST")
        assertThat(actual, Matchers.equalTo(expected))
    }

    @Test
    fun p_englishPluralText_germanPluralResult() {

        val daysRemaining = 5

        L.setLanguage("de")
        val expected = "Ihr Probeabo läuft in $daysRemaining Tagen ab"
        val actual = L.p(
            "Your trial will expire in {0} day",
            "Your trial will expire in {0} days",
            daysRemaining,
            daysRemaining
        )
        assertThat(actual, Matchers.equalTo(expected))

    }

    @Test
    fun p_englishSingularText_germanSingularResult() {

        val daysRemaining = 1

        L.setLanguage("de")
        val expected = "Ihr Probeabo läuft in $daysRemaining Tag ab"
        val actual = L.p(
            "Your trial will expire in {0} day",
            "Your trial will expire in {0} days",
            daysRemaining,
            daysRemaining
        )
        assertThat(actual, Matchers.equalTo(expected))

    }

    /**
     * Developed to test [this](https://github.com/protectednet/android-adblock/issues/273) issue.
     * Make sure the language is Arabic, the locale is United Arab Emirates and the TODO
     */
    @Test
    fun p_englishSingularTextInArabicLocale_germanSingularResult() {

        val daysRemaining = 1

        val deviceLocale = getDeviceLocale()
        if (deviceLocale.toLanguageTag() != "ar-AE" ) { // This particular locale is known to have caused a crash in the past before the code was improved
            throw Throwable("This test is designed for a device with a device locale of ar-AE! Country should be United Arab Emirates and the language should be Arabic")
        }

        L.setLanguage("de")
        val expected = "Ihr Probeabo läuft in $daysRemaining Tag ab"
        val actual = L.p(
            "Your trial will expire in {0} day",
            "Your trial will expire in {0} days",
            daysRemaining,
            daysRemaining
        )
        assertThat(actual, Matchers.equalTo(expected))

    }

    @Test
    fun p_englishPluralTextInArabicLocale_germanPluralResult() {

        val daysRemaining = 2

        val deviceLocale = getDeviceLocale()
        if (!deviceLocale.toLanguageTag().contains("ar-AE")  ) { // This particular locale is known to have caused a crash in the past before the code was improved
            throw Throwable("This test is designed for a device with a device locale which contains ar-AE in the language tag! Country should be United Arab Emirates and the language should be Arabic")
        }

        L.setLanguage("de")
        val expected = "Ihr Probeabo läuft in $daysRemaining Tagen ab"
        val actual = L.p(
            "Your trial will expire in {0} day",
            "Your trial will expire in {0} days",
            daysRemaining,
            daysRemaining
        )
        assertThat(actual, Matchers.equalTo(expected))

    }

    private fun getDeviceLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList.getDefault().get(0) // Get the first (primary) locale
        } else {
            Locale.getDefault()
        }
    }

}
