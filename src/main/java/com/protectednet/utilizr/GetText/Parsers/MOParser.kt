package com.protectednet.utilizr.GetText.Parsers

import android.util.Log
import kotlin.collections.HashMap
import java.nio.ByteBuffer
import java.nio.ByteOrder


class MOStringInfo() {
    var LENGTH: Int? = null
    var OFFSET: Int? = null

    constructor(byteArray: ByteArray) : this() {
        val bb = ByteBuffer.wrap(byteArray)
        bb.order(ByteOrder.LITTLE_ENDIAN)
        val ib= bb.asIntBuffer()
        LENGTH = ib.get(0)
        OFFSET = ib.get(1)
        val uinversion = LENGTH!!.toUInt()
    }
}

class MOHeader() {
    var MAGIC_NUMBER: UInt? = null
    var FILE_FORMAT_REVISION: Int? = null
    var NUMBER_OF_STRINGS: Int? = null
    var OFFSET_OF_TABLE_WITH_ORIGINAL_STRINGS: Int? = null
    var OFFSET_OF_TABLE_WITH_TRANSLATION_STRINGS: Int? = null
    var SIZE_OF_HASHING_TABLE: Int? = null
    var OFFSET_OF_HASHING_TABLE: Int? = null

    constructor(byteArray: ByteArray) : this() {

        val bb = ByteBuffer.wrap(byteArray)
        bb.order(ByteOrder.LITTLE_ENDIAN)
        val ib= bb.asIntBuffer()


        MAGIC_NUMBER = bb.getInt(0).toUInt()
        var magicint = MAGIC_NUMBER!!.toInt()
        FILE_FORMAT_REVISION = ib.get(1)
        NUMBER_OF_STRINGS =ib.get(2)
        OFFSET_OF_TABLE_WITH_ORIGINAL_STRINGS = ib.get(3)
        OFFSET_OF_TABLE_WITH_TRANSLATION_STRINGS = ib.get(4)
        SIZE_OF_HASHING_TABLE = ib.get(5)
        OFFSET_OF_HASHING_TABLE = ib.get(6)
        Log.d("","")
    }
}

data class MoParseResult(val header:MOHeader, val translationDictionary:HashMap<String,String>)

class EndianExtensions: ClassLoader(){
    fun ToStructureHostEndian(byteArray: ByteArray):MOStringInfo{

        var cls= this.defineClass(MOStringInfo::javaClass.name,byteArray,0,byteArray.size)
        return  cls as MOStringInfo
    }
    fun ToStructureHostEndianHeader(byteArray: ByteArray):MOHeader{
        var cls= this.defineClass(MOHeader::javaClass.name,byteArray,0,byteArray.size)
        return  cls as MOHeader
    }
}

class MOParser {

    companion object{
        @ExperimentalUnsignedTypes
        fun parse(inputData: ByteArray):MoParseResult{
            val d = HashMap<String, String>()

            val headerLength = 28
//            inputStream.position = 0
            val header = inputData.copyOfRange(0,headerLength)

            //todo: handle big-endian systems (some day)
            val moHeader = MOHeader(header) //EndianExtensions().ToStructureHostEndianHeader(header)
            //get all the lovely strings
            val stringInfoLength = 8
            var stringInfo = ByteArray(stringInfoLength)
            var sInfo: MOStringInfo
            var nStrParts: List<String>

            for (i in 0 until moHeader.NUMBER_OF_STRINGS!!) {
                //--original string
                //get the string info
                var startIndex = moHeader.OFFSET_OF_TABLE_WITH_ORIGINAL_STRINGS!! + (i * stringInfoLength)
                stringInfo = inputData.copyOfRange(startIndex, startIndex + stringInfoLength)
                sInfo = MOStringInfo(stringInfo) //EndianExtensions().ToStructureHostEndian(stringInfo)
                //ignore metadata portion
                if (sInfo.LENGTH == 0) continue
                //read the null terminated source string (only the singular is required as the key)

                var nStr = String(inputData,sInfo.OFFSET!!,sInfo.LENGTH!!) //Encoding.UTF8.GetString(data, sInfo.OFFSET as Int, sInfo.LENGTH as Int)
                nStrParts = nStr.split('\u0000')
                val baseKey = nStrParts[0]
                //translated string(s)
                startIndex = moHeader.OFFSET_OF_TABLE_WITH_TRANSLATION_STRINGS!! + (i * stringInfoLength)
                stringInfo = inputData.copyOfRange(startIndex, startIndex + stringInfoLength)

                sInfo = MOStringInfo(stringInfo) //EndianExtensions().ToStructureHostEndian(stringInfo)
                //read null terminated translated strings
                nStr = String(inputData, sInfo.OFFSET as Int, sInfo.LENGTH as Int)
                nStrParts = nStr.split('\u0000')
                for (j in nStrParts.indices) {
                    val key = if (j == 0) baseKey else baseKey + j
                    d[key] = nStrParts[j]
                }
            }
            return MoParseResult(moHeader, d)
        }
    }
}