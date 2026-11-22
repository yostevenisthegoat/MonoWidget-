package com.example.util

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

data class SunTimes(
    val sunriseText: String,
    val sunsetText: String,
    val daylightProgress: Float,
    val daylightDuration: String
)

object SunCalculator {

    fun calculate(latitude: Double, longitude: Double): SunTimes {
        val cal = Calendar.getInstance()
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val tzOffsetHours = TimeZone.getDefault().getOffset(cal.timeInMillis) / 3600000.0

        val sunriseMin = computeSunMinutes(dayOfYear, latitude, longitude, tzOffsetHours, isSunrise = true)
        val sunsetMin = computeSunMinutes(dayOfYear, latitude, longitude, tzOffsetHours, isSunrise = false)

        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val span = (sunsetMin - sunriseMin).coerceAtLeast(1)
        val progress = ((nowMin - sunriseMin).toFloat() / span.toFloat()).coerceIn(0f, 1f)
        val durHrs = span / 60
        val durMins = span % 60

        return SunTimes(
            sunriseText = formatMinutes(sunriseMin),
            sunsetText = formatMinutes(sunsetMin),
            daylightProgress = progress,
            daylightDuration = "${durHrs}h ${durMins}m"
        )
    }

    private fun computeSunMinutes(
        dayOfYear: Int,
        lat: Double,
        lon: Double,
        tzOffset: Double,
        isSunrise: Boolean
    ): Int {
        val lngHour = lon / 15.0
        val t = dayOfYear + ((if (isSunrise) 6.0 else 18.0) - lngHour) / 24.0
        val m = 0.9856 * t - 3.289
        var l = m + 1.916 * sin(Math.toRadians(m)) + 0.020 * sin(Math.toRadians(2 * m)) + 282.634
        l = ((l % 360.0) + 360.0) % 360.0

        var ra = Math.toDegrees(kotlin.math.atan(0.91764 * tan(Math.toRadians(l))))
        ra = ((ra % 360.0) + 360.0) % 360.0
        val lQuad = floor(l / 90.0) * 90.0
        val raQuad = floor(ra / 90.0) * 90.0
        ra = (ra + (lQuad - raQuad)) / 15.0

        val sinDec = 0.39782 * sin(Math.toRadians(l))
        val cosDec = cos(kotlin.math.asin(sinDec))
        val cosH = (cos(Math.toRadians(90.833)) - sinDec * sin(Math.toRadians(lat))) /
            (cosDec * cos(Math.toRadians(lat)))

        if (cosH > 1.0) return 7 * 60
        if (cosH < -1.0) return 19 * 60

        var h = Math.toDegrees(acos(cosH))
        if (isSunrise) h = 360.0 - h
        h /= 15.0

        val localMeanTime = h + ra - 0.06571 * t - 6.622
        var ut = (localMeanTime - lngHour) % 24.0
        if (ut < 0) ut += 24.0
        var localTime = (ut + tzOffset) % 24.0
        if (localTime < 0) localTime += 24.0
        return (localTime * 60.0).toInt()
    }

    private fun formatMinutes(totalMinutes: Int): String {
        val h = (totalMinutes / 60) % 24
        val m = totalMinutes % 60
        return String.format(LocaleHelper.default(), "%02d:%02d", h, m)
    }
}

private object LocaleHelper {
    fun default() = java.util.Locale.getDefault()
}
