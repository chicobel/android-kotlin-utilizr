package com.protectednet.utilizr.GetText.Localization

import com.protectednet.utilizr.GetText.Parsers.MOHeader

class DummyResourceContext(val ieftTag:String, S:(String)->String,P:(String,String,Int)->String) :ResourceContext(ieftTag, MOHeader(), hashMapOf()){
    val pDelegate=P
    val sDelegate=S
    override fun lookupPluralString(s: String, p: String, n: Int): String {
        return pDelegate.invoke(s,p,n)
    }

    override fun lookupString(s: String): String {
        return sDelegate.invoke(s)
    }

}