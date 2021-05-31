package com.example.cameraxdemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val liveData = MutableLiveData<CameraProviderResult>()
    private var mCameraProvider: ProcessCameraProvider? = null
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA
    )
    var cameraView: MyGLSurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val surfaceView = findViewById<MyGLSurfaceView>(R.id.cameraView)
        cameraView = surfaceView
        getCameraProvider()


    }

    override fun onStart() {
        super.onStart()
        if (allPermissionsGranted()) {
            getCamera()
        } else {
            mRequestPermissions.launch(REQUIRED_PERMISSIONS)
        }
    }

    override fun onStop() {
        super.onStop()
        mCameraProvider?.unbindAll()
    }

    // **************************** Permission handling code start *******************************//
    private val mRequestPermissions =
        registerForActivityResult(
            RequestMultiplePermissions(),
            ActivityResultCallback<Map<String?, Boolean?>> { result ->
                for (permission in REQUIRED_PERMISSIONS) {
                    if (result[permission] != true) {
                        Toast.makeText(
                            this@MainActivity, "Permissions not granted",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }

                // All permissions granted.
                if (mCameraProvider != null) {
                    startCamera()
                }
            })

    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    private fun getCameraProvider() {
        liveData.observe(this, androidx.lifecycle.Observer {
            if (it.cameraProvider != null) {
                mCameraProvider = it.cameraProvider
                Log.d(
                    "FAFA", "Set camera provider",
                    it.error
                );
                if (allPermissionsGranted()) {
                    startCamera()
                }
            } else {
                Log.e(
                    "FAFA", "Failed to retrieve ProcessCameraProvider",
                    it.error
                );
                Toast.makeText(
                    applicationContext,
                    "Unable to initialize CameraX. See logs for details.", Toast.LENGTH_LONG
                ).show();
            }
        })
    }

    private fun startCamera() {
        // Keep screen on for this app. This is just for convenience, and is not required.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        cameraView?.let { glSurfaceView ->
            glSurfaceView.post {
                Log.d("FAFA", "Added observer for surface texture")
                val displayMetrics = DisplayMetrics().also {
                    glSurfaceView.display.getRealMetrics(it)
                }
                Log.d(
                    "FAFA",
                    "height : ${displayMetrics.heightPixels}, width : ${displayMetrics.widthPixels}"
                )
                val preview =
                    Preview.Builder().setTargetRotation(glSurfaceView.display.rotation)
                        .setTargetResolution(
                            Size(
                                displayMetrics.widthPixels,
                                displayMetrics.heightPixels
                            )
                        ).build()
                //mRenderer.attachInputPreview(preview)
                val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
                val rederer = glSurfaceView.getRenderer()
                val executor = Executors.newSingleThreadExecutor()
                val previewSurfaceProvider = PreviewSurfaceProvider(rederer, executor)
                preview.setSurfaceProvider(executor, previewSurfaceProvider)
                Log.d("FAFA", "bound camera to lifecycle")
                mCameraProvider?.bindToLifecycle(this, cameraSelector, preview)
            }
        }

    }

    private fun getCamera() {
        //tryConfigureCameraProvider();
        try {
            val cameraProviderFuture =
                ProcessCameraProvider.getInstance(application)
            cameraProviderFuture.addListener(Runnable {
                try {
                    val cameraProvider =
                        cameraProviderFuture.get()
                    liveData.value = CameraProviderResult(cameraProvider = cameraProvider)
                } catch (e: ExecutionException) {
                    liveData.value = CameraProviderResult(error = e.cause)
                } catch (e: InterruptedException) {
                    liveData.value = CameraProviderResult(error = e.cause)
                }
            }, ContextCompat.getMainExecutor(getApplication()))
        } catch (e: IllegalStateException) {
            // Failure during ProcessCameraProvider.getInstance()
            liveData.value = CameraProviderResult(error = e.cause)
        }

    }

    data class CameraProviderResult(
        val cameraProvider: ProcessCameraProvider? = null,
        val error: Throwable? = null
    )
}