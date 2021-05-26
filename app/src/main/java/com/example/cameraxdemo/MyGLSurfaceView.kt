package com.example.cameraxdemo

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.SurfaceHolder

class MyGLSurfaceView(context: Context, attributeSet: AttributeSet) :
    GLSurfaceView(context, attributeSet) {

    private val renderer: MainRenderer = MainRenderer(this)

    init {
        setEGLContextClientVersion(2);
        setRenderer(renderer);
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        renderer.close()
        super.surfaceDestroyed(holder)
    }

    fun getRenderer() = renderer

}