package jp.flatfish.ishinomakihack2018

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.support.v4.content.FileProvider
import android.util.Log
import android.view.MotionEvent
import android.view.PixelCopy
import android.widget.TextView
import android.widget.Toast
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionLabel
import kotlinx.android.synthetic.main.test_view.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max


class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.java!!.getSimpleName()
    private val MIN_OPENGL_VERSION = 3.0

    private var arFragment: ArFragment? = null
    private var viewRenderable: ViewRenderable? = null
    private var labelList = mutableListOf<FirebaseVisionLabel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }
        setContentView(R.layout.activity_main)

//        val textView = TextView(this)
//        textView.text = "hello

        //ARCore
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?
        ViewRenderable.builder()
                .setView(this, R.layout.test_view)
                .build()
                .thenAccept({ renderable -> viewRenderable = renderable })

        arFragment?.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, motionEvent: MotionEvent ->
            Log.d(TAG+"hogehoge", "tapped!!")
            // Create the Anchor.
//            val anchor = hitResult.createAnchor()
//            val anchorNode = AnchorNode(anchor)
//            anchorNode.setParent(arFragment?.getArSceneView()?.scene)

            // Create the transformable andy and add it to the anchor.
//            val andy = TransformableNode(arFragment?.getTransformationSystem())
//            andy.setParent(anchorNode)
//            andy.renderable = viewRenderable
//            andy.select()
//            val bitmap = getBitmap()

            takePhoto()
        }

        //Firebase
        val db: FirebaseFirestore = FirebaseFirestore.getInstance()
        db.collection("samples")
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        for (document in task.result) {
                            Log.d(TAG, document.id + " => " + document.data)
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

//    README: 画像を取得したあとMLKitでラベルを検出する
    private fun takePhoto() {
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
                            }
                            labelList.addAll(labels)
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                        }
                        .addOnCompleteListener{
                            labelList.forEach {
                                Log.d("confirm hogehoge", "${it.label}: ${it.confidence}")
                            }
                        }
            } else {
                val toast = Toast.makeText(this,
                        "Failed to copyPixels: $copyResult", Toast.LENGTH_LONG)
                toast.show()
            }
            handlerThread.quitSafely()
        }, Handler(handlerThread.looper))
    }
}
