package os.lmg.launcher

import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.IBinder
import android.view.*
import android.widget.*

class LockActivity : Activity() {
    private lateinit var gd: GestureDetector
    private lateinit var root: FrameLayout
    private fun d(x: Int) = (x * resources.displayMetrics.density).toInt()
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.statusBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        root = FrameLayout(this); root.setBackgroundColor(Color.BLACK); root.layoutDirection = View.LAYOUT_DIRECTION_RTL; setContentView(root)
        val wall = ImageView(this); wall.scaleType = ImageView.ScaleType.CENTER_CROP; wall.setImageResource(R.drawable.wall); root.addView(wall, -1, -1)
        val shade = View(this); shade.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x88000000.toInt(), 0, 0, 0xDD000000.toInt())); root.addView(shade, -1, -1)
        val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL; col.gravity = Gravity.CENTER_HORIZONTAL; col.setPadding(0, d(90), 0, 0)
        val lg = ImageView(this); lg.setImageResource(R.drawable.logo); col.addView(lg, d(64), d(64))
        val c = TextClock(this); c.format24Hour = "HH:mm"; c.format12Hour = "HH:mm"; c.textSize = 80f; c.setTextColor(Color.WHITE)
        c.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL); c.setShadowLayer(26f, 0f, 0f, 0xFF1E9BFF.toInt()); col.addView(c)
        val dt = TextClock(this); dt.format24Hour = "EEEE d MMMM"; dt.format12Hour = "EEEE d MMMM"; dt.textSize = 15f; dt.setTextColor(NEON); col.addView(dt)
        root.addView(col, -1, -2)
        val hint = TextView(this); hint.text = "⌃\nبرای باز کردن قفل بکشید بالا"; hint.gravity = Gravity.CENTER; hint.textSize = 12f; hint.setTextColor(0xFF8FB8DD.toInt())
        root.addView(hint, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply { bottomMargin = d(50) })
        android.animation.ObjectAnimator.ofFloat(hint, "translationY", 0f, -d(8).toFloat()).apply { duration = 1100; repeatCount = -1; repeatMode = android.animation.ValueAnimator.REVERSE; start() }
        gd = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean { if (vy < -600 && Math.abs(vy) > Math.abs(vx)) unlock(); return true }
        })
    }
    private fun unlock() {
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        root.animate().translationY(-root.height.toFloat()).alpha(0f).setDuration(280).withEndAction { finish(); overridePendingTransition(0, 0) }.start()
    }
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean { gd.onTouchEvent(ev); return super.dispatchTouchEvent(ev) }
    override fun onBackPressed() {}
}

class LockService : Service() {
    private val r = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (c.getSharedPreferences("lmg", 0).getBoolean("lock", true)) c.startActivity(Intent(c, LockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION))
        }
    }
    override fun onCreate() { super.onCreate(); registerReceiver(r, IntentFilter(Intent.ACTION_SCREEN_OFF)) }
    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY
    override fun onBind(i: Intent?): IBinder? = null
    override fun onDestroy() { super.onDestroy(); unregisterReceiver(r) }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) { c.startService(Intent(c, LockService::class.java)) }
}
