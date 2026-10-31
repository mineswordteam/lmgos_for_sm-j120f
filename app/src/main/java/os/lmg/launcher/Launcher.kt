package os.lmg.launcher

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.*

val NEON = 0xFF38B6FF.toInt()
val HIDDEN = setOf("com.sec.android.app.camera", "com.sec.android.gallery3d", "com.android.settings", "com.android.camera2", "com.android.gallery3d")

fun nbtn(c: Context, t: String, click: () -> Unit): TextView {
    val b = TextView(c); b.text = t; b.textSize = 20f; b.setTextColor(Color.WHITE); b.gravity = Gravity.CENTER
    b.background = GradientDrawable().apply { setColor(0xFF08213D.toInt()); setStroke(2, NEON); cornerRadius = 18f }
    b.setOnClickListener { click() }; return b
}

class HexView(c: Context, private val icon: Drawable?, private val glyph: String? = null) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        path.reset(); path.moveTo(w / 2, 3f); path.lineTo(w - 3, h * .25f); path.lineTo(w - 3, h * .75f)
        path.lineTo(w / 2, h - 3); path.lineTo(3f, h * .75f); path.lineTo(3f, h * .25f); path.close()
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(w / 2, h * .3f, w, 0xFF0B2442.toInt(), 0xFF02060D.toInt(), Shader.TileMode.CLAMP)
        cv.drawPath(path, p); p.shader = null
        p.style = Paint.Style.STROKE; p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = 9f; p.color = 0x401E9BFF; cv.drawPath(path, p)
        p.strokeWidth = 3f; p.color = NEON; cv.drawPath(path, p)
        icon?.let { val s = (w * .5f).toInt(); val l = ((w - s) / 2).toInt(); val t = ((h - s) / 2).toInt(); it.setBounds(l, t, l + s, t + s); it.draw(cv) }
        if (glyph != null) { p.style = Paint.Style.FILL; p.color = 0xFFBFE6FF.toInt(); p.textAlign = Paint.Align.CENTER; p.textSize = w * .22f; p.isFakeBoldText = true; cv.drawText(glyph, w / 2, h / 2 + p.textSize / 3, p); p.isFakeBoldText = false }
    }
}

class Launcher : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var home: FrameLayout
    private lateinit var drawer: ScrollView
    private lateinit var grid: GridLayout
    private lateinit var panel: LinearLayout
    private lateinit var pager: HorizontalScrollView
    private lateinit var dots: LinearLayout
    private lateinit var binfo: TextView
    private lateinit var notePrev: TextView
    private lateinit var gd: GestureDetector
    private var drawerOn = false
    private var panelOn = false
    private var torch = false
    private val sw get() = resources.displayMetrics.widthPixels
    private fun d(x: Int) = (x * resources.displayMetrics.density).toInt()
    private fun prefs() = getSharedPreferences("lmg", 0)

    private fun hex(icon: Drawable?, glyph: String?, w: Int, h: Int, m: Int = 7, act: () -> Unit): HexView {
        val v = HexView(this, icon, glyph)
        v.layoutParams = LinearLayout.LayoutParams(d(w), d(h)).apply { setMargins(d(m), 0, d(m), 0) }
        v.setOnClickListener { x -> x.animate().scaleX(.85f).scaleY(.85f).setDuration(90).withEndAction { x.animate().scaleX(1f).scaleY(1f).setDuration(120).start(); act() }.start() }
        return v
    }

    private fun cell(icon: Drawable?, glyph: String?, label: CharSequence, act: () -> Unit): LinearLayout {
        val c = LinearLayout(this); c.orientation = LinearLayout.VERTICAL; c.gravity = Gravity.CENTER_HORIZONTAL
        c.addView(hex(icon, glyph, 62, 70, 7, act))
        val t = TextView(this); t.text = label; t.textSize = 11f; t.setTextColor(0xFFBFE6FF.toInt()); t.maxLines = 1; t.gravity = Gravity.CENTER
        t.ellipsize = android.text.TextUtils.TruncateAt.END
        c.addView(t, LinearLayout.LayoutParams(d(76), -2)); return c
    }

    private fun gp() = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply { width = 0; topMargin = d(14) }

    private fun ic(i: Intent) = packageManager.resolveActivity(i, 0)?.loadIcon(packageManager)
    private fun rec(i: Intent) {
        val cn = i.component ?: packageManager.resolveActivity(i, 0)?.activityInfo?.let { ComponentName(it.packageName, it.name) } ?: return
        if (cn.className.endsWith("Launcher") || cn.className.endsWith("RecentsActivity")) return
        addRecent(this, cn)
    }
    private fun go(i: Intent) { try { rec(i); startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { Toast.makeText(this, "برنامه‌ای پیدا نشد", Toast.LENGTH_SHORT).show() } }

    private fun page(): FrameLayout { val f = FrameLayout(this); f.layoutParams = LinearLayout.LayoutParams(sw, -1); return f }

    private fun setDot(p: Int) { for (i in 0 until dots.childCount) dots.getChildAt(i).alpha = if (i == p) 1f else .3f }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        root = FrameLayout(this); root.setBackgroundColor(Color.BLACK); root.layoutDirection = View.LAYOUT_DIRECTION_RTL
        setContentView(root)
        val wall = ImageView(this); wall.scaleType = ImageView.ScaleType.CENTER_CROP; wall.setImageResource(R.drawable.wall)
        root.addView(wall, -1, -1)
        val shade = View(this); shade.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x99000000.toInt(), 0, 0, 0xCC000000.toInt()))
        root.addView(shade, -1, -1)
        startService(Intent(this, LockService::class.java))

        home = FrameLayout(this); root.addView(home, -1, -1)
        pager = HorizontalScrollView(this); pager.layoutDirection = View.LAYOUT_DIRECTION_LTR; pager.isHorizontalScrollBarEnabled = false; pager.overScrollMode = View.OVER_SCROLL_NEVER
        val strip = LinearLayout(this); strip.layoutDirection = View.LAYOUT_DIRECTION_LTR; pager.addView(strip, -2, -1)
        home.addView(pager, FrameLayout.LayoutParams(-1, -1).apply { bottomMargin = d(120) })

        val p1 = page()
        val top = LinearLayout(this); top.orientation = LinearLayout.VERTICAL; top.gravity = Gravity.CENTER_HORIZONTAL; top.setPadding(0, d(70), 0, 0)
        val clock = TextClock(this); clock.format24Hour = "HH:mm"; clock.format12Hour = "HH:mm"; clock.textSize = 64f
        clock.setTextColor(Color.WHITE); clock.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL); clock.setShadowLayer(24f, 0f, 0f, 0xFF1E9BFF.toInt())
        val date = TextClock(this); date.format24Hour = "EEEE d MMMM"; date.format12Hour = "EEEE d MMMM"; date.textSize = 14f; date.setTextColor(NEON)
        top.addView(clock); top.addView(date); p1.addView(top, -1, -2)
        val hint = TextView(this); hint.text = "⌃\nبرای منو بکشید بالا"; hint.gravity = Gravity.CENTER; hint.textSize = 11f; hint.setTextColor(0xFF5B7A99.toInt())
        p1.addView(hint, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply { bottomMargin = d(8) })
        android.animation.ObjectAnimator.ofFloat(hint, "translationY", 0f, -d(7).toFloat()).apply { duration = 1200; repeatCount = -1; repeatMode = android.animation.ValueAnimator.REVERSE; start() }

        val p2 = page()
        val fg = GridLayout(this); fg.columnCount = 4; fg.setPadding(d(6), d(90), d(6), 0)
        fun fav(l: String, g: String, i: Intent) = fg.addView(cell(null, g, l) { go(i) }, gp())
        fun sys(l: String, i: Intent) = fg.addView(cell(packageManager.resolveActivity(i, 0)?.loadIcon(packageManager), null, l) { go(i) }, gp())
        fav("ماشین‌حساب", "CALC", Intent(this, CalcActivity::class.java)); fav("یادداشت", "NOTE", Intent(this, NotesActivity::class.java))
        fav("ساعت", "TIME", Intent(this, ClockActivity::class.java)); fav("نصب‌کننده", "APK", Intent(this, Installer::class.java))
        fav("دوربین", "CAM", Intent(this, CameraActivity::class.java)); fav("گالری", "PIC", Intent(this, GalleryActivity::class.java))
        fav("تنظیمات", "SET", Intent(this, SettingsActivity::class.java)); fav("اخیر", "TABS", Intent(this, RecentsActivity::class.java))
        sys("تماس", Intent(Intent.ACTION_DIAL)); sys("پیام", Intent(Intent.ACTION_VIEW, Uri.parse("sms:")))
        p2.addView(fg, -1, -2)

        val p3 = page()
        val c3 = LinearLayout(this); c3.orientation = LinearLayout.VERTICAL; c3.gravity = Gravity.CENTER_HORIZONTAL; c3.setPadding(d(24), d(80), d(24), 0)
        val lg = ImageView(this); lg.setImageResource(R.drawable.logo); c3.addView(lg, d(120), d(120))
        val inf = TextView(this); inf.text = "LMG OS 0.3\nSM-J120F · Android " + Build.VERSION.RELEASE; inf.gravity = Gravity.CENTER; inf.setTextColor(NEON); inf.textSize = 13f; c3.addView(inf)
        notePrev = TextView(this); notePrev.setTextColor(0xFFBFE6FF.toInt()); notePrev.textSize = 13f; notePrev.setPadding(d(14), d(14), d(14), d(14)); notePrev.maxLines = 6
        notePrev.background = GradientDrawable().apply { setColor(0x99061224.toInt()); setStroke(2, 0xFF1E5FA8.toInt()); cornerRadius = d(12).toFloat() }
        c3.addView(notePrev, LinearLayout.LayoutParams(-1, -2).apply { topMargin = d(22) }); p3.addView(c3, -1, -2)
        strip.addView(p1); strip.addView(p2); strip.addView(p3)

        dots = LinearLayout(this); dots.gravity = Gravity.CENTER
        for (i in 0..2) dots.addView(View(this).apply { background = GradientDrawable().apply { setColor(NEON); shape = GradientDrawable.OVAL } }, LinearLayout.LayoutParams(d(7), d(7)).apply { setMargins(d(4), 0, d(4), 0) })
        setDot(0); home.addView(dots, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply { bottomMargin = d(112) })
        pager.setOnTouchListener { v, ev ->
            if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) v.post { val p = (pager.scrollX + sw / 2) / sw; pager.smoothScrollTo(p * sw, 0); setDot(p) }
            false
        }

        val dock = LinearLayout(this); dock.gravity = Gravity.CENTER
        fun dk(i: Drawable?, g: String?, t: Intent) = dock.addView(hex(i, g, 54, 61, 4) { go(t) })
        val dial = Intent(Intent.ACTION_DIAL); val smsI = Intent(Intent.ACTION_VIEW, Uri.parse("sms:"))
        dk(ic(dial), null, dial); dk(ic(smsI), null, smsI); dk(null, "TABS", Intent(this, RecentsActivity::class.java))
        dk(null, "CAM", Intent(this, CameraActivity::class.java)); dk(null, "SET", Intent(this, SettingsActivity::class.java))
        home.addView(dock, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply { bottomMargin = d(34) })

        grid = GridLayout(this); grid.columnCount = 4; grid.setPadding(d(6), d(60), d(6), d(70))
        drawer = ScrollView(this); drawer.setBackgroundColor(0xF2010308.toInt()); drawer.addView(grid); drawer.visibility = View.INVISIBLE
        root.addView(drawer, -1, -1)

        panel = LinearLayout(this); panel.orientation = LinearLayout.VERTICAL; panel.setPadding(d(10), d(46), d(10), d(18)); panel.visibility = View.INVISIBLE
        panel.background = GradientDrawable().apply { setColor(0xF2050E1C.toInt()); val r = d(22).toFloat(); cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r); setStroke(2, 0xFF1E9BFF.toInt()) }
        binfo = TextView(this); binfo.setTextColor(NEON); binfo.textSize = 13f; binfo.gravity = Gravity.CENTER; binfo.setPadding(0, 0, 0, d(10)); panel.addView(binfo)
        val row1 = LinearLayout(this); row1.gravity = Gravity.CENTER
        val row2 = LinearLayout(this); row2.gravity = Gravity.CENTER; row2.setPadding(0, d(10), 0, 0)
        fun tile(row: LinearLayout, label: String, on: Boolean, act: (View) -> Unit) {
            val v = hex(null, label, 62, 70) {}; v.alpha = if (on) 1f else .4f; v.setOnClickListener { act(v) }; row.addView(v)
        }
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        tile(row1, "WiFi", wm.isWifiEnabled) { v -> try { wm.isWifiEnabled = !wm.isWifiEnabled; v.alpha = if (wm.isWifiEnabled) 1f else .4f } catch (e: Exception) { } }
        tile(row1, "BT", BluetoothAdapter.getDefaultAdapter()?.isEnabled == true) { v ->
            try { val a = BluetoothAdapter.getDefaultAdapter(); if (a.isEnabled) a.disable() else a.enable(); v.alpha = if (v.alpha < 1f) 1f else .4f } catch (e: Exception) { }
        }
        tile(row1, "TORCH", false) { v -> toggleTorch(v) }
        tile(row1, "LOCK", prefs().getBoolean("lock", true)) { v -> val n = !prefs().getBoolean("lock", true); prefs().edit().putBoolean("lock", n).apply(); v.alpha = if (n) 1f else .4f }
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        tile(row2, "SOUND", am.ringerMode == AudioManager.RINGER_MODE_NORMAL) { v ->
            val n = am.ringerMode == AudioManager.RINGER_MODE_NORMAL; am.ringerMode = if (n) AudioManager.RINGER_MODE_VIBRATE else AudioManager.RINGER_MODE_NORMAL; v.alpha = if (n) .4f else 1f
        }
        tile(row2, "SET", true) { closePanel(); go(Intent(this, SettingsActivity::class.java)) }
        tile(row2, "TABS", true) { closePanel(); go(Intent(this, RecentsActivity::class.java)) }
        tile(row2, "BOOT", true) { closePanel(); showSplash() }
        panel.addView(row1); panel.addView(row2)
        val vol = SeekBar(this); vol.max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC); vol.progress = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        vol.progressTintList = android.content.res.ColorStateList.valueOf(NEON); vol.thumbTintList = android.content.res.ColorStateList.valueOf(NEON)
        vol.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, u: Boolean) { if (u) am.setStreamVolume(AudioManager.STREAM_MUSIC, p, 0) }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {}
        })
        panel.addView(vol, LinearLayout.LayoutParams(-1, -2).apply { topMargin = d(18) })
        root.addView(panel, -1, -2)
        root.post { drawer.translationY = root.height.toFloat(); drawer.visibility = View.VISIBLE; panel.translationY = -panel.height.toFloat(); panel.visibility = View.VISIBLE }

        gd = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 == null || Math.abs(vy) < Math.abs(vx) || Math.abs(vy) < 600) return false
                val up = vy < 0
                when {
                    panelOn -> if (up) closePanel()
                    drawerOn -> if (!up && drawer.scrollY == 0) closeDrawer()
                    up -> openDrawer()
                    else -> openPanel()
                }
                return false
            }
            override fun onLongPress(e: MotionEvent) { if (!drawerOn && !panelOn) showMenu() }
        })
        if (b == null) showSplash()
    }

    override fun onResume() { super.onResume(); notePrev.text = prefs().getString("note", "")?.ifEmpty { "یادداشتی ندارید. از صفحهٔ دوم «یادداشت» را باز کنید." } }
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean { gd.onTouchEvent(ev); return super.dispatchTouchEvent(ev) }
    override fun onBackPressed() { if (panelOn) closePanel() else if (drawerOn) closeDrawer() else { pager.smoothScrollTo(0, 0); setDot(0) } }
    override fun onNewIntent(i: Intent) { super.onNewIntent(i); if (panelOn) closePanel(); if (drawerOn) closeDrawer(); pager.smoothScrollTo(0, 0); setDot(0) }

    private fun showMenu() {
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert).setItems(arrayOf("تنظیمات تلفن", "پخش انیمیشن بوت", "قفل صفحهٔ LMG: روشن یا خاموش")) { _, w ->
            when (w) { 0 -> go(Intent(Settings.ACTION_SETTINGS)); 1 -> showSplash(); else -> prefs().edit().putBoolean("lock", !prefs().getBoolean("lock", true)).apply() }
        }.show()
    }

    private fun toggleTorch(v: View) {
        if (Build.VERSION.SDK_INT < 23) return
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) { requestPermissions(arrayOf(Manifest.permission.CAMERA), 1); return }
        try { val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager; torch = !torch; cm.setTorchMode(cm.cameraIdList[0], torch); v.alpha = if (torch) 1f else .4f }
        catch (e: Exception) { Toast.makeText(this, "چراغ‌قوه در دسترس نیست", Toast.LENGTH_SHORT).show() }
    }

    private fun showSplash() {
        val s = FrameLayout(this); s.setBackgroundColor(Color.BLACK); s.isClickable = true
        val logo = ImageView(this); logo.setImageResource(R.drawable.logo); logo.alpha = 0f; logo.scaleX = .6f; logo.scaleY = .6f
        s.addView(logo, FrameLayout.LayoutParams(d(190), d(190), Gravity.CENTER)); root.addView(s, -1, -1)
        logo.animate().alpha(1f).scaleX(1.05f).scaleY(1.05f).setDuration(1800).setInterpolator(DecelerateInterpolator()).withEndAction {
            s.animate().alpha(0f).setStartDelay(500).setDuration(600).withEndAction { root.removeView(s) }.start()
        }.start()
    }

    private fun fill() {
        grid.removeAllViews()
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(q, 0).filter { it.activityInfo.packageName !in HIDDEN }.sortedBy { it.loadLabel(packageManager).toString().lowercase() }.forEach { r ->
            grid.addView(cell(r.loadIcon(packageManager), null, r.loadLabel(packageManager)) {
                closeDrawer(); go(Intent(Intent.ACTION_MAIN).setClassName(r.activityInfo.packageName, r.activityInfo.name))
            }, gp())
        }
    }

    private fun openDrawer() {
        if (drawerOn) return; drawerOn = true; fill(); drawer.scrollTo(0, 0)
        drawer.animate().translationY(0f).setDuration(380).setInterpolator(DecelerateInterpolator(2f)).start()
        home.animate().alpha(.25f).scaleX(.94f).scaleY(.94f).setDuration(380).start()
        for (i in 0 until grid.childCount) grid.getChildAt(i).apply { alpha = 0f; translationY = d(40).toFloat(); animate().alpha(1f).translationY(0f).setStartDelay(i * 18L).setDuration(350).start() }
    }
    private fun closeDrawer() {
        drawerOn = false
        drawer.animate().translationY(root.height.toFloat()).setDuration(300).start()
        home.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300).start()
    }
    private fun openPanel() {
        val bi = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val lv = bi?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0; val sc = bi?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        binfo.text = "باتری " + (lv * 100 / sc) + "٪   ·   LMG OS"
        panelOn = true; panel.animate().translationY(0f).setDuration(380).setInterpolator(DecelerateInterpolator(2f)).start()
    }
    private fun closePanel() { panelOn = false; panel.animate().translationY(-panel.height.toFloat()).setDuration(300).start() }
}
