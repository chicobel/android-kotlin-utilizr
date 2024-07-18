package com.protectednet.utilizr.GetText

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.os.LocaleListCompat
import com.protectednet.utilizr.BuildConfig
import com.protectednet.utilizr.GetText.Localization.DummyResourceContext
import com.protectednet.utilizr.GetText.Localization.ResourceContext
import com.protectednet.utilizr.eventBus.RxBus
import java.util.*
import kotlin.collections.HashMap
import java.text.MessageFormat


interface ITranslatable {
    val english: String
    val translation: String
}

class LArgsInfo(vararg fa: Any) {
    val formatArgs = fa.toList()
}

class SupportedLanguage(val name: String, val nativeName: String, val ietfLanguageTag: String)

/**
 * Not sure what MS stands for. Best guess someone came up with was: Mo Singular
 */
class MS(text: String, fa: () -> LArgsInfo?) : ITranslatable {
    val t = text
    val formatArgs = fa
    override val translation: String
        get() {
            val lArgs = formatArgs.invoke()
            if (t.isEmpty())
                return t
            return if (lArgs != null && lArgs.formatArgs.isNotEmpty())
                L.t(t, lArgs.formatArgs)
            else
                L.t(t)
        }

    override val english: String
        get() {
            val lArgs = formatArgs.invoke()
            return if (lArgs != null && lArgs.formatArgs.isNotEmpty())
                L.t(t, lArgs.formatArgs)
            else
                return t
        }
}

/**
 * Not sure what MP stands for. Best guess someone came up with was: Mo Plural.
 */
class MP(textSingular: String, textPlural: String, c: () -> Int, fa: () -> LArgsInfo?) :
    ITranslatable {
    private val t = textSingular
    private val tPlural = textPlural
    private val counter = c
    private val formatArgs = fa

    override val translation: String
        get() {
            val count = counter.invoke()

            if (t.isEmpty() && count == 1)
                return t

            if (t.isEmpty() && count != 1)
                return tPlural

            val lArgs = formatArgs.invoke()
            return if (lArgs == null)
                L.p(t, tPlural, count)
            else
                L.p(t, tPlural, count, lArgs.formatArgs)
        }
    override val english: String
        get() {
            val count = counter.invoke()
            val lArgs = formatArgs.invoke()

            return if (lArgs == null || lArgs.formatArgs.isEmpty()) {
                if (count == 1)
                    t
                else
                    tPlural
            } else {
                if (count == 1)
                    L.getFormattedString(
                        t,
                        lArgs.formatArgs
                    )
                else
                    L.getFormattedString(
                        tPlural,
                        lArgs.formatArgs
                    )
            }
        }
}

class L {
    companion object {
        const val TAG = "GetText"
        private var lookupDictionary: HashMap<String, ResourceContext> = hashMapOf()
        private var moFileLookup: HashMap<String, String> = hashMapOf()
        private var indexedMoFiles: Boolean = false
        private val debugLanguage: SupportedLanguage = SupportedLanguage("Blank", "*****", "blank")
        var currentLanguage: String = "en"

        /**
         * Locale assumed based on the currently selected language.
         * Even if, say, a language like German is selected from the app and then the app is closed, when the app
         * is started up, as the language changes from "en" to "de" this will get assigned the correct Locale for de.
         * check [setMainLocaleForCurrentLanguage] a descriptive explaination for why this was needed.
         */
        private var mainLocaleOfCurrentLanguage: Locale = Locale.US

        private var mSupportedLanguages: List<SupportedLanguage> = listOf()
        val supportedLanguages: List<SupportedLanguage>
            get() {
                val supported = mutableListOf<SupportedLanguage>()
                try {
                    if (mSupportedLanguages.isNotEmpty())
                        return mSupportedLanguages

                    if (!indexedMoFiles) {
                        return mSupportedLanguages
                    }

//                var langNotLocale = moFileLookup.keys.filter { p -> !p.contains("-") }
                    val allCultures = Locale.getAvailableLocales()
                    val tmp = hashMapOf<String, Boolean>()
                    for (culture in allCultures) {
                        Log.d(
                            TAG,
                            "culture.language = ${culture.language} culture.displayName = ${culture.displayName} total locales on the device = ${allCultures.size}"
                        )
                        if (!moFileLookup.containsKey(culture.language))
                            continue

                        if (tmp.containsKey(culture.language))
                            continue
                        tmp[culture.language] = true
                        supported.add(
                            SupportedLanguage(
                                culture.displayName,
                                culture.displayName,
                                culture.language
                            )
                        )
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to get list, defaulting to English only $ex")
                }

                // We always have English without en-GB.po
                supported.add(SupportedLanguage("English", "English", "en"))

                if (BuildConfig.DEBUG) {
                    //add a dummy 'blank' language that always returns a blanked out string - helpful for finding missing translations/errors
                    supported.add(debugLanguage)
                }

                mSupportedLanguages = supported
                return mSupportedLanguages
            }

        private var mSupportedLanguagesSorted: List<SupportedLanguage> = listOf()

        /**
         * Provides the supported languages in the ascending order of name. "Blank" language will appear at the very end after everything else is sorted.
         * This was adapted by copying the code in supportedLanguages above. It is possible to refactor code to eliminate repetition but to avoid the risk of
         * breaking existing functionality, this was created as a new one.
         */
        val supportedLanguagesSorted: List<SupportedLanguage>
            get() {
                var supported = mutableListOf<SupportedLanguage>()

                try {
                    if (mSupportedLanguagesSorted.isNotEmpty())
                        return mSupportedLanguagesSorted

                    if (!indexedMoFiles) {
                        return mSupportedLanguagesSorted
                    }

//                var langNotLocale = moFileLookup.keys.filter { p -> !p.contains("-") }
                    val allInstalledLocales = Locale.getAvailableLocales()
                    val tmp = hashMapOf<String, Boolean>()
                    var displayName: String
                    var mainLocaleOfLanguage: Locale // Used to store the main locale for a given language. e.g. Locale("de", "DE") German language in Germany
                    for (aLocale in allInstalledLocales) {
                        //Log.d(TAG, "culture.language = ${culture.language} culture.displayName = ${culture.displayName} total locales on the device = ${allCultures.size}")

                        if (!moFileLookup.containsKey(aLocale.language))
                            continue

                        /* It seems the reason for doing this is that there are multiple entries with the same "culture.language" value and
                          ,in most cases, it is only the first one of these that will have a clean language name in the "culture.displayName" without an associated country
                          shown in brackets. This first entry for a given language code seems to be the main one. e.g. the first entry
                          for "en" would have something like "Englisch" for culture.displayName but the second entry with the same language code would be something
                          like "Englisch (Welt)" – note the brackets.
                          Note the above seems to be true in most cases only. It was found that this didn't work this way on an Android 7.0 API 24 Pixel 2 emulator.
                          In this one the languages were not all grouped together at all and the ietf tag was not sorted in any particular order either. So, the logic here previously in place resulted
                          in the country in brackets also to show.
                          Also, it seems what culture.displayName shows depends on the main locale setup on the device in most cases. */
                        if (tmp.containsKey(aLocale.language))
                            continue
                        tmp[aLocale.language] = true
                        mainLocaleOfLanguage =
                            Locale(aLocale.language) // https://stackoverflow.com/questions/36061116/get-language-name-in-that-language-from-language-code
                        displayName = mainLocaleOfLanguage.getDisplayLanguage(mainLocaleOfLanguage)
                            .replaceFirstChar { it.uppercase() } // each language will appear in its own alphabet
                        supported.add(
                            SupportedLanguage(
                                aLocale.displayName,
                                displayName,
                                aLocale.language
                            )
                        )
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to get list, defaulting to English only $ex")
                }

                // We always have English without en-GB.po
                supported.add(SupportedLanguage("English", "English", "en"))

                supported = supported.sortedBy { it.name }.toMutableList()

                if (BuildConfig.DEBUG) {
                    // add a dummy 'blank' language that always returns a blanked out string - helpful for finding missing translations/errors
                    supported.add(debugLanguage)
                }

                mSupportedLanguagesSorted = supported
                return mSupportedLanguagesSorted
            }

        private var mSupportedLanguagesNativeNamesAndEnglishSorted: List<SupportedLanguage> =
            listOf()

        /**
         * Provides a list of the supported languages by the App sorted in the ascending alphabetical order of native names along with their corresponding English names.
         * "Blank" language will appear at the very end of the list after everything else is sorted.
         * Full expanded version of the property name: Supported Languages Native and English Names Sorted.
         * This was adapted by copying the code in the supportedLanguagesSorted property in the same class.
         * It was perhaps possible to refactor code to eliminate repetition and use the same property but to avoid the risk of
         * breaking existing functionality, this was created as a new one.
         * Main motivation for creating this was that supportedLanguagesSorted did not provide the native names.
         */
        val supportedLangsWithNativeAndEnglishNamesSorted: List<SupportedLanguage>
            get() {
                var supported = mutableListOf<SupportedLanguage>()

                try {
                    if (mSupportedLanguagesNativeNamesAndEnglishSorted.isNotEmpty())
                        return mSupportedLanguagesNativeNamesAndEnglishSorted

                    if (!indexedMoFiles) {
                        return mSupportedLanguagesNativeNamesAndEnglishSorted
                    }

                    val allInstalledLocales = Locale.getAvailableLocales()
                    val tmp = hashMapOf<String, Boolean>()
                    var nativeName: String
                    var englishName: String
                    var mainLocaleOfLanguage: Locale // Used to store the main locale for a given language. e.g. Locale("de", "DE") German language in Germany
                    for (aLocale in allInstalledLocales) {
                        //Log.d(TAG, "culture.language = ${culture.language} culture.displayName = ${culture.displayName} total locales on the device = ${allCultures.size}")

                        if (!moFileLookup.containsKey(aLocale.language))
                            continue

                        /* It seems the reason for doing this is that there are multiple entries with the same "culture.language" value and
                          ,in most cases, it is only the first one of these that will have a clean language name in the "culture.displayName" without an associated country
                          shown in brackets. This first entry for a given language code seems to be the main one. e.g. the first entry
                          for "en" would have something like "Englisch" for culture.displayName but the second entry with the same language code would be something
                          like "Englisch (Welt)" – note the brackets.
                          Note the above seems to be true in most cases only. It was found that this didn't work this way on an Android 7.0 API 24 Pixel 2 emulator.
                          In this one the languages were not all grouped together at all and the ietf tag was not sorted in any particular order either. So, the logic here previously in place resulted
                          in the country in brackets also to show.
                          Also, it seems what culture.displayName shows depends on the main locale setup on the device in most cases. */
                        if (tmp.containsKey(aLocale.language))
                            continue
                        tmp[aLocale.language] = true

                        mainLocaleOfLanguage =
                            Locale(aLocale.language) // https://stackoverflow.com/questions/36061116/get-language-name-in-that-language-from-language-code
                        nativeName = mainLocaleOfLanguage.getDisplayLanguage(mainLocaleOfLanguage)
                            .replaceFirstChar { it.uppercase() } // each language will appear in its own alphabet

                        englishName = mainLocaleOfLanguage.getDisplayLanguage(Locale("en"))

                        supported.add(
                            SupportedLanguage(
                                englishName,
                                nativeName,
                                aLocale.language
                            )
                        )
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to get list, defaulting to English only $ex")
                }

                // We always have English without en-GB.po
                supported.add(SupportedLanguage("English", "English", "en"))

                supported = supported.sortedBy { it.nativeName }
                    .toMutableList() // Note that it is sorted by native name unlike in the other two properties above (supportedLanguagesSorted and supportedLanguages)

                if (BuildConfig.DEBUG) {
                    // add a dummy 'blank' language that always returns a blanked out string - helpful for finding missing translations/errors
                    supported.add(debugLanguage)
                }

                mSupportedLanguagesNativeNamesAndEnglishSorted = supported
                return mSupportedLanguagesNativeNamesAndEnglishSorted
            }

        @ExperimentalUnsignedTypes
        fun indexMoFile(ietfLanguageTag: String, s: ByteArray) {
            if (lookupDictionary.containsKey(ietfLanguageTag))
                return
            try {
                lookupDictionary[ietfLanguageTag] = ResourceContext.fromStream(s, ietfLanguageTag)
                moFileLookup[ietfLanguageTag] = ""
                indexedMoFiles = true
            } catch (ex: Exception) {
                Log.e(TAG, ex.message ?: "")
            }
        }

        /** Had to add the annotation in order to access this method from a Java file.
         * [See](https://www.baeldung.com/kotlin/companion-objects-in-java) */
        @JvmStatic
        fun t(text: String, vararg args: Any): String {
            var res: String
            if (currentLanguage.isNotEmpty() && lookupDictionary.containsKey(currentLanguage)) {
                if (lookupDictionary[currentLanguage] != null) {
                    res = lookupDictionary[currentLanguage]!!.lookupString(text)
                    if (args.isNotEmpty()) {
                        if (res.contains("'"))
                            res = res.replace(Regex("(?<!')'(?!')"), "''")
                        res = getFormattedString(res, args.toList())
                    }
                    return res
                }
            }
            res = text
            if (args.isNotEmpty()) {
                if (res.contains("'"))
                    res = res.replace(Regex("(?<!')'(?!')"), "''")
                res = getFormattedString(res, args.toList())
            }
            return res
        }

        fun t(text: String, args: List<Any>): String {
            var res: String
            if (currentLanguage.isNotEmpty() && lookupDictionary.containsKey(currentLanguage)) {
                if (lookupDictionary[currentLanguage] != null) {
                    res = lookupDictionary[currentLanguage]!!.lookupString(text)
                    if (args.isNotEmpty()) {
                        if (res.contains("'"))
                            res = res.replace(Regex("(?<!')'(?!')"), "''")
                        res = getFormattedString(res, args)
                    }
                    return res
                }
            }
            res = text
            if (args.isNotEmpty()) {
                if (res.contains("'"))
                    res = res.replace(Regex("(?<!')'(?!')"), "''")
                res = getFormattedString(res, args)
            }
            return res
        }

        fun p(textSingular: String, textPlural: String, n: Int, vararg args: Any): String {
            var res: String
            if (lookupDictionary.containsKey(currentLanguage)) {
                if (lookupDictionary[currentLanguage] != null) {
                    res = lookupDictionary[currentLanguage]!!.lookupPluralString(
                        textSingular,
                        textPlural,
                        n
                    )
                    if (args.isNotEmpty()) {
                        if (res.contains("'"))
                            res = res.replace(Regex("(?<!')'(?!')"), "''")
                        res = getFormattedString(res, args.toList())
                    }
                    return res
                }
            }
            //couldn't find resource context so return default values
            res = if (n == 1) textSingular else textPlural
            if (args.isNotEmpty()) {
                if (res.contains("'"))
                    res = res.replace(Regex("(?<!')'(?!')"), "''")
                res = getFormattedString(res, args.toList())
            }
            return res
        }

        fun p(textSingular: String, textPlural: String, n: Int, args: List<Any>): String {
            var res: String
            if (lookupDictionary.containsKey(currentLanguage)) {
                if (lookupDictionary[currentLanguage] != null) {
                    res = lookupDictionary[currentLanguage]!!.lookupPluralString(
                        textSingular,
                        textPlural,
                        n
                    )
                    if (args.isNotEmpty()) {
                        if (res.contains("'"))
                            res = res.replace(Regex("(?<!')'(?!')"), "''")
                        res = getFormattedString(res, args)
                    }
                    return res
                }
            }
            //couldn't find resource context so return default values
            res = if (n == 1) textSingular else textPlural
            if (args.isNotEmpty()) {
                if (res.contains("'"))
                    res = res.replace(Regex("(?<!')'(?!')"), "''")
                res = getFormattedString(res, args)
            }
            return res
        }

        fun i(text: String, args: () -> LArgsInfo? = { null }): ITranslatable {
            return MS(text, args)
        }

        fun ip(
            textSingular: String,
            textPlural: String,
            n: () -> Int,
            args: () -> LArgsInfo
        ): ITranslatable {
            return MP(textSingular, textPlural, n, args)
        }

        /**
         * Note that ietfLanguageTag.toLowerCase(Locale.getDefault()) line was replaced with ietfLanguageTag.lowercase(Locale.ENGLISH) on 17 Oct 2023
         * as toLowerCase was deprecated and getDefault() could cause bugs as per [this](https://stackoverflow.com/a/11063161) SO post.
         * Comment added in case this causes any unforeseen issues.
         * @param ietfLanguageTag
         */
        fun setLanguage(ietfLanguageTag: String) {
            val lowerIeft = ietfLanguageTag.lowercase(Locale.ENGLISH)

            if (!indexedMoFiles)
                return
            preloadLanguage(lowerIeft)
            var changed = false
            if (currentLanguage != lowerIeft)
                changed = true
            currentLanguage = lowerIeft

            if (changed) {
                raiseLocaleChangedEvent()
                setMainLocaleForCurrentLanguage() // As we are in the class that the language change is made itself, not using a RxBus subscription unnecessarily to do this
            }
        }

        private fun preloadLanguage(ietfLanguageTag: String) {
            if (!lookupDictionary.containsKey(ietfLanguageTag)) {
                //load the mo file for the specified language code
                if (BuildConfig.DEBUG) {
                    //add a dummy 'blank' language that always returns a blanked out string - helpful for finding missing translations/errors
                    if (ietfLanguageTag == "blank") {
                        lookupDictionary["blank"] = DummyResourceContext("blank",
                            { s ->
                                val chars = CharArray(s.length)
                                for (i in chars.indices) {
                                    chars[i] = if (s[i].isWhitespace()) s[i] else '*'
                                }
                                String(chars)
                            },
                            { s, p, n ->
                                val chars = CharArray(s.length)
                                var insideFormatPlaceholder = 0
                                for (i in chars.indices) {
                                    // Don't replace character if whitespace to avoid one long string of *******
                                    // Don't replace string format placeholders, such as {0:N0}

                                    chars[i] = s[i]
                                    if (s[i].isWhitespace())
                                        continue

                                    if (s[i] == '{') {
                                        insideFormatPlaceholder++
                                        continue
                                    }

                                    if (s[i] == '}') {
                                        insideFormatPlaceholder--
                                        continue
                                    }

                                    if (insideFormatPlaceholder > 0)
                                        continue

                                    chars[i] = '*'

                                }
                                String(chars)
                            })
                    }
                }
            }
        }

        private fun raiseLocaleChangedEvent() {
            RxBus.publish(LocaleChangedMessage(currentLanguage))
        }

        fun addCustomTranslation(ietfLanguageTag: String, id: String, translation: String) {
//            if (!indexedMoFiles)
//                IndexMoFiles()

            preloadLanguage(ietfLanguageTag)
            var context: ResourceContext? = null
            var key = ietfLanguageTag
            if (!lookupDictionary.containsKey(key)) {
                key = key.substring(0, 2)
            }
            if (!lookupDictionary.containsKey(key))
                return
            context = lookupDictionary[ietfLanguageTag]

            context?.addCustomTranslation(id, translation)
        }

        /**
         * Checks whether the provided language tag is supported by the App.
         *
         * @param ietfLanguageTag case sensitive ietf language tag to check if the App supports. Must be a two letter code with everything after "-" stripped off.
         * @return true if the language tag is supported. 'false' if it is not supported.
         */
        fun isLanguageSupportedByApp(ietfLanguageTag: String): Boolean {
            if (ietfLanguageTag.length != 2 && ietfLanguageTag.contains("-")) throw IllegalArgumentException(
                "Supplied language tag must only have two characters and should not have -"
            )
            return supportedLanguagesSorted.any { it.ietfLanguageTag == ietfLanguageTag }
        }

        /**
         * Given an ieft language code, this function returns a tag compatible with our website.
         * If it is not supported by our site, "en" is returned as the language tag.
         * Only the characters before "-" of language tags like en-GB, de-DE are considered when finding a compatible code.
         *
         * Main motivation for creating this was because although in Android the language code for Norwegian Nynorsk is "nn", on our website the code was "no"
         *
         * When checked on 11 Sep 2023, the following language codes were identified on www.totalav.com
         * it, en, fr, de, es, nl, no, pt, sv, tr, pl, da
         * I.e. When the language is changed on the website, it appends one of the above codes to the URL in the example format below:
         * e.g. https://www.totalav.com?forceLang=de
         */
        fun websiteCompatibleLangTag(ietfLanguageTag: String): String {

            val twoLetterLangCodeInAndroid = ietfLanguageTag.substringBefore("-")
            val codesSupportedOnWebsite =
                listOf("it", "en", "fr", "de", "es", "nl", "no", "pt", "sv", "tr", "pl", "da")

            val codeToLookup = if (twoLetterLangCodeInAndroid == "nn") {
                "no"
            } else {
                twoLetterLangCodeInAndroid
            }

            return if (codesSupportedOnWebsite.contains(codeToLookup)) codeToLookup else "en"

        }

        data class DeviceLanguageWantedInfo(
            val ietfLanguageTag: String,
            val displayLanguage: String
        )

        /**
         * Given a list of language codes supported by the device, this function returns the first supported language by the App.
         * Reason for creating this was as follows:
         * LocaleManagerCompat.getSystemLocales(AvApplication.instance!!.applicationContext)[0] returns two different results in API 33+ and API<33
         * In API 33, it returns whatever is on top when the order the languages are listed in device settings is considered.
         * In API<33, if there is at least one supported language in the list, the above line of code returns the first such language instead of what is on top of the list visually in the system UI.
         * This meant the results were inconsistent in API 33+ and API<33.
         * @param deviceSystemLocales list of locales supported by the device obtained by calling LocaleManagerCompat.getSystemLocales(AvApplication.instance!!.applicationContext)
         * @return ietf language tag and display language of first matching language wrapped in a data class or null if not a single device language is supported by the App.
         */
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        fun getInfoOf1stDeviceLangSupportedByApp(deviceSystemLocales: LocaleListCompat): DeviceLanguageWantedInfo? {
            var ietfLanguageTag = ""
            var localeInConsideration: Locale
            for (i in 0 until deviceSystemLocales.size()) {
                localeInConsideration = deviceSystemLocales.get(i)
                    ?: return null // This shouldn't be null but just in case the unforeseen happens
                ietfLanguageTag = localeInConsideration.toLanguageTag().substringBefore("-")
                if (isLanguageSupportedByApp(ietfLanguageTag)) {
                    val displayLanguage =
                        localeInConsideration.getDisplayLanguage(Locale(currentLanguage))  // Otherwise, the device language name doesn't appear in the correct translation.
                    return DeviceLanguageWantedInfo(ietfLanguageTag, displayLanguage)
                }
            }
            return null
        }

        /**
         *
         * @param pattern String pattern. e.g. "Only {0} days left for your birthday"
         * @param args  arguments list passed to replace the place holders in the pattern. e.g. listOf(4)
         * If the above examples string are passed, this should return the following:  Only 4 days left for your birthday
         * This was created so that MessageFormat is only used inside this and it can be replaced easily if needed in the future.
         */
        fun getFormattedString(pattern: String, args: List<Any>): String {
            try {
                return MessageFormat(
                    pattern,
                    mainLocaleOfCurrentLanguage
                ).format(args.toTypedArray())
            } catch (e: Exception) {
                Log.d(
                    TAG,
                    "MessageFormat exception for string $pattern. Exception message: ${e.message}"
                )
                return pattern // returning the same string passed without allowing the app to crash.
            }
        }

        /**
         * Sets a static field in this class to a Locale that best represents the language tag of the currently selected language
         * Motivation for creating this:
         * Imagine we want to display “Your birthday is in 4 days” using L.t(Your birthday is in {0} days", days)
         * In a certain Arabic Locale, it could get displayed as “Your birthday is in ٢ days” as MessageFormat which is used under the bonnet used the default locale of the user's device.
         * In another place in the code, as the above scenario was unforeseen, the app was crashing as well.
         * Therefore, we now find the main locale of the current language so that MessageFormat can be given an appropriate Locale without being at the mercy of the user's device locale.
         */
        private fun setMainLocaleForCurrentLanguage() {
            mainLocaleOfCurrentLanguage =
                if (currentLanguage.lowercase(Locale.US) == "blank")
                    Locale.US
                else
                    Locale.forLanguageTag(currentLanguage) // Using this method rather than using hard coded language and country names(e.g. de_DE) to make it easier to maintain when new languages are added.
            Log.d(
                TAG,
                "Language has changed. Current language code = $currentLanguage, Current Locale language tag= ${mainLocaleOfCurrentLanguage.toLanguageTag()}"
            )
        }

    }
}