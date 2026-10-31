package os.lmg.launcher

import android.animation.ValueAnimator
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import java.io.File

class WinBar(c: Context) : View(c) {
    var p = 0f
        set(v) { field = v; invalidate() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var sh = 0f
    init { ValueAnimator.ofFloat(0f, 1f).apply { duration = 1100; repeatCount = -1; addUpdateListener { sh = it.animatedValue as Float; invalidate() }; start() } }
    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val fw = w * p
        paint.style = Paint.Style.FILL; paint.color = 0xFF04101E.toInt(); c.drawRoundRect(0f, 0f, w, h, 6f, 6f, paint)
        if (fw > 2f) {
            paint.shader = LinearGradient(0f, 0f, 0f, h, intArrayOf(0xFF7FD0FF.toInt(), 0xFF1E9BFF.toInt(), 0xFF0B5FC2.toInt(), 0xFF2FB0FF.toInt()), floatArrayOf(0f, .48f, .52f, 1f), Shader.TileMode.CLAMP)
            c.drawRoundRect(0f, 0f, fw, h, 6f, 6f, paint)
            c.save(); c.clipRect(0f, 0f, fw, h)
            val x = (fw + 90f) * sh - 90f
            paint.shader = LinearGradient(x, 0f, x + 90f, 0f, intArrayOf(0, 0x99FFFFFF.toInt(), 0), null, Shader.TileMode.CLAMP)
            c.drawRect(x, 0f, x + 90f, h, paint); c.restore(); paint.shader = null
        }
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f; paint.color = 0xFF2A6CB0.toInt(); c.drawRoundRect(1f, 1f, w - 1, h - 1, 6f, 6f, paint)
    }
}

class Installer : Activity() {
    private val act = "os.lmg.launcher.INSTALL"
    private lateinit var sheet: LinearLayout
    private lateinit var bar: WinBar
    private lateinit var msg: TextView
    private lateinit var row: LinearLayout
    private lateinit var title: TextView
    private lateinit var sub: TextView
    private lateinit var iconBox: FrameLayout
    private lateinit var instBtn: TextView
    private lateinit var f: File
    private var pkg: String? = null
    private fun d(x: Int) = (x * resources.displayMetrics.density).toInt()

    private val rcv = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> i.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                PackageInstaller.STATUS_SUCCESS -> done()
                else -> msg.text = "نصب ناموفق بود: " + i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
            }
        }
    }

    private fun btn(t: String, click: () -> Unit): TextView {
        val b = TextView(this); b.text = t; b.textSize = 15f; b.setTextColor(Color.WHITE); b.gravity = Gravity.CENTER; b.setPadding(d(26), d(10), d(26), d(10))
        b.background = GradientDrawable().apply { setColor(0xFF0B4A8F.toInt()); setStroke(2, 0xFF38B6FF.toInt()); cornerRadius = d(10).toFloat() }
        b.setOnClickListener { click() }; return b
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val root = FrameLayout(this); root.setBackgroundColor(Color.BLACK); root.layoutDirection = View.LAYOUT_DIRECTION_RTL; setContentView(root)
        val head = LinearLayout(this); head.gravity = Gravity.CENTER_VERTICAL; head.setPadding(d(16), d(40), d(16), d(14))
        iconBox = FrameLayout(this); head.addView(iconBox, d(62), d(70))
        val names = LinearLayout(this); names.orientation = LinearLayout.VERTICAL; names.setPadding(d(12), 0, d(12), 0)
        title = TextView(this); title.textSize = 18f; title.setTextColor(Color.WHITE); title.text = "LMG Installer"
        sub = TextView(this); sub.textSize = 11f; sub.setTextColor(0xFF5B7A99.toInt()); sub.text = "یک فایل APK انتخاب کنید"
        names.addView(title); names.addView(sub); head.addView(names, LinearLayout.LayoutParams(0, -2, 1f))
        instBtn = btn("نصب") { install() }; instBtn.visibility = View.GONE; head.addView(instBtn)
        root.addView(head, -1, -2)
        sheet = LinearLayout(this); sheet.orientation = LinearLayout.VERTICAL; sheet.setPadding(d(18), d(16), d(18), d(20)); sheet.setBackgroundColor(0xFF040B16.toInt()); sheet.visibility = View.INVISIBLE
        msg = TextView(this); msg.textSize = 13f; msg.setTextColor(0xFFBFE6FF.toInt()); msg.text = "در حال نصب…"; bar = WinBar(this)
        row = LinearLayout(this); row.gravity = Gravity.CENTER; row.visibility = View.GONE
        row.addView(btn("باز کردن") { pkg?.let { p -> packageManager.getLaunchIntentForPackage(p)?.let { startActivity(it) } } })
        row.addView(Space(this), d(12), 1)
        row.addView(btn("انجام شد") { sheet.animate().translationY(sheet.height.toFloat()).setDuration(300).withEndAction { finish() }.start() })
        sheet.addView(msg); sheet.addView(bar, -1, d(20)); sheet.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = d(14) })
        root.addView(sheet, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        registerReceiver(rcv, IntentFilter(act))
        val u = intent.data
        if (u != null) load(u) else startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE), 7)
    }

    override fun onActivityResult(r: Int, c: Int, data: Intent?) { val u = data?.data; if (u != null) load(u) else if (r == 7) finish() }
    override fun onDestroy() { super.onDestroy(); try { unregisterReceiver(rcv) } catch (e: Exception) {} }

    private fun load(u: Uri) {
        f = File(cacheDir, "x.apk")
        Thread {
            try { contentResolver.openInputStream(u)!!.use { i -> f.outputStream().use { o -> i.copyTo(o) } } } catch (e: Exception) { runOnUiThread { sub.text = "خواندن فایل ناموفق بود" }; return@Thread }
            runOnUiThread {
                val info = packageManager.getPackageArchiveInfo(f.path, 0)
                if (info == null) { sub.text = "این فایل APK معتبر نیست"; return@runOnUiThread }
                info.applicationInfo.sourceDir = f.path; info.applicationInfo.publicSourceDir = f.path
                pkg = info.packageName; title.text = info.applicationInfo.loadLabel(packageManager); sub.text = info.packageName + "  v" + info.versionName
                iconBox.addView(HexView(this, info.applicationInfo.loadIcon(packageManager)), -1, -1); instBtn.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun install() {
        instBtn.isEnabled = false; instBtn.alpha = .4f
        sheet.visibility = View.VISIBLE; sheet.translationY = d(200).toFloat(); sheet.animate().translationY(0f).setDuration(400).start()
        Thread {
            try {
                val pi = packageManager.packageInstaller
                val id = pi.createSession(PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL))
                val s = pi.openSession(id); val len = f.length()
                s.openWrite("apk", 0, len).use { o ->
                    f.inputStream().use { i ->
                        val buf = ByteArray(65536); var t = 0L; var n = i.read(buf)
                        while (n > 0) { o.write(buf, 0, n); t += n; val v = t.toFloat() / len * .9f; runOnUiThread { bar.p = v }; n = i.read(buf) }
                    }
                    s.fsync(o)
                }
                val pe = PendingIntent.getBroadcast(this, id, Intent(act).setPackage(packageName), PendingIntent.FLAG_UPDATE_CURRENT)
                s.commit(pe.intentSender)
            } catch (e: Exception) { runOnUiThread { msg.text = "خطا: " + e.message } }
        }.start()
    }

    private fun done() { bar.p = 1f; msg.text = "نصب انجام شد"; row.alpha = 0f; row.visibility = View.VISIBLE; row.animate().alpha(1f).setDuration(400).start() }
}
