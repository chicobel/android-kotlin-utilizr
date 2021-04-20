package com.protectednet.utilizr.GetText.Localization

import android.util.Log
import com.protectednet.utilizr.BuildConfig
import com.protectednet.utilizr.GetText.Parsers.MOHeader
import java.io.InputStream
import com.protectednet.utilizr.GetText.Parsers.MOParser
import com.protectednet.utilizr.GetText.Plurals.PluralRules


open class ResourceContext(
    val ietfLanguageTag: String,
    val header: MOHeader,
    val translationLookup: HashMap<String, String>,
    val userDefinedTranslations: HashMap<String, String> = hashMapOf()
) {
    open fun lookupString(s: String): String {
        var t = s
        if (translationLookup.containsKey(s)) {
            t = translationLookup[s]!!
            return t
        }

        synchronized(userDefinedTranslations) {
            if (userDefinedTranslations.containsKey(s)) {
                t = userDefinedTranslations[s]!!
                return t
            }
        }
        if (BuildConfig.DEBUG)
            Log.d("ResourceContext", "[no translation for string] [{s}]")

        return s
    }

    open fun addCustomTranslation(s:String, t:String) {
        synchronized(userDefinedTranslations)
        {
            if (userDefinedTranslations.containsKey(s))
                return

            userDefinedTranslations[s] = t
        }
    }

    open fun lookupPluralString(s: String, p: String, n: Int): String {
        val pluralIndex = PluralRules.getPluralIndexForCulture(ietfLanguageTag, n)
        //singular
        var t = s
        if (pluralIndex == 0) {
            if (translationLookup.containsKey(s)) {
                t = translationLookup[s]!!
                return t
            }
        }

        //plural
        var key = s + pluralIndex
        var tp: String
        if (translationLookup.containsKey(key)) {
            tp = translationLookup[key]!!
            return tp
        }

        //fallback to 1st plural rule
        if (pluralIndex > 1) {
            key = s + 1
            tp = ""
            if (translationLookup.containsKey(key)) {
                tp = translationLookup[key]!!
                return tp
            }
        }

        //fallback to singular
        if (translationLookup.containsKey(s)) {
            t = translationLookup[s]!!
            return t
        }

        //no luck just return english
        return if (pluralIndex == 0) s else p
    }

    companion object {
        @ExperimentalUnsignedTypes
        fun fromStream(sourceArray: ByteArray, ietfLanguageTag: String): ResourceContext {
            val res = MOParser.parse(sourceArray)
            return ResourceContext(ietfLanguageTag, res.header, res.translationDictionary)
        }
    }
}