package com.arindam.camerax.data.camera

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.camera.core.CameraEffect
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import com.arindam.camerax.domain.model.ColorFilterType
import com.arindam.camerax.util.log.Logger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.Executor

/**
 * GPU color-matrix [SurfaceProcessor] used as a CameraX [CameraEffect] on preview and video.
 * Update [colorMatrix] without rebinding to switch filters.
 */
class ColorFilterProcessor : SurfaceProcessor, SurfaceTexture.OnFrameAvailableListener {

    private val thread = HandlerThread("CameraXColorFilter").apply { start() }
    val handler = Handler(thread.looper)
    val glExecutor: Executor = Executor { handler.post(it) }

    @Volatile
    var colorMatrix: FloatArray = ColorFilters.glMatrix(ColorFilterType.NONE)

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglConfig: EGLConfig? = null
    private var inputTexture: SurfaceTexture? = null
    private var inputSurface: Surface? = null
    private var outputEglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var outputSurface: Surface? = null
    private var surfaceOutput: SurfaceOutput? = null
    private var program = 0
    private var oesTextureId = 0
    private var aPosition = 0
    private var aTexCoord = 0
    private var uTexMatrix = 0
    private var uColorMatrix = 0
    private var released = false

    override fun onInputSurface(request: SurfaceRequest) {
        handler.post {
            if (released) {
                request.willNotProvideSurface()
                return@post
            }
            initEgl()
            val size = request.resolution
            oesTextureId = createOesTexture()
            val texture = SurfaceTexture(oesTextureId).apply {
                setDefaultBufferSize(size.width, size.height)
                setOnFrameAvailableListener(this@ColorFilterProcessor, handler)
            }
            val surface = Surface(texture)
            inputTexture = texture
            inputSurface = surface
            request.provideSurface(surface, glExecutor) {
                inputSurface?.release()
                inputTexture?.release()
                inputSurface = null
                inputTexture = null
            }
        }
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        handler.post {
            if (released) {
                surfaceOutput.close()
                return@post
            }
            initEgl()
            val surface = surfaceOutput.getSurface(glExecutor) {
                releaseOutput()
                surfaceOutput.close()
            }
            this.surfaceOutput = surfaceOutput
            outputSurface = surface
            outputEglSurface = EGL14.eglCreateWindowSurface(
                eglDisplay,

                eglConfig,
                surface,
                intArrayOf(EGL14.EGL_NONE),
                0
            )
            checkEgl("eglCreateWindowSurface")
            if (program == 0) {
                program = createProgram()
                aPosition = GLES20.glGetAttribLocation(program, "aPosition")
                aTexCoord = GLES20.glGetAttribLocation(program, "aTexCoord")
                uTexMatrix = GLES20.glGetUniformLocation(program, "uTexMatrix")
                uColorMatrix = GLES20.glGetUniformLocation(program, "uColorMatrix")
            }
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        if (released || outputEglSurface == EGL14.EGL_NO_SURFACE) return
        surfaceTexture.updateTexImage()
        val texMatrix = FloatArray(16)
        surfaceTexture.getTransformMatrix(texMatrix)
        val drawMatrix = FloatArray(16)
        surfaceOutput?.updateTransformMatrix(drawMatrix, texMatrix) ?: return

        EGL14.eglMakeCurrent(eglDisplay, outputEglSurface, outputEglSurface, eglContext)
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(uTexMatrix, 1, false, drawMatrix, 0)
        GLES20.glUniformMatrix4fv(uColorMatrix, 1, false, colorMatrix, 0)

        VERTEX.position(0)
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 0, VERTEX)
        TEXTURE.position(0)
        GLES20.glEnableVertexAttribArray(aTexCoord)
        GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, 0, TEXTURE)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        val timestamp = surfaceTexture.timestamp
        EGLExt.eglPresentationTimeANDROID(eglDisplay, outputEglSurface, timestamp)
        EGL14.eglSwapBuffers(eglDisplay, outputEglSurface)
    }

    fun release() {
        handler.post {
            released = true
            releaseOutput()
            inputSurface?.release()
            inputTexture?.release()
            inputSurface = null
            inputTexture = null
            if (program != 0) {
                GLES20.glDeleteProgram(program)
                program = 0
            }
            if (oesTextureId != 0) {
                GLES20.glDeleteTextures(1, intArrayOf(oesTextureId), 0)
                oesTextureId = 0
            }
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(
                    eglDisplay,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT
                )
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                EGL14.eglReleaseThread()
                EGL14.eglTerminate(eglDisplay)
            }
            eglDisplay = EGL14.EGL_NO_DISPLAY
            eglContext = EGL14.EGL_NO_CONTEXT
            thread.quitSafely()
        }
    }

    private fun releaseOutput() {
        if (outputEglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(eglDisplay, outputEglSurface)
            outputEglSurface = EGL14.EGL_NO_SURFACE
        }
        outputSurface = null
        surfaceOutput = null
    }

    private fun initEgl() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) return
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        EGL14.eglInitialize(eglDisplay, version, 0, version, 1)
        val configs = arrayOfNulls<EGLConfig>(1)
        val num = IntArray(1)
        EGL14.eglChooseConfig(
            eglDisplay,
            intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGLExt.EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
            ),
            0,
            configs,
            0,
            1,
            num,
            0
        )
        eglConfig = configs[0]
        eglContext = EGL14.eglCreateContext(
            eglDisplay,
            eglConfig,
            EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE),
            0
        )
        if (eglContext == EGL14.EGL_NO_CONTEXT || eglConfig == null) {
            Logger.error(TAG, "Falling back to OpenGL ES 2 context")
            EGL14.eglChooseConfig(
                eglDisplay,
                intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                    EGLExt.EGL_RECORDABLE_ANDROID, 1,
                    EGL14.EGL_NONE
                ),
                0,
                configs,
                0,
                1,
                num,
                0
            )
            eglConfig = configs[0]
            eglContext = EGL14.eglCreateContext(
                eglDisplay,
                eglConfig,
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                0
            )
        }
        checkEgl("eglCreateContext")
        val placeholder = EGL14.eglCreatePbufferSurface(
            eglDisplay,
            eglConfig,
            intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE),
            0
        )
        EGL14.eglMakeCurrent(eglDisplay, placeholder, placeholder, eglContext)
    }

    private fun createOesTexture(): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, ids[0])
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        return ids[0]
    }

    private fun createProgram(): Int {
        val vertex = compile(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragment = compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vertex)
        GLES20.glAttachShader(prog, fragment)
        GLES20.glLinkProgram(prog)
        val link = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, link, 0)
        if (link[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(prog)
            GLES20.glDeleteProgram(prog)
            throw IllegalStateException("Filter shader link failed: $log")
        }
        return prog
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw IllegalStateException("Filter shader compile failed: $log")
        }
        return shader
    }

    private fun checkEgl(op: String) {
        val error = EGL14.eglGetError()
        if (error != EGL14.EGL_SUCCESS) {
            Logger.error(TAG, "$op failed: 0x${Integer.toHexString(error)}")
        }
    }

    companion object {
        private const val TAG = "ColorFilterGL"

        private val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec4 aTexCoord;
            uniform mat4 uTexMatrix;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = (uTexMatrix * aTexCoord).xy;
            }
        """.trimIndent()

        private val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTexCoord;
            uniform samplerExternalOES sTexture;
            uniform mat4 uColorMatrix;
            void main() {
                vec4 color = texture2D(sTexture, vTexCoord);
                gl_FragColor = uColorMatrix * color;
            }
        """.trimIndent()

        private val VERTEX = floatBuffer(
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f
        )
        private val TEXTURE = floatBuffer(
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f
        )

        private fun floatBuffer(vararg values: Float): FloatBuffer =
            ByteBuffer.allocateDirect(values.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(values)
                    position(0)
                }
    }
}

fun ColorFilterProcessor.asCameraEffect(targets: Int): CameraEffect {
    return ColorMatrixEffect(
        targets,
        glExecutor,
        this
    ) { error -> Logger.error("ColorFilterGL", "Effect error: ${error.message}") }
}

private class ColorMatrixEffect(
    targets: Int,
    executor: Executor,
    processor: SurfaceProcessor,
    errorListener: androidx.core.util.Consumer<Throwable>
) : CameraEffect(targets, executor, processor, errorListener)
