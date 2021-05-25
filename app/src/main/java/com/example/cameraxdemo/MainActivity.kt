package com.example.cameraxdemo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val liveData = MutableLiveData<CameraProviderResult>()
    private var mCameraProvider: ProcessCameraProvider? = null
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA
    )
    //var cameraView: PreviewView? = null
    var cameraView: TextureView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraView = findViewById<TextureView>(R.id.cameraView)
        cameraView?.surfaceTextureListener = object : SurfaceTextureListener {
            private var mSurface: Surface? = null
            override fun onSurfaceTextureAvailable(
                st: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                /*mSurface = Surface(st)
                    renderer.attachOutputSurface(
                        mSurface, Size(width, height),
                        Surfaces.toSurfaceRotationDegrees(textureView.getDisplay().getRotation())
                    )*/
                Log.d("FAFA", "surfaceTextureListener : onSurfaceTextureAvailable")
            }

            override fun onSurfaceTextureSizeChanged(
                st: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                /*renderer.attachOutputSurface(
                        mSurface, Size(width, height),
                        Surfaces.toSurfaceRotationDegrees(textureView.getDisplay().getRotation())
                    )*/
                Log.d("FAFA", "surfaceTextureListener : onSurfaceTextureSizeChanged")
            }

            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                /*val surface = mSurface
                    mSurface = null
                    renderer.detachOutputSurface().addListener({
                        surface!!.release()
                        st.release()
                    }, ContextCompat.getMainExecutor(textureView.getContext()))*/
                Log.d("FAFA", "surfaceTextureListener : onSurfaceTextureDestroyed")
                return false
            }

            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {
                Log.d("FAFA", "surfaceTextureListener : onSurfaceTextureUpdated")
            }
        }
        getCameraProvider()

        if (allPermissionsGranted()) {
            getCamera()
        } else {
            mRequestPermissions.launch(REQUIRED_PERMISSIONS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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
                Log.e(
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

        // Set the aspect ratio of Preview to match the aspect ratio of the view finder (defined
        // with ConstraintLayout).
        val preview =
            Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9).build()
        //mRenderer.attachInputPreview(preview)
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        /*preview.setSurfaceProvider(cameraView?.)
        mCameraProvider!!.bindToLifecycle(this, cameraSelector, preview)*/
        //cameraView?.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        cameraView?.let {

            val surfaceTexture = it.surfaceTexture
            val surface = Surface(surfaceTexture)
            val executor = Executors.newSingleThreadExecutor()
            val previewSurfaceProvider = PreviewSurfaceProvider(surface, executor)
            preview.setSurfaceProvider(executor,previewSurfaceProvider)
            mCameraProvider?.bindToLifecycle(this, cameraSelector, preview)
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