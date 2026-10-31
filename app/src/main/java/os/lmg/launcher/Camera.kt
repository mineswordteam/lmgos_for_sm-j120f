package os.lmg.launcher

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.*
import java.io.File

@Suppress("DEPRECATION")
class CameraActivity : Activity(), TextureView.SurfaceTextureListener {
    private var cam: Camera? = null
    private var id = 0
    private var flash = false
    private var asked = false
    private lateinit var tv: TextureView
    private lateinit var fl: View
    private fun d(x: Int) = (x * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b); window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        val root = FrameLayout(this); root.setBackgroundColor(Color.BLACK); setContentView(root)
        tv = TextureView(this); tv.surfaceTextureListener = this; root.addView(tv, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        fl = View(this); fl.setBackgroundColor(Color.WHITE); fl.alpha = 0f; root.addView(fl, -1, -1)
        val bar = LinearLayout(this); bar.gravity = Gravity.CENTER; bar.layoutDirection = View.LAYOUT_DIRECTION_LTR
        fun hx(g: String, w: Int, h: Int, a: (View) -> Unit): HexView {
            val v = HexView(this, null, g); v.layoutParams = LinearLayout.LayoutParams(d(w), d(h)).apply { setMargins(d(14), 0, d(14), 0) }
            v.setOnClickListener { a(it) }; return v
        }
        bar.addView(hx("FLIP", 56, 64) { flip() }); bar.addView(hx("SNAP", 76, 86) { snap() })
        val fb = hx("FLASH", 56, 64) { v -> flash = !flash; v.alpha = if (flash) 1f else .45f; applyFlash() }; fb.alpha = .45f; bar.addView(fb)
        root.addView(bar, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply { bottomMargin = d(26) })
    }

    private fun ok() = Build.VERSION.SDK_INT < 23 || (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)

    override fun onResume() {
        super.onResume()
        if (!ok()) { if (!asked && Build.VERSION.SDK_INT >= 23) { asked = true; requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), 2) } }
        else if (tv.isAvailable) open()
    }
    override fun onRequestPermissionsResult(rc: Int, p: Array<String>, g: IntArray) { if (ok() && tv.isAvailable) open() }
    override fun onPause() { super.onPause(); close() }
    override fun onSurfaceTextureAvailable(s: SurfaceTexture, w: Int, h: Int) { if (ok()) open() }
    override fun onSurfaceTextureSizeChanged(s: SurfaceTexture, w: Int, h: Int) {}
    override fun onSurfaceTextureDestroyed(s: SurfaceTexture): Boolean = true
    override fun onSurfaceTextureUpdated(s: SurfaceTexture) {}

    private fun close() { try { cam?.stopPreview(); cam?.release() } catch (e: Exception) { }; cam = null }

    private fun applyFlash() { try { val c = cam ?: return; val p = c.parameters; if (p.supportedFlashModes != null) { p.flashMode = if (flash) Camera.Parameters.FLASH_MODE_ON else Camera.Parameters.FLASH_MODE_OFF; c.parameters = p } } catch (e: Exception) { } }

    private fun open() {
        close()
        try {
            val c = Camera.open(id); cam = c; val p = c.parameters
            val ps = p.supportedPreviewSizes.filter { it.width <= 1280 && Math.abs(it.width.toFloat() / it.height - 4f / 3) < .05 }.maxByOrNull { it.width * it.height } ?: p.supportedPreviewSizes[0]
            p.setPreviewSize(ps.width, ps.height)
            val pc = p.supportedPictureSizes.filter { it.width <= 2600 && Math.abs(it.width.toFloat() / it.height - ps.width.toFloat() / ps.height) < .05 }.maxByOrNull { it.width * it.height }
            if (pc != null) p.setPictureSize(pc.width, pc.height)
            if (p.supportedFocusModes?.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) == true) p.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            if (p.supportedFlashModes != null) p.flashMode = if (flash) Camera.Parameters.FLASH_MODE_ON else Camera.Parameters.FLASH_MODE_OFF
            val info = Camera.CameraInfo(); Camera.getCameraInfo(id, info)
            c.setDisplayOrientation(if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) (360 - info.orientation) % 360 else info.orientation)
            p.setRotation(info.orientation); c.parameters = p
            val sw = resources.displayMetrics.widthPixels
            tv.layoutParams = FrameLayout.LayoutParams(sw, sw * ps.width / ps.height, Gravity.CENTER)
            c.setPreviewTexture(tv.surfaceTexture); c.startPreview()
        } catch (e: Exception) { Toast.makeText(this, "دوربین باز نشد", Toast.LENGTH_SHORT).show() }
    }

    private fun flip() { val n = Camera.getNumberOfCameras(); if (n > 1) { id = (id + 1) % n; open() } }

    private fun snap() {
        val c = cam ?: return
        fl.alpha = .8f; fl.animate().alpha(0f).setDuration(250).start()
        try { c.takePicture(null, null, Camera.PictureCallback { data, _ -> save(data); try { c.startPreview() } catch (e: Exception) { } }) } catch (e: Exception) { }
    }

    private fun save(data: ByteArray) {
        Thread {
            try {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "LMG"); dir.mkdirs()
                val f = File(dir, "IMG_" + System.currentTimeMillis() + ".jpg"); f.writeBytes(data)
                MediaScannerConnection.scanFile(this, arrayOf(f.path), null, null)
                runOnUiThread { Toast.makeText(this, "عکس ذخیره شد", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) { runOnUiThread { Toast.makeText(this, "ذخیره نشد", Toast.LENGTH_SHORT).show() } }
        }.start()
    }
}
