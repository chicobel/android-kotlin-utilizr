package com.protectednet.utilizr.GetText

import android.util.Log
import com.protectednet.utilizr.BuildConfig
import com.protectednet.utilizr.GetText.Localization.DummyResourceContext
import com.protectednet.utilizr.GetText.Localization.ResourceContext
import com.protectednet.utilizr.eventBus.RxBus
import java.util.*
import kotlin.collections.HashMap
import java.text.MessageFormat

interface ITranslatable{
    val english:String
    val translation:String
}

class LArgsInfo(vararg fa:Any){
    val formatArgs= fa.toList()
}

class SupportedLanguage(val name:String, val nativeName:String, val ietfLanguageTag:String)

class MS (t:String, fa: ()->LArgsInfo?): ITranslatable {
    val T = t
    val formatArgs = fa
    override val translation: String
        get() {
            val lArgs = formatArgs.invoke()
            if (T.isEmpty())
                return T
            return if (lArgs != null && lArgs.formatArgs.isNotEmpty())
                L.t(T, lArgs.formatArgs)
            else
                L.t(T)
        }

    override val english: String
        get() {
            val lArgs = formatArgs.invoke()
            return if (lArgs != null && lArgs.formatArgs.isNotEmpty())
                L.t(T, lArgs.formatArgs)
            else
                return T
        }
}

class MP(t:String, tplural:String,c:() -> Int ,fa: () -> LArgsInfo?) : ITranslatable {
    private val T = t
    private val TPlural = tplural
    private val counter = c
    private val formatArgs = fa

    override val translation: String
        get() {
            val count = counter.invoke()

            if (T.isEmpty() && count == 1)
                return T

            if (T.isEmpty() && count != 1)
                return TPlural

            val lArgs = formatArgs.invoke()
            return if (lArgs == null)
                L.p(T, TPlural, count)
            else
                L.p(T, TPlural, count, lArgs.formatArgs)
        }
    override val english: String
        get() {
            val count = counter.invoke()
            val lArgs = formatArgs.invoke()

            return if (lArgs== null || lArgs.formatArgs.isEmpty()) {
                if (count == 1)
                    T
                else
                    TPlural
            } else {
                if (count == 1)
                    MessageFormat(T).format(lArgs.formatArgs)
                else
                    MessageFormat(TPlural).format(lArgs.formatArgs)
            }
        }
}

class L {
    companion object{
        const val TAG="GetText"
        private var lookupDictionary:HashMap<String, ResourceContext> = hashMapOf()
        private var moFileLookup:HashMap<String,String> = hashMapOf()
        private var indexedMoFiles:Boolean=false
        private val debugLanguage:SupportedLanguage= SupportedLanguage("Blank","*****", "blank")
        var currentLanguage:String= "en"

        private var mSupportedLanguages:List<SupportedLanguage> = listOf()
        val supportedLanguages:List<SupportedLanguage>
        get() {
            val supported = mutableListOf<SupportedLanguage>()
            try {
                if (mSupportedLanguages.isNotEmpty())
                    return mSupportedLanguages

                if (!indexedMoFiles) {
                    return  mSupportedLanguages
                }

//                var langNotLocale = moFileLookup.keys.filter { p -> !p.contains("-") }
                val allCultures = Locale.getAvailableLocales()
                val tmp = hashMapOf<String,Boolean>()
                for (culture in allCultures) {
                    Log.d(TAG, "culture.language = ${culture.language} culture.displayName = ${culture.displayName} total locales on the device = ${allCultures.size}")
                    if (!moFileLookup.containsKey(culture.language))
                        continue

                    if(tmp.containsKey(culture.language))
                        continue
                    tmp[culture.language]=true
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

        private var mSupportedLanguagesSorted:List<SupportedLanguage> = listOf()
        /**
         * Provides the supported languages in the ascending order of name. "Blank" language will appear at the very end after everything else is sorted.
         * This was adapted by copying the code in supportedLanguages above. It is possible to refactor code to eliminate repetition but to avoid the risk of
         * breaking existing functionality, this was created as a new one.
         */
        val supportedLanguagesSorted:List<SupportedLanguage>
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
                        mainLocaleOfLanguage = Locale(aLocale.language) // https://stackoverflow.com/questions/36061116/get-language-name-in-that-language-from-language-code
                        displayName = mainLocaleOfLanguage.getDisplayLanguage(mainLocaleOfLanguage).replaceFirstChar { it.uppercase() } // each language will appear in its own alphabet
                        supported.add(
                            SupportedLanguage(
                                displayName,
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

        @ExperimentalUnsignedTypes
        fun indexMoFile(ietfLanguageTag: String, s: ByteArray) {
            if (lookupDictionary.containsKey(ietfLanguageTag))
                return
            try {
                lookupDictionary[ietfLanguageTag]= ResourceContext.fromStream(s, ietfLanguageTag)
                moFileLookup[ietfLanguageTag] = ""
                indexedMoFiles=true
            } catch (ex: Exception) {
               Log.e(TAG,ex.message?:"")
            }
        }


        fun t(T:String, vararg args:Any):String{
            var res: String
            if (currentLanguage.isNotEmpty() && lookupDictionary.containsKey(currentLanguage)) {
                if (lookupDictionary[currentLanguage] != null) {
                    res = lookupDictionary[currentLanguage]!!.lookupString(T)
                    if (args.isNotEmpty()) {
                        if(res.contains("'"))
                            res = res.replace(Regex("(?<!')'(?!')"), "''")
                        res = MessageFormat(res).format(args)
                    }
                    return res
                }
            }
            res = T
            if (args.isNotEmpty()) {
                if(res.contains("'"))
                    res = res.replace(Regex("(?<!')'(?!')"), "''")
                res = MessageFormat(res).format(args)
            }
            return res
        }
        fun t(T:String, args:List<Any>):String{
            var res: String
            if (currentLanguage.isNotEmpty() && lookupDictionary.containsKey(currentLanguage)) {
                if (lookupDictionary[currentLanguage] != null) {
                    res = lookupDictionary[currentLanguage]!!.lookupString(T)
                    if (args.isNotEmpty()) {
                        if(res.contains("'"))
                            res = res.replace(Regex("(?<!')'(?!')"), "''")
                        res = MessageFormat(res).format(args.toTypedArray())
                    }
                    return res
                }
            }
            res = T
            if (args.isNotEmpty()) {
                if(res.contains("'"))
                    res = res.replace(Regex("(?<!')'(?!')"), "''")
                res = MessageFormat(res).format(args.toTypedArray())
            }
            return res
        }

        fun p(T:String, TPlural:String, n:Int, vararg args:Any):String {
            var res: String
            if (lookupDictionary.containsKey(currentLanguage)) {
                if (lookupDictionary[currentLanguage] != null) {
                    res = lookupDictionary[currentLanguage]!!.lookupPluralString(T, TPlural, n)
                    if (args.isNotEmpty()) {
                        if(res.contains("'"))
                            res = res.replace(Regex("(?<!')'(?!')"), "''")
                        res = MessageFormat(res).format(args)
                    }
                    return res
                }
            }
            //couldn't find resource context so return default values
            res = if (n == 1) T else TPlural
            if (args.isNotEmpty()) {
                if(res.contains("'"))
                    res = res.replace(Regex("(?<!')'(?!')"), "''")
                res = MessageFormat(res).format(args)
            }
            return res
        }
        fun p(T:String, TPlural:String, n:Int, args:List<Any>):String {
            var res: String
            if (lookupDictionary.containsKey(currentLanguage)) {
                if (lookupDictionary[currentLanguage] != null) {
                    res = lookupDictionary[currentLanguage]!!.lookupPluralString(T, TPlural, n)
                    if (args.isNotEmpty()) {
                        if(res.contains("'"))
                            res = res.replace(Regex("(?<!')'(?!')"), "''")
                        res = MessageFormat(res).format(args.toTypedArray())
                    }
                    return res
                }
            }
            //couldn't find resource context so return default values
            res = if (n == 1) T else TPlural
            if (args.isNotEmpty()) {
                if(res.contains("'"))
                    res = res.replace(Regex("(?<!')'(?!')"), "''")
                res = MessageFormat(res).format(args.toTypedArray())
            }
            return res
        }

        fun i(t:String, args:()->LArgsInfo? = {null}):ITranslatable{
            return MS(t,args)
        }

        fun ip(t:String, tPlural:String, n: ()->Int, args:()->LArgsInfo):ITranslatable{
            return MP(t,tPlural,n,args)
        }

        fun setLanguage(ietfLanguageTag: String) {
            val lowerIeft = ietfLanguageTag.toLowerCase(Locale.getDefault())
            if (!indexedMoFiles)
                return
            preloadLanguage(lowerIeft)
            var changed = false
            if (currentLanguage != lowerIeft)
                changed = true
            currentLanguage = lowerIeft

            if (changed)
                raiseLocaleChangedEvent()
        }

        private fun preloadLanguage(ietfLanguageTag:String) {
            if (!lookupDictionary.containsKey(ietfLanguageTag))
            {
                //load the mo file for the specified language code
                if (BuildConfig.DEBUG) {
                    //add a dummy 'blank' language that always returns a blanked out string - helpful for finding missing translations/errors
                    if (ietfLanguageTag == "blank") {
                        lookupDictionary["blank"]= DummyResourceContext ("blank",
                        { s->
                            val chars = CharArray(s.length)
                            for (i in chars.indices)
                            {
                                chars[i] = if(s[i].isWhitespace()) s[i] else '*'
                            }
                            String(chars)
                        },
                        { s, p, n ->
                            val chars = CharArray(s.length)
                            var insideFormatPlaceholder = 0
                            for (i in chars.indices)
                            {
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
                            String (chars)
                        })
                    }
                }
            }
        }

        private fun raiseLocaleChangedEvent(){
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
    }
}