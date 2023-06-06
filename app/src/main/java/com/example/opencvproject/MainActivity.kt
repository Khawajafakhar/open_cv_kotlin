package com.example.opencvproject

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.opencvproject.databinding.ActivityMainBinding
import org.opencv.android.OpenCVLoader
import java.io.ByteArrayOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var mImageFrame: FrameLayout
    private lateinit var imageView: ImageView
    private lateinit var mPolygonView: PolygonView
    private var bitmap: Bitmap? = null
    private var button: Button? = null
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mImageFrame = findViewById(R.id.fl_image)
        imageView = findViewById(R.id.imageView)
        mPolygonView = findViewById(R.id.polygon_view)
        button = findViewById(R.id.button)

        val inputStream = assets.open("IMG_20230526_123327.jpg")
        bitmap = BitmapFactory.decodeStream(inputStream)
        try {
            mImageFrame.post(Runnable() {

                run {
                    bitmap?.let { setBitmap(it) }
                }
            })
        } catch (e: IOException) {
            e.printStackTrace()
        }
        button?.setOnClickListener {
            val points = mPolygonView.getPoints()
            Log.e("Points", points.toString())
            if (isScanPointsValid(points)) {
                ScanAsyncTask(points).execute()
            } else {
                showErrorDialog()
            }
        }

    }

    private fun setBitmap(original: Bitmap) {
        val scaledBitmap = scaledBitmap(original, mImageFrame.width, mImageFrame.height)
        imageView.setImageBitmap(scaledBitmap)
        val tempBitmap = (imageView.drawable as BitmapDrawable).bitmap
        val pointFs: Map<Int?, PointF?>? = getEdgePoints(tempBitmap)
        mPolygonView.setPoints(pointFs)
        mPolygonView.visibility = View.VISIBLE
        val padding = 10
        val layoutParams = FrameLayout.LayoutParams(
            tempBitmap.width + 2 * padding,
            tempBitmap.height + 2 * padding
        )
        layoutParams.gravity = Gravity.CENTER
        mPolygonView.layoutParams = layoutParams
    }

    private fun scaledBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        val m = Matrix()
        m.setRectToRect(
            RectF(0F, 0F, bitmap.width.toFloat(), bitmap.height.toFloat()), RectF(
                0F, 0F,
                width.toFloat(),
                height.toFloat()
            ), Matrix.ScaleToFit.CENTER
        )
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

    private fun getEdgePoints(tempBitmap: Bitmap): Map<Int?, PointF?>? {
        val pointFs: List<PointF>? = getContourEdgePoints(tempBitmap)
        return orderedValidEdgePoints(tempBitmap, pointFs)
    }

    //    private fun getContourEdgePoints(tempBitmap: Bitmap): List<PointF>? {
//        val points: FloatArray? = getPoints(tempBitmap)
//        val x1 = points!![0]
//        val x2 = points[1]
//        val x3 = points[2]
//        val x4 = points[3]
//        val y1 = points[4]
//        val y2 = points[5]
//        val y3 = points[6]
//        val y4 = points[7]
//        val pointFs: MutableList<PointF> = ArrayList()
//        pointFs.add(PointF(x1, y1))
//        pointFs.add(PointF(x2, y2))
//        pointFs.add(PointF(x3, y3))
//        pointFs.add(PointF(x4, y4))
//        return pointFs
//    }
    private fun getContourEdgePoints(tempBitmap: Bitmap): List<PointF>? {
        val points = getPoints(tempBitmap)
        Log.e("POINTS",points.contentToString())
        Log.e("NULL",(points != null).toString())
        Log.e("EMPTY",(points?.isNotEmpty()).toString())
        Log.e("SIZE",(points?.size!! >= 8).toString())

        if (points != null) {
            if (points.isNotEmpty() && points.size >= 8) {
                val x1 = points[0]
                val x2 = points[1]
                val x3 = points[2]
                val x4 = points[3]
                val y1 = points[4]
                val y2 = points[5]
                val y3 = points[6]
                val y4 = points[7]

                val pointFs: MutableList<PointF> = ArrayList()
                pointFs.add(PointF(x1, y1))
                pointFs.add(PointF(x2, y2))
                pointFs.add(PointF(x3, y3))
                pointFs.add(PointF(x4, y4))
                Log.e("POINTS",pointFs.toString())

                return pointFs
            }
        }

        return null
    }

    /**
     * A native method that is implemented by the 'opencvproject' native library,
     * which is packaged with this application.
     */
    // external fun stringFromJNI(): String
    //external fun validate(one: Long,two: Long): String

    companion object {
        // Used to load the 'opencvproject' library on application startup.
        init {
            System.loadLibrary("opencvproject")
            System.loadLibrary("opencv_java4");
        }
    }

    private external fun getScannedBitmap(
        bitmap: Bitmap?,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        x4: Float,
        y4: Float
    ): Bitmap?

    external fun getGrayBitmap(bitmap: Bitmap?): Bitmap?
    external fun getMagicColorBitmap(bitmap: Bitmap?): Bitmap?
    external fun getBWBitmap(bitmap: Bitmap?): Bitmap?
    external fun getPoints(bitmap: Bitmap?): FloatArray?
    //  external fun processDocument(inputMat: Long,outputMat: Long)

    private fun orderedValidEdgePoints(
        tempBitmap: Bitmap,
        pointFs: List<PointF>?
    ): Map<Int?, PointF?>? {
        var orderedPoints: Map<Int?, PointF?>? = mPolygonView.getOrderedPoints(pointFs)
        if (!mPolygonView.isValidShape(orderedPoints)) {
            orderedPoints = getOutlinePoints(tempBitmap, orderedPoints)
        }
        return orderedPoints
    }

//    private fun getOutlinePoints(tempBitmap: Bitmap): Map<Int?, PointF?> {
//        val outlinePoints: MutableMap<Int?, PointF?> = HashMap()
//        outlinePoints[0] = PointF(0F, 0F)
//        outlinePoints[1] = PointF(tempBitmap.width.toFloat(), 0F)
//        outlinePoints[2] = PointF(0F, tempBitmap.height.toFloat())
//        outlinePoints[3] = PointF(tempBitmap.width.toFloat(), tempBitmap.height.toFloat())
//        return outlinePoints
//    }

    private fun getOutlinePoints(
        tempBitmap: Bitmap,
        orderedPoints: Map<Int?, PointF?>?
    ): Map<Int?, PointF?> {
        val points = getPoints(tempBitmap)
        val outlinePoints: MutableMap<Int?, PointF?> = HashMap()
        Log.e("OUTLINE POINTS", orderedPoints.toString())
        if (orderedPoints != null) {
            if (orderedPoints.size >= 4) {
                for (i in 0..3) {
                    outlinePoints[i] = orderedPoints[i]?.let {
                        orderedPoints[i]?.let { it1 ->
                            PointF(
                                it.x,
                                it1.y
                            )
                        }
                    }
                }
            }
        }

        return outlinePoints
    }

    private fun showErrorDialog() {
        Toast.makeText(this, "Invalid shape", Toast.LENGTH_LONG).show()
    }

    private fun isScanPointsValid(points: Map<Int?, PointF?>): Boolean {
        return points.size == 4
    }

    inner class ScanAsyncTask(private val points: Map<Int?, PointF?>) :
        AsyncTask<Void?, Void?, Bitmap>() {

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(bitmap: Bitmap) {
            super.onPostExecute(bitmap)
            val uri: Uri? = getUri(this@MainActivity, bitmap)
            bitmap.recycle()
            val intent = Intent(this@MainActivity, ResultActivity::class.java)
            intent.putExtra("BITMAP_PATH", uri.toString())
            startActivity(intent)
            // scanner.onScanFinish(uri);
        }

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Void?): Bitmap? {
            return getScannedBitmap(bitmap, points);
        }
    }

    fun getUri(context: Context, bitmap: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path =
            MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "Title", null)
        return Uri.parse(path)
    }

    private fun getScannedBitmap(
        original: Bitmap?,
        points: Map<Int?, PointF?>
    ): Bitmap? {
        try {
            val width = original?.width
            val height = original?.height
            val xRatio = original?.width?.toFloat()?.div(imageView.width)
            val yRatio = original?.height?.toFloat()?.div(imageView.height)
            val x1 = points[0]!!.x * xRatio!!
            val x2 = points[1]!!.x * xRatio
            val x3 = points[2]!!.x * xRatio
            val x4 = points[3]!!.x * xRatio
            val y1 = points[0]!!.y * yRatio!!
            val y2 = points[1]!!.y * yRatio
            val y3 = points[2]!!.y * yRatio
            val y4 = points[3]!!.y * yRatio
            Log.d("", "POints($x1,$y1)($x2,$y2)($x3,$y3)($x4,$y4)")
            return getScannedBitmap(original, x1, y1, x2, y2, x3, y3, x4, y4)
        } catch (e: RuntimeException) {
            Log.e("Runtime Exception", "" + e)
        }
        return null
    }

}