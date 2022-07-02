package com.example.testgl

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGLRenderer(context: Context) : GLSurfaceView.Renderer {
    private var mTriangles: Vector<Triangle> = Vector(1)

    // vPMatrix is an abbreviation for "Model View Projection Matrix"
    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val rotationMatrix = FloatArray(16)

    private var mActivityContext: Context = context

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        // initialize a triangles
        mTriangles.add(Triangle())

        mTriangles[0].loadTexture(mActivityContext, R.drawable.texture, R.drawable.brick)
        // Set the background frame color
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)
    }

    private val constIncrement = 0.02f

    override fun onDrawFrame(unused: GL10) {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 5f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)


                // Calculate the projection and view transformation
                Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

                val scratch = FloatArray(16)
                // Create a rotation transformation for the triangle
                val time = SystemClock.uptimeMillis() % 4000L
                val angle = 0.090f * time.toInt()
                Matrix.setRotateM(rotationMatrix, 0, angle, 1.0f, 0f, 0.0f)

        // Calculate the projection and view transformation
        Matrix.multiplyMM(scratch, 0, vPMatrix, 0, rotationMatrix, 0)

        mTriangles[0].draw(scratch)
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio: Float = width.toFloat() / height.toFloat()

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)

    }
}

class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {

    private val renderer: MyGLRenderer

    init {

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)

        renderer = MyGLRenderer(context)

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(renderer)
    }
}

class MainActivity : Activity() {
    private lateinit var gLView: GLSurfaceView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity.
        gLView = MyGLSurfaceView(this)
        setContentView(gLView)
    }
}

class Triangle {
    private val vertexShaderCode =
    // This matrix member variable provides a hook to manipulate
        // the coordinates of the objects that use this vertex shader
        "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "attribute vec2 TexCoordinate;" +
                "varying vec2 v_TexCoordinate;" +
                "void main() {" +
                "  v_TexCoordinate = TexCoordinate;" +
                // the matrix must be included as a modifier of gl_Position
                // Note that the uMVPMatrix factor *must be first* in order
                // for the matrix multiplication product to be correct.
                "  gl_Position = uMVPMatrix * vPosition;" +
                "}"

    // Use to access and set the view transformation
    private var vPMatrixHandle: Int = 0


    private val fragmentShaderCode =
        "precision mediump float;" +
                "uniform sampler2D u_Texture;" +
                "uniform sampler2D back_Texture;" +
                "varying vec2 v_TexCoordinate;" +
                "void main() {" +
                "  if (gl_FrontFacing) {" +
                "    gl_FragColor = texture2D(u_Texture, v_TexCoordinate);" +
                "  } else {" +
                "    gl_FragColor = texture2D(back_Texture, v_TexCoordinate);" +
                "  }" +
                "}"



    private fun loadShader(type: Int, shaderCode: String): Int {

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        return GLES20.glCreateShader(type).also { shader ->

            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    private var mTextureDataHandle = Vector<Int>()

    fun loadTexture(context: Context, resourceId: Int, resourceId2: Int): Vector<Int> {
        val textureHandle = IntArray(2)

        GLES20.glGenTextures(2, textureHandle, 0)
        if (textureHandle[0] != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])

            val options = BitmapFactory.Options()
            options.inScaled = false
            var bitmap: Bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options)
            var matrix: android.graphics.Matrix = android.graphics.Matrix()
            matrix.postRotate(180.0f)
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

            bitmap.recycle()

            mTextureDataHandle.add(textureHandle[0])
        } else {
            throw RuntimeException("Error loading texture.")
        }

        if (textureHandle[1] != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[1])

            val options = BitmapFactory.Options()
            options.inScaled = false
            var bitmap: Bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId2, options)
            var matrix: android.graphics.Matrix = android.graphics.Matrix()
            matrix.postRotate(180.0f)
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

            bitmap.recycle()

            mTextureDataHandle.add(textureHandle[1])
        } else {
            throw RuntimeException("Error loading texture.")
        }
        return mTextureDataHandle
    }

    private var mProgram: Int

  init {

        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // create empty OpenGL ES Program
        mProgram = GLES20.glCreateProgram().also {

            // add the vertex shader to program
            GLES20.glAttachShader(it, vertexShader)

            // add the fragment shader to program
            GLES20.glAttachShader(it, fragmentShader)

            // creates OpenGL ES program executables
            GLES20.glLinkProgram(it)
        }
    }

    private var positionHandle: Int = 0

    // number of coordinates per vertex in this array
    private val COORDS_PER_VERTEX = 3
    private var triangleCoords = floatArrayOf(     // in counterclockwise order:
        -0.5f, 0.0f, 0.0f,      // bottom left
        0.0f, 1.0f, 0.0f,    // top
        0.5f, 0.0f, 0.0f      // bottom right
    )

    private val vertexCount: Int = triangleCoords.size / COORDS_PER_VERTEX
    private val vertexStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

    private var vertexBuffer: FloatBuffer =
        // (number of coordinate values * 4 bytes per float)
        ByteBuffer.allocateDirect(triangleCoords.size * 4).run {
            // use the device hardware's native byte order
            order(ByteOrder.nativeOrder())

            // create a floating point buffer from the ByteBuffer
            asFloatBuffer().apply {
                // add the coordinates to the FloatBuffer
                put(triangleCoords)
                // set the buffer to read the first coordinate
                position(0)
            }
        }

    fun draw(mvpMatrix: FloatArray) {
        GLES20.glUseProgram(mProgram)

        // get handle to vertex shader's vPosition member
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition").also {
            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(it)

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(
                it,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                vertexBuffer
            )
        }

        // get handle to shape's transformation matrix
        vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")

        // Pass the projection and view transformation to the shader
        GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

        var textureUniformHandle: Int = GLES20.glGetUniformLocation(mProgram, "u_Texture")

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle[0])
        GLES20.glUniform1i(textureUniformHandle, 0)

        textureUniformHandle = GLES20.glGetUniformLocation(mProgram, "back_Texture")

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle[1])
        GLES20.glUniform1i(textureUniformHandle, 1)

        val textureCoordinateData = floatArrayOf(
            1.0f, 0.0f,
            0.5f, 1.0f,
            0.0f, 0.0f,
        )

        val textureCoordinates: FloatBuffer =
            ByteBuffer.allocateDirect(textureCoordinateData.size * 4).run {
                order(ByteOrder.nativeOrder())

                asFloatBuffer().apply {
                    put(textureCoordinateData)
                    position(0)
                }
            }


        val textureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "TexCoordinate").also {

            GLES20.glEnableVertexAttribArray(it)
            GLES20.glVertexAttribPointer(
               it,
                2,
                GLES20.GL_FLOAT,
                false,
                8,
                textureCoordinates
            )
        }

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionHandle)

    }

}
