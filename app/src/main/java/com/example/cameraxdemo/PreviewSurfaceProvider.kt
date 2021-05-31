package com.example.cameraxdemo

import android.util.Log
import android.view.Surface
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.core.util.Consumer
import java.util.concurrent.Executor

class PreviewSurfaceProvider(private val renderer: MainRenderer, private val executor: Executor) : Preview.SurfaceProvider {

    override fun onSurfaceRequested(request: SurfaceRequest) {
        val surface = Surface(renderer.createTexture())
        request.provideSurface(surface, executor, Consumer { result: SurfaceRequest.Result ->
            run {
                surface.release()
                Log.d(
                    "FAFA",
                    "PreviewSurfaceProvider onSurfaceRequested consumer result : ${result.resultCode}"
                )
            }
        })
    }
}
