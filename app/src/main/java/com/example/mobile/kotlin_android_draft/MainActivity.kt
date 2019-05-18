package com.example.mobile.kotlin_android_draft

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File

/**
 * Created by nestorsokil on 04.02.2017.
 */

@SuppressWarnings( "deprecation" )
class MainActivity : AppCompatActivity() {
    lateinit var surfaceHolder: SurfaceHolder
    lateinit var cameraView: SurfaceView
    lateinit var resultView: TextView
    lateinit var btnDetect: Button
    lateinit var btnSpeak:  Button
    lateinit var btnAgain:  Button
    lateinit var btnSwitchCam: ImageButton

    lateinit var tesseract: TessBaseAPI

    var camera: android.hardware.Camera? = null
    var CAMERA_FACING: Int = Camera.CameraInfo.CAMERA_FACING_BACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        cameraView   = findViewById(R.id.surface_view) as SurfaceView
        resultView   = findViewById(R.id.text_result)  as TextView
        btnDetect    = findViewById(R.id.btnDetect)    as Button
        btnSpeak     = findViewById(R.id.btnSpeak)     as Button
        btnAgain     = findViewById(R.id.btnAgain)     as Button
        btnSwitchCam = findViewById(R.id.btnSwitchCam) as ImageButton

        camera = Camera.open()

        initTesseract()

        btnSwitchCam.setOnClickListener { _ -> switchCamera()        }
        btnDetect.setOnClickListener    { _ -> takePictureCallback() }
        btnAgain.setOnClickListener     { _ -> tryAgainCallback()    }

        surfaceHolder = cameraView.holder
        surfaceHolder.addCallback(surfaceCallback)
    }

    override fun onResume() {
        super.onResume()
        // TODO: crashing on 4.2.2 (on app start)
        camera = Camera.open()
    }

    override fun onPause(){
        super.onPause()
        camera?.setPreviewCallback(null)
        camera?.stopPreview()
        camera?.release()
        camera = null
    }

    private val surfaceCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder?) {

            camera!!.setPreviewDisplay(holder)

            val previewSize = camera!!.parameters.previewSize
            val aspect = previewSize.width / previewSize.height

            val surfaceWidth = cameraView.width
            val surfaceHeight = cameraView.height

            val layoutParams = cameraView.layoutParams
            if(resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE){
                camera!!.setDisplayOrientation(0)
                layoutParams.height = surfaceHeight
                layoutParams.width  = surfaceHeight / aspect
            } else {
                camera!!.setDisplayOrientation(0)
                layoutParams.width  = surfaceWidth
                layoutParams.height = surfaceWidth / aspect
            }
            cameraView.layoutParams = layoutParams
            camera!!.startPreview()
        }

        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {}
        override fun surfaceDestroyed(holder: SurfaceHolder?) {}
    }

    private fun takePictureCallback() {
        camera?.takePicture(null, null, null, { bytes, _ ->
            this@MainActivity.resultView.post { ->
                val n = System.nanoTime()
                val snap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                Log.i("DECODE TIME MS: ", ((System.nanoTime() - n) / 1000000).toString())
                if (snap != null) {
                    val m = System.nanoTime()
                    val bin = ImageUtils.binarize(snap)
                    tesseract.setImage(bin)
                    val parsed = tesseract.utF8Text
                    this@MainActivity.resultView.text = parsed
                    Log.i("TESSERACT TIME MS: ", ((System.nanoTime() - m) / 1000000).toString())

                    btnDetect.visibility = View.INVISIBLE
                    btnSpeak.visibility  = View.VISIBLE
                    btnAgain.visibility  = View.VISIBLE
                }
            }
        })
    }

    private fun tryAgainCallback() {
        btnAgain.visibility  = View.INVISIBLE
        btnSpeak.visibility  = View.INVISIBLE
        btnDetect.visibility = View.VISIBLE
        this@MainActivity.resultView.text = ""
        camera?.startPreview()
    }

    private fun switchCamera(){
        camera?.stopPreview()
        camera?.release()

        CAMERA_FACING = if(CAMERA_FACING == Camera.CameraInfo.CAMERA_FACING_BACK)
            Camera.CameraInfo.CAMERA_FACING_FRONT
        else Camera.CameraInfo.CAMERA_FACING_BACK
        camera = Camera.open(CAMERA_FACING)
        camera!!.setPreviewDisplay(surfaceHolder)
        camera!!.setDisplayOrientation(0)
        camera!!.startPreview()
    }

    private fun initTesseract() {
        val localFolder = "tesseract"
        val resourceFolder = "tessdata"
        val sep = File.separator
        val path = filesDir.toString() + sep + localFolder + sep
        FileUtils.checkFile(File(path + resourceFolder + sep), path, assets)

        tesseract = TessBaseAPI()
        tesseract.init(path, "ukr")
    }
}
