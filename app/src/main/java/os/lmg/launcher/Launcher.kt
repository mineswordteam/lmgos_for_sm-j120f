package os.lmg.launcher

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.*

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
        p.strokeWidth = 3f; p.color = 0xFF38B6FF.toInt(); cv.drawPath(path, p)
        icon?.let { val s = (w * .5f).toInt(); val l = ((w - s) / 2).toInt(); val t = ((h - s) / 2).toInt(); it.setBounds(l, t, l + s, t + s); it.draw(cv) }
        if (glyph != null) { p.style = Paint.Style.FILL; p.color = 0xFFBFE6FF.toInt(); p.textAlign = Paint.Align.CENTER; p.textSize = w * .24f; p.isFakeBoldText = true; cv.drawText(glyph, w / 2, h / 2 + p.textSize / 3, p); p.isFakeBoldText = false }
    }
}

class Launcher : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var home: FrameLayout
    private lateinit var drawer: ScrollView
    private lateinit var grid: GridLayout
    private lateinit var panel: LinearLayout
    private lateinit var gd: GestureDetector
    private var drawerOn = false
    private var panelOn = false
    private fun d(x: Int) = (x * resources.displayMetrics.density).toInt()

    private fun hex(icon: Drawable?, glyph: String?, w: Int, h: Int, act: () -> Unit): HexView {
        val v = HexView(this, icon, glyph)
        v.layoutParams = LinearLayout.LayoutParams(d(w), d(h)).apply { setMargins(d(7), 0, d(7), 0) }
        v.setOnClickListener { x -> x.animate().scaleX(.85f).scaleY(.85f).setDuration(90).withEndAction { x.animate().scaleX(1f).scaleY(1f).setDuration(120).start(); act() }.start() }
        return v
    }

    private fun go(i: Intent) { try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { Toast.makeText(this, "برنامه‌ای پیدا نشد", Toast.LENGTH_SHORT).show() } }

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

        home = FrameLayout(this); root.addView(home, -1, -1)
        val top = LinearLayout(this); top.orientation = LinearLayout.VERTICAL; top.gravity = Gravity.CENTER_HORIZONTAL; top.setPadding(0, d(70), 0, 0)
        val clock = TextClock(this); clock.format24Hour = "HH:mm"; clock.format12Hour = "HH:mm"; clock.textSize = 64f
        clock.setTextColor(Color.WHITE); clock.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL); clock.setShadowLayer(24f, 0f, 0f, 0xFF1E9BFF.toInt())
        val date = TextClock(this); date.format24Hour = "EEEE d MMMM"; date.format12Hour = "EEEE d MMMM"; date.textSize = 14f; date.setTextColor(0xFF38B6FF.toInt())
        top.addView(clock); top.addView(date); home.addView(top, -1, -2)
        val hint = TextView(this); hint.text = "⌃\nبرای منو بکشید بالا"; hint.gravity = Gravity.CENTER; hint.textSize = 11f; hint.setTextColor(0xFF5B7A99.toInt())
        home.addView(hint, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply { bottomMargin = d(128) })
        android.animation.ObjectAnimator.ofFloat(hint, "translationY", 0f, -d(7).toFloat()).apply { duration = 1200; repeatCount = -1; repeatMode = android.animation.ValueAnimator.REVERSE; start() }
        val dock = LinearLayout(this); dock.gravity = Gravity.CENTER
        listOf(Intent(Intent.ACTION_DIAL), Intent(Intent.ACTION_VIEW, Uri.parse("sms:")), Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), Intent(Settings.ACTION_SETTINGS)).forEach { i ->
            dock.addView(hex(packageManager.resolveActivity(i, 0)?.loadIcon(packageManager), null, 62, 70) { go(i) })
        }
        home.addView(dock, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply { bottomMargin = d(34) })

        grid = GridLayout(this); grid.columnCount = 4; grid.setPadding(d(6), d(60), d(6), d(70))
        drawer = ScrollView(this); drawer.setBackgroundColor(0xF2010308.toInt()); drawer.addView(grid); drawer.visibility = View.INVISIBLE
        root.addView(drawer, -1, -1)

        panel = LinearLayout(this); panel.orientation = LinearLayout.VERTICAL; panel.setPadding(d(10), d(50), d(10), d(18)); panel.visibility = View.INVISIBLE
        panel.background = GradientDrawable().apply { setColor(0xF2050E1C.toInt()); val r = d(22).toFloat(); cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r); setStroke(2, 0xFF1E9BFF.toInt()) }
        val tiles = LinearLayout(this); tiles.gravity = Gravity.CENTER
        fun tile(label: String, on: Boolean, act: (View) -> Unit) {
            val v = hex(null, label, 62, 70) {}
            v.alpha = if (on) 1f else .4f
            v.setOnClickListener { act(v) }
            tiles.addView(v)
        }
        tile("WiFi", (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled) { v ->
            try { val w = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager; w.isWifiEnabled = !w.isWifiEnabled; v.alpha = if (w.isWifiEnabled) 1f else .4f } catch (e: Exception) {}
        }
        tile("BT", BluetoothAdapter.getDefaultAdapter()?.isEnabled == true) { v ->
            try { val a = BluetoothAdapter.getDefaultAdapter(); if (a.isEnabled) a.disable() else a.enable(); v.alpha = if (v.alpha < 1f) 1f else .4f } catch (e: Exception) {}
        }
        tile("SET", true) { go(Intent(Settings.ACTION_SETTINGS)) }
        tile("BOOT", true) { closePanel(); showSplash() }
        panel.addView(tiles); root.addView(panel, -1, -2)
        root.post { drawer.translationY = root.height.toFloat(); drawer.visibility = View.VISIBLE; panel.translationY = -panel.height.toFloat(); panel.visibility = View.VISIBLE }

        gd = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 == null || Math.abs(vy) < Math.abs(vx) || Math.abs(vy) < 700) return false
                val up = vy < 0
                when {
                    panelOn -> if (up) closePanel()
                    drawerOn -> if (!up && drawer.scrollY == 0) closeDrawer()
                    up -> openDrawer()
                    e1.y < d(140) -> openPanel()
                }
                return false
            }
        })
        if (b == null) showSplash()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean { gd.onTouchEvent(ev); return super.dispatchTouchEvent(ev) }
    override fun onBackPressed() { if (panelOn) closePanel() else if (drawerOn) closeDrawer() }
    override fun onNewIntent(i: Intent) { super.onNewIntent(i); if (panelOn) closePanel(); if (drawerOn) closeDrawer() }

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
        packageManager.queryIntentActivities(q, 0).sortedBy { it.loadLabel(packageManager).toString().lowercase() }.forEach { r ->
            val cell = LinearLayout(this); cell.orientation = LinearLayout.VERTICAL; cell.gravity = Gravity.CENTER_HORIZONTAL
            val h = hex(r.loadIcon(packageManager), null, 62, 70) {
                closeDrawer(); go(Intent(Intent.ACTION_MAIN).setClassName(r.activityInfo.packageName, r.activityInfo.name))
            }
            cell.addView(h)
            val t = TextView(this); t.text = r.loadLabel(packageManager); t.textSize = 11f; t.setTextColor(0xFFBFE6FF.toInt()); t.maxLines = 1; t.gravity = Gravity.CENTER; t.ellipsize = android.text.TextUtils.TruncateAt.END
            cell.addView(t, LinearLayout.LayoutParams(d(76), -2))
            grid.addView(cell, GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply { width = 0; topMargin = d(14) })
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
    private fun openPanel() { panelOn = true; panel.animate().translationY(0f).setDuration(380).setInterpolator(DecelerateInterpolator(2f)).start() }
    private fun closePanel() { panelOn = false; panel.animate().translationY(-panel.height.toFloat()).setDuration(300).start() }
}
