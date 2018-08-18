package jp.flatfish.ishinomakihack2018

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import android.support.v4.app.ActivityCompat
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.Manifest
import android.location.LocationManager
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.support.annotation.NonNull
import android.location.LocationProvider
import android.provider.Settings
import java.util.stream.Collectors


class MainActivity : AppCompatActivity(), LocationListener {
    private val TAG = MainActivity::class.java.simpleName

    private val MIN_OPENGL_VERSION = 3.0
    private var mBeers = mutableListOf<Beer>()
    private var sortedBeers = mutableListOf<Beer>()

    private var nowLocation: Location? = null

    private var locationManager: LocationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1000)
        } else {
            locationStart()

            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    1000, 10f, this)

        }

        //Firebase
        val db: FirebaseFirestore = FirebaseFirestore.getInstance()
        db.collection("samples")
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        for (document in task.result) {
                            val data = document.data
                            val beer = Beer()
                            beer.name = data.get("name").toString()
                            beer.msg = data.get("msg").toString()
//                            beer.tags = data.get("tags")
//                            beer.location = data.get("location")
                            mBeers.add(beer)
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.exception)
                    }
                }


    }
    private fun locationStart() {
        Log.d("debug", "locationStart()")

        // LocationManager インスタンス生成
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (locationManager != null && locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d("debug", "location manager Enabled")
        } else {
            // GPSを設定するように促す
            val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(settingsIntent)
            Log.d("debug", "not gpsEnable, startActivity")
        }

        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1000)

            Log.d("debug", "checkSelfPermission false")
            return
        }

        locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000, 50f, this)

    }


    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        when (status) {
            LocationProvider.AVAILABLE -> Log.d("debug", "LocationProvider.AVAILABLE")
            LocationProvider.OUT_OF_SERVICE -> Log.d("debug", "LocationProvider.OUT_OF_SERVICE")
            LocationProvider.TEMPORARILY_UNAVAILABLE -> Log.d("debug", "LocationProvider.TEMPORARILY_UNAVAILABLE")
        }
    }

    private fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later")
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show()
            activity.finish()
            return false
        }
        val openGlVersionString = (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion
        if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later")
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show()
            activity.finish()
            return false
        }
        return true
    }

    private fun sort(beers: List<Beer>): List<Beer> {
        return beers.stream().sorted { beer1, beer2 ->
            if(beer1.distance == null || beer2.distance == null) {
                return@sorted 0
            } else if (beer1.distance!! > beer2.distance!!) {
                return@sorted -1
            } else {
                return@sorted 1
            }
        }.collect(Collectors.toList())
    }

    private fun calcDistance() {
        mBeers.forEach { beer ->
            nowLocation?.let {
                beer.calcDist(it.latitude, it.longitude)
            }
        }
    }

    private fun getBeerEstmate(tag: String): Beer? {
        sortedBeers?.forEach { beer ->
            beer.checkTag(tag)?.let {
                return it
            }
        }
        return null
    }

    override fun onLocationChanged(location: Location) {
        nowLocation = location
        Log.d("debug", "Latitude: ${location.latitude}")
        Log.d("debug", "Longitude: ${location.longitude}")

    }

    override fun onProviderEnabled(p0: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onProviderDisabled(p0: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 1000) {
            // 使用が許可された
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("debug", "checkSelfPermission true")

                locationStart()

            } else {
                // それでも拒否された時の対応
                val toast = Toast.makeText(this,
                        "これ以上なにもできません", Toast.LENGTH_SHORT)
                toast.show()
            }
        }
    }
}
