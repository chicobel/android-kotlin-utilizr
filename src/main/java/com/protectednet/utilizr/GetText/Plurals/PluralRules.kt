package com.protectednet.utilizr.GetText.Plurals

class PluralRules {
    companion object{
        val LOCK_OBJECT= object{}

        private var mlookupDictionary= hashMapOf<String,(Int)->Int>()

        val lookupDictionary:HashMap<String,(Int)->Int>
            get() {
                if (mlookupDictionary.isEmpty())
                    populateLookupDictionary()
                return mlookupDictionary
            }

        fun getPluralIndexForCulture(ietfLanguageTag:String, n:Int):Int{
            if (lookupDictionary.containsKey(ietfLanguageTag))
            {
                return lookupDictionary[ietfLanguageTag]!!(n)
            }
            return defaultPluralRule(n)
        }

        fun defaultPluralRule(n: Int): Int {
            return if (n == 1) 0 else 1
        }

        fun populateLookupDictionary(){
            synchronized (LOCK_OBJECT)
            {
                if (mlookupDictionary.isEmpty())
                {
                    var d = hashMapOf<String,(Int)->Int>()
                    //add rules here
                    d["fr-FR"] = { n ->
                        if (n > 1)
                            1
                        else 0
                    }


                    d["pl_PL"] = { n ->
                        if (n == 1)
                            0
                        if (n % 10 >= 2 && n % 4 <= 4 && (n % 100 < 10 || n % 100 >= 20))
                            1
                        2
                    }
                    mlookupDictionary = d
                }
            }
        }
    }
}