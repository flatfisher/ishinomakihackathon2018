package jp.flatfish.ishinomakihack2018

import com.google.firebase.firestore.GeoPoint

class Beer {
    var name: String? = null
    var msg: String? = null
    var location: GeoPoint? = null
    var tags:Array<String>? = null
}