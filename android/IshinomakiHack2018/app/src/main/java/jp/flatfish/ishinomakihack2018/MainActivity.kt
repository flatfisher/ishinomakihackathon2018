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


class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.java!!.getSimpleName()
    private val MIN_OPENGL_VERSION = 3.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }
        setContentView(R.layout.activity_main)

        //Firebase
        val db: FirebaseFirestore = FirebaseFirestore.getInstance()
        db.collection("samples")
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        for (document in task.result) {
                            Log.d(TAG, document.id + " => " + document.data)
//                            val beer = Beer()
//                            beer.name = document.data.get("name").toString()
//                            beer.msg = document.data.get("msg").toString()
//                            beer.tags = document.data.get("tags") as Array<String>
//                            beer.location = document.data.get("location") as GeoPoint?
//                            Log.d("Beer", beer.toString())
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.exception)
                    }
                }

    }

    fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
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
}
