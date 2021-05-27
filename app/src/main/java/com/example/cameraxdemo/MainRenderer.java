package com.example.cameraxdemo;

import android.graphics.Camera;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private final String vss =
            "attribute vec2 vPosition;\n" +
                    "attribute vec2 vTexCoord;\n" +
                    "varying vec2 texCoord;\n" +
                    "void main() {\n" +
                    "  texCoord = vTexCoord;\n" +
                    "  gl_Position = vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
                    "}";

    private final String fss =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "varying vec2 texCoord;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture,texCoord);\n" +
                    "}";

    private int[] hTex;
    private FloatBuffer pVertex;
    private FloatBuffer pTexCoord;
    private int hProgram;

    private MutableLiveData<SurfaceTexture> surfaceTextureLiveData = new MediatorLiveData<>();

    public LiveData<SurfaceTexture> getmSTexture() {
        return surfaceTextureLiveData;
    }

    private SurfaceTexture mSTexture;

    private boolean mUpdateST = false;

    private MyGLSurfaceView mView;

    MainRenderer(MyGLSurfaceView view) {
        mView = view;
        float[] vtmp = {
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -0.8f, 1.0f,
                0.8f, 1.0f
        };
        /*float[] ttmp = {
                0, 1, // X1,Y1
                1, 1, // X2,Y2
                0, 0, // X3,Y3
                1, 0 // X4,Y4
        };*/
        float[] ttmp = {
                1, 0, // X1,Y1
                0, 0, // X2,Y2
                1, 1, // X3,Y3
                0, 1 // X4,Y4
        };
        /*
         -1, -1      1, 1
         -1, 1      0, 1
         1, -1      1, 0
         1, 1       0, 0

         -1, 1      1, 1       0, 0     1, 0
            ***********         **********
            *         *         *        *
            *         *         *        *
            ***********         **********
         -1,-1      1, -1      0,1       1,1
         */
        pVertex = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        pVertex.put(vtmp);
        pVertex.position(0);
        pTexCoord = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        pTexCoord.put(ttmp);
        pTexCoord.position(0);
    }

    public void close() {
        mUpdateST = false;
        mSTexture.release();
        deleteTex();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initTex();
        mSTexture = new SurfaceTexture(hTex[0]);
        Log.d("FAFA", "updated live data for surface texture");
        mSTexture.setOnFrameAvailableListener(this);
        surfaceTextureLiveData.postValue(mSTexture);
        GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);

        hProgram = loadShader(vss, fss);
    }

    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        synchronized (this) {
            if (mUpdateST) {
                mSTexture.updateTexImage();
                mUpdateST = false;
            }
        }

        GLES20.glUseProgram(hProgram);

        int ph = GLES20.glGetAttribLocation(hProgram, "vPosition");
        int tch = GLES20.glGetAttribLocation(hProgram, "vTexCoord");
        int th = GLES20.glGetUniformLocation(hProgram, "sTexture");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, hTex[0]);
        GLES20.glUniform1i(th, 0);

        GLES20.glVertexAttribPointer(ph, 2, GLES20.GL_FLOAT, false, 4 * 2, pVertex);
        GLES20.glVertexAttribPointer(tch, 2, GLES20.GL_FLOAT, false, 4 * 2, pTexCoord);
        GLES20.glEnableVertexAttribArray(ph);
        GLES20.glEnableVertexAttribArray(tch);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFlush();
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        mSTexture.setDefaultBufferSize(width, height);
        GLES20.glViewport(0, 0, width, height);
        Log.d("FAFA", "MainRenderer : onSurfaceChanged - width : " + width + " , height : " + height);
    }

    private void initTex() {
        hTex = new int[1];
        GLES20.glGenTextures(1, hTex, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, hTex[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
    }

    private void deleteTex() {
        GLES20.glDeleteTextures(1, hTex, 0);
    }

    public synchronized void onFrameAvailable(SurfaceTexture st) {
        mUpdateST = true;
        mView.requestRender();
    }

    private static int loadShader(String vss, String fss) {
        int vshader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vshader, vss);
        GLES20.glCompileShader(vshader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(vshader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e("Shader", "Could not compile vshader");
            Log.v("Shader", "Could not compile vshader:" + GLES20.glGetShaderInfoLog(vshader));
            GLES20.glDeleteShader(vshader);
            vshader = 0;
        }

        int fshader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fshader, fss);
        GLES20.glCompileShader(fshader);
        GLES20.glGetShaderiv(fshader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e("Shader", "Could not compile fshader");
            Log.v("Shader", "Could not compile fshader:" + GLES20.glGetShaderInfoLog(fshader));
            GLES20.glDeleteShader(fshader);
            fshader = 0;
        }

        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vshader);
        GLES20.glAttachShader(program, fshader);
        GLES20.glLinkProgram(program);

        return program;
    }
}
