package jp.flatfish.ishinomakihack2018

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.PixelCopy
import android.widget.TextView
import android.widget.Toast
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.google.firebase.firestore.FirebaseFirestore
import android.support.v4.app.ActivityCompat
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.Manifest
import android.location.LocationManager
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationProvider
import android.provider.Settings
import com.google.firebase.firestore.GeoPoint
import java.util.stream.Collectors
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionLabel


class MainActivity : AppCompatActivity(), LocationListener {
    private val TAG = MainActivity::class.java.simpleName

    private val MIN_OPENGL_VERSION = 3.0
    private var mBeers = mutableListOf<Beer>()
    private var sortedBeers = mutableListOf<Beer>()

    private var nowLocation: Location? = null

    private var locationManager: LocationManager? = null

    private var arFragment: ArFragment? = null
    private var viewRenderable: ViewRenderable? = null
    //private var labelList = mutableListOf<FirebaseVisionLabel>()

    private var textView:TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }
        setContentView(R.layout.activity_main)

//        README: Prepare ARCore
        textView = LayoutInflater.from(this).inflate(R.layout.text_view, null) as TextView
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?
        ViewRenderable.builder()
                .setView(this, textView)
                .build()
                .thenAccept({ renderable -> viewRenderable = renderable })

        arFragment?.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, motionEvent: MotionEvent ->
            Log.d(TAG + "hogehoge", "tapped!!")
            // Create the Anchor.
            val anchor = hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arFragment?.getArSceneView()?.scene)

            calcDistance()
            sortedBeers = sort(mBeers) as MutableList<Beer>

            takePhoto(anchorNode)
        }

        //README: Getting data from firestore
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
                            val tags = data.get("tags").toString()
                            beer.tags = tags.substring(1, tags.lastIndex).split(",", limit = 0).toTypedArray()
                            beer.location = data.get("location") as? GeoPoint
                            mBeers.add(beer)
                            Log.d("hogehoge", beer.toString())
                            Log.d(TAG, document.id + " => " + document.data)
                        }

                        calcDistance()
                        sortedBeers = sort(mBeers) as MutableList<Beer>

                        Toast.makeText(this, "Get Firestore Data", Toast.LENGTH_SHORT).show()

                    } else {
                        Log.w(TAG, "Error getting documents.", task.exception)
                    }
                }

//        README: Prepare getting current location
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
                100, 50f, this)

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
            } else if (beer1.distance!! < beer2.distance!!) {
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
                Log.d("hogehoge", "distance:${beer.distance}")
            }
        }
    }

    private fun getBeerEstimate(tag: String): Beer? {
        sortedBeers?.forEach { beer ->
            if (beer.distance?:Float.MAX_VALUE > DISTANCE_MAX) {
                return@forEach
            }
            beer.checkTag(tag)?.let {
                return it
            }
        }
        Log.d("hogehoge", "distance")
        return null
    }

    override fun onLocationChanged(location: Location) {
        nowLocation = location

        calcDistance()
        sortedBeers = sort(mBeers) as MutableList<Beer>
        Toast.makeText(this, "Location Changed", Toast.LENGTH_SHORT).show()

        Log.d("hogehoge1", "Latitude: ${location.latitude}")
        Log.d("hogehoge", "Longitude: ${location.longitude}")
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
    //    README: 画像を取得したあとMLKitでラベルを検出する
    private fun takePhoto(anchorNode:AnchorNode) {
        val view = arFragment?.getArSceneView()

        // Create a bitmap the size of the scene view.
        val bitmap = Bitmap.createBitmap(view!!.width, view.height,
                Bitmap.Config.ARGB_8888)

        // Create a handler thread to offload the processing of the image.
        val handlerThread = HandlerThread("PixelCopier")
        handlerThread.start()
        // Make the request to copy.
        PixelCopy.request(view, bitmap, { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {
                Log.d(TAG+"hogehoge", "copyResult")
//              README: ラベルの検出
                val image = FirebaseVisionImage.fromBitmap(bitmap)

                FirebaseVision.getInstance()
                        .visionLabelDetector
                        .detectInImage(image)
                        .addOnSuccessListener { labels ->
                            labels.forEach {
                                Log.d("labeling hogehoge", "${it.label}: ${it.confidence}")
                                val beer = getBeerEstimate(it.label)
                                beer?.let {
                                    Log.d("msg", beer?.msg)
                                    val andy = TransformableNode(arFragment?.getTransformationSystem())
                                    andy.setParent(anchorNode)
                                    andy.renderable = viewRenderable
                                    andy.select()
                                    textView?.text = it.msg
                                    return@forEach
                                }
                            }
                            //labelList.addAll(labels)
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                        }
                        .addOnCompleteListener{
                           /* labelList.forEach {
                                Log.d("confirm hogehoge", "${it.label}: ${it.confidence}")
                                val beer = getBeerEstimate(it.label)
                                beer?.let {
                                    Log.d("msg", beer?.msg)
                                    val andy = TransformableNode(arFragment?.getTransformationSystem())
                                    andy.setParent(anchorNode)
                                    andy.renderable = viewRenderable
                                    andy.select()
                                    textView?.text = it.msg
                                    return@forEach
                                }
                            }*/

                        }
            } else {
                val toast = Toast.makeText(this,
                        "Failed to copyPixels: $copyResult", Toast.LENGTH_LONG)
                toast.show()
            }
            handlerThread.quitSafely()
        }, Handler(handlerThread.looper))
    }

    companion object {
        const val DISTANCE_MAX: Float = 100.0f
    }
}
