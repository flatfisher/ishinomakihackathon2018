package jp.flatfish.ishinomakihack2018

import com.google.firebase.firestore.GeoPoint

import android.location.Location

class Beer {
    var name: String? = null
    var msg: String? = null
    var location: GeoPoint? = null
    var tags:Array<String>? = null
    var distance: Float? = null

    fun calcDist(latitude: Double, longitude: Double) {
        var results = FloatArray(3)
        location?.let {
            Location.distanceBetween(it.latitude, it.longitude, latitude, longitude, results)
            distance = results[0]
        }
    }

    fun checkTag(tag: String): Beer? {
        tags?.forEach {
            if (it == tag) {
                return this
            }
        }
        return null
    }
}