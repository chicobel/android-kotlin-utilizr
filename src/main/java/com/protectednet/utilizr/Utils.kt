package com.protectednet.utilizr

import android.content.Context
import android.os.Build
import android.text.Html
import android.text.SpannableString
import android.text.Spanned

class Utils {

    companion object{
        @SuppressWarnings("deprecation")
        fun fromHtml(html:String?):Spanned{
            if(html == null)
                return SpannableString("")
            return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
            }else{
                Html.fromHtml(html)
            }
        }


    }

}