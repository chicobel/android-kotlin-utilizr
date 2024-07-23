package com.protectednet.utilizr

import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.protectednet.utilizr.GetText.L
import com.protectednet.utilizr.GetText.Localization.ResourceContext
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Assert.assertEquals
import org.junit.Before

import org.junit.Test
import org.junit.runner.RunWith

//import org.junit.Assert.*
import org.junit.runners.Parameterized
import org.junit.runners.Suite
import java.util.Locale

@RunWith(Suite::class)
@Suite.SuiteClasses(InstrumentedLanguageTestsNormal::class, InstrumentedLanguageTestsGroup1Parameterized::class)
class LClassTestSuite {

    companion object {

        private var moFilesIndexed: Boolean = false
        private var languagesCodesList: List<String>? = null

        internal fun indexMoFilesAndGetAllSupportedLanguageCodes(): List<String> {

            if (moFilesIndexed) return languagesCodesList!!
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext

            /* A new asset folder was created and the mo files copied over from the main source set's asset folder on 19th July 2024 to facilitate testing
            *  It would be good to periodically copy the latest mo files if the latest translations need to be used for testing
            */
            val assets = appContext.assets
            val langCodesList = mutableListOf<String>()

            val moFiles = assets.list("locales")
            if (moFiles != null) {
                for (mo in moFiles) {
                    val language = mo.substring(0, 2)
                    langCodesList.add(language)
                    try {
                        val moStream = assets.open("locales/$mo")
                        val data = moStream.readBytes()
                        moStream.close()
                        L.indexMoFile(language, data)
                    } catch (e: Exception) {
                        Log.d("InstrumentTest", e.message ?: "")
                        return langCodesList
                    }
                }
            }
            languagesCodesList = langCodesList
            moFilesIndexed = true

            return langCodesList
        }

    }

}

/**
 * Instrumented test, which will execute on an Android device.
 */
class InstrumentedLanguageTestsNormal {

    // This was there before changes so leaving although it is not doing anything useful
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.protectednet.utilizr.test", appContext.packageName)
    }

    // TODO change the locale to that problematic one in some tests

    @OptIn(ExperimentalUnsignedTypes::class)
    @Before
    fun prepareForTesting() {
        LClassTestSuite.indexMoFilesAndGetAllSupportedLanguageCodes()
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

    @Test
    fun p_allTranslations() {

        val langCode = "de"
        L.setLanguage(langCode)
        val allDictionaries = L.getLookupDictionary()
        val langSpecificDictionary = allDictionaries[langCode]
        val allEnglishSentences = langSpecificDictionary?.translationLookup?.keys
        var actual: String
        var expected: String
        if (allEnglishSentences != null) {
            for (anEnglishSetence in allEnglishSentences) {
                actual = L.t(anEnglishSetence)
                expected = langSpecificDictionary.translationLookup[anEnglishSetence]!!
                assertThat(actual, Matchers.equalTo(expected))
            }
        }

    }

    private fun getDeviceLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList.getDefault().get(0) // Get the first (primary) locale
        } else {
            Locale.getDefault()
        }
    }

}

@RunWith(Parameterized::class)
class InstrumentedLanguageTestsGroup1Parameterized(private val langCode: String, private val englishText: String, private val translatedText: String) {

   /* @OptIn(ExperimentalUnsignedTypes::class)
    @Before
    fun indexMoFiles() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        *//* A new asset folder was created and the mo files copied over from the main source set's asset folder on 19th July 2024 to facilitate testing
        * It would be good to periodically copy the latest mo files if the latest translations need to be used for testing
        *//*
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
    }*/

    // TODO: pass even the parameters as parameters
    //  copy over the latest MO files
    @Test
    fun t_withIntegersAsVarargs_allLanguages() {

        Log.d("TEST", "-------------------------------------")
        Log.d("TEST", "Input = $englishText")
        Log.d("TEST", "Expected = $translatedText")
        Log.d("TEST", "-------------------------------------")
        L.setLanguage(langCode)
        val actual: String
        var expected = translatedText

        if (englishText.contains("{0}") && englishText.contains("{1}")) {
            actual = L.t(englishText, 1, 2)
            expected = expected.replace("{0}", "1")
            expected = expected.replace("{1}", "2")
        } else if (englishText.contains("{0}")) {
            actual = L.t(englishText, 1)
            expected = expected.replace("{0}", "1")
        } else {
            actual = L.t(englishText)
        }

        assertThat(actual, Matchers.equalTo(expected))
    }

    @Test
    fun t_withTextAsVarargs_allLanguages() {

        Log.d("TEST", "-------------------------------------")
        Log.d("TEST", "Input = $englishText")
        Log.d("TEST", "Expected = $translatedText")
        Log.d("TEST", "-------------------------------------")
        L.setLanguage(langCode)
        val actual: String
        var expected = translatedText
         if (englishText.contains("{0}") && englishText.contains("{1}")) {
              actual = L.t(englishText, "DummyParam1Of2", "DummyParam2Of2")
              expected = expected.replace("{0}", "DummyParam1Of2")
              expected = expected.replace("{1}", "DummyParam2Of2")
         } else if (englishText.contains("{0}")) {
              actual = L.t(englishText, "DummyParam1Of1")
              expected = expected.replace("{0}", "DummyParam1Of1")
         } else {
             actual = L.t(englishText) // Replace with your actual logic
         }

        assertThat(actual, Matchers.equalTo(expected))
    }

    @Test
    fun t_withIntegersAsListOfArgs_allLanguages() {

        Log.d("TEST", "-------------------------------------")
        Log.d("TEST", "Input = $englishText")
        Log.d("TEST", "Expected = $translatedText")
        Log.d("TEST", "-------------------------------------")
        L.setLanguage(langCode)
        val actual: String
        var expected = translatedText

        if (englishText.contains("{0}") && englishText.contains("{1}")) {
            actual = L.t(englishText, listOf(1,2))
            expected = expected.replace("{0}", "1")
            expected = expected.replace("{1}", "2")
        } else if (englishText.contains("{0}")) {
            actual = L.t(englishText, listOf(1))
            expected = expected.replace("{0}", "1")
        } else {
            actual = L.t(englishText)
        }


        assertThat(actual, Matchers.equalTo(expected))
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: langCode = {0}, engText = {1}") // Omitting {2} as the last part of some long text can't be easily read in the IDE
        fun data(): List<Array<String>> {

            val langCodesList = LClassTestSuite.indexMoFilesAndGetAllSupportedLanguageCodes()
            val finalParametersListForTests = mutableListOf<Array<String>>()
            val allDictionaries = L.getLookupDictionary()
            var langSpecificDictionary:  ResourceContext?
            langCodesList.forEach {langCode->
                langSpecificDictionary = allDictionaries[langCode]
                langSpecificDictionary!!.translationLookup.forEach { (englishText, translatedText) ->
                    finalParametersListForTests.add(arrayOf(langCode, englishText, translatedText))
                }
            }
            return finalParametersListForTests

        }

    }

}
