package os.lmg.launcher

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.*
import java.util.concurrent.Executors

fun thumb(path: String, target: Int): Bitmap? {
    val o = BitmapFactory.Options(); o.inJustDecodeBounds = true; BitmapFactory.decodeFile(path, o)
    var s = 1; while (o.outWidth / (s * 2) >= target && o.outHeight / (s * 2) >= target) s *= 2
    val o2 = BitmapFactory.Options(); o2.inSampleSize = s
    val bm = BitmapFactory.decodeFile(path, o2) ?: return null
    val ori = try { android.media.ExifInterface(path).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1) } catch (e: Exception) { 1 }
    val deg = when (ori) { 6 -> 90f; 3 -> 180f; 8 -> 270f; else -> 0f }
    return if (deg != 0f) Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, Matrix().apply { postRotate(deg) }, true) else bm
}

class GalleryActivity : Activity() {
    private val ex = Executors.newFixedThreadPool(3)
    private lateinit var grid: GridLayout
    private lateinit var viewer: ImageView
    private lateinit var msg: TextView
    private fun d(x: Int) = (x * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b); window.statusBarColor = Color.BLACK
        val root = FrameLayout(this); root.setBackgroundColor(Color.BLACK)
        val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL; col.setPadding(d(4), d(34), d(4), 0)
        val t = TextView(this); t.text = "گالری"; t.textSize = 22f; t.setTextColor(Color.WHITE); t.setShadowLayer(14f, 0f, 0f, 0xFF1E9BFF.toInt()); t.setPadding(d(10), 0, 0, d(10)); col.addView(t)
        msg = TextView(this); msg.setTextColor(0xFF5B7A99.toInt()); msg.gravity = Gravity.CENTER; msg.setPadding(0, d(40), 0, 0); col.addView(msg)
        grid = GridLayout(this); grid.columnCount = 3
        val sv = ScrollView(this); sv.addView(grid); col.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f)); root.addView(col, -1, -1)
        viewer = ImageView(this); viewer.setBackgroundColor(Color.BLACK); viewer.visibility = View.GONE; viewer.setOnClickListener { viewer.visibility = View.GONE }; root.addView(viewer, -1, -1)
        setContentView(root)
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 3) else load()
    }
    override fun onRequestPermissionsResult(rc: Int, p: Array<String>, g: IntArray) { if (g.isNotEmpty() && g[0] == PackageManager.PERMISSION_GRANTED) load() else msg.text = "برای دیدن عکس‌ها دسترسی لازم است" }
    override fun onBackPressed() { if (viewer.visibility == View.VISIBLE) viewer.visibility = View.GONE else finish() }
    override fun onDestroy() { super.onDestroy(); ex.shutdownNow() }

    private fun load() {
        grid.removeAllViews(); val size = (resources.displayMetrics.widthPixels - d(8)) / 3; var n = 0
        try {
            contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media.DATA), null, null, "date_added DESC")?.use { c ->
                while (c.moveToNext() && n < 150) {
                    val path = c.getString(0) ?: continue; n++
                    val iv = ImageView(this); iv.scaleType = ImageView.ScaleType.CENTER_CROP; iv.setBackgroundColor(0xFF06101D.toInt())
                    iv.setOnClickListener { ex.execute { val bm = thumb(path, 1024); runOnUiThread { if (bm != null) { viewer.setImageBitmap(bm); viewer.visibility = View.VISIBLE } } } }
                    grid.addView(iv, GridLayout.LayoutParams().apply { width = size - d(2); height = size - d(2); setMargins(d(1), d(1), d(1), d(1)) })
                    ex.execute { val bm = thumb(path, 200); runOnUiThread { iv.setImageBitmap(bm) } }
                }
            }
        } catch (e: Exception) { }
        msg.text = if (n == 0) "عکسی پیدا نشد" else ""
    }
}
