package com.protectednet.utilizr.Mapping

import kotlin.math.sin

data class MapPoint(var x:Float, var y:Float)

class MapUtil {
    private var MapHeight: Double=0.0
    private var MapWidth: Double=0.0
    private var NsDirect: Double=0.0
    private var WeDirect: Double=0.0
    private var SwNeLog: Double=0.0
    private var NwSwLog: Double=0.0

    init {
        calibrate(-61.0, 295.0, 306.0, -57.0, 1000.0, 500.0)
    }

    fun calibrate(
        nwSwLog: Double,
        swNeLog: Double,
        weDirect: Double,
        nsDirect: Double,
        nSLog: Double,
        nSLogGranularity: Double
    ) {
        this.NwSwLog = nwSwLog
        this.SwNeLog = swNeLog
        this.WeDirect = weDirect
        this.NsDirect = nsDirect
        this.MapWidth = nSLog
        this.MapHeight = nSLogGranularity
    }

    fun project(lon: Double, lat: Double): MapPoint {
        val RadiansPerDegree = Math.PI / 180
        val Rad = lat * RadiansPerDegree
        val FSin = sin(Rad)
        val DegreeEqualsRadians = 0.017453292519943
        val EarthsRadius = 6378137.0

        val y = EarthsRadius / 2.0 * Math.log((1.0 + FSin) / (1.0 - FSin))
        val x = lon * DegreeEqualsRadians * EarthsRadius

        return MapPoint(x.toFloat(), y.toFloat())
    }

    fun ConvertLatLongToXY(lt: Double, lon: Double): MapPoint {
        var lat = lt
        val mapWidth = MapWidth
        val mapHeight = MapHeight

        val mapLonLeft = NwSwLog
        val mapLonRight = SwNeLog
        val mapLonDelta = mapLonRight - mapLonLeft

        val mapLatBottom = NsDirect
        val mapLatBottomDegree = mapLatBottom * Math.PI / 180

        val x = (lon - mapLonLeft) * (mapWidth / mapLonDelta) + WeDirect

        lat = lat * Math.PI / 180
        val worldMapWidth = mapWidth / mapLonDelta * 360 / (2 * Math.PI)
        val mapOffsetY = worldMapWidth / 2 * Math.log(
            (1 + sin(mapLatBottomDegree)) / (1 - sin(mapLatBottomDegree))
        )
        val y = mapHeight - (worldMapWidth / 2 * Math.log((1 + sin(lat)) / (1 - sin(lat))) - mapOffsetY)

        return MapPoint(x.toFloat(), y.toFloat())
    }

}