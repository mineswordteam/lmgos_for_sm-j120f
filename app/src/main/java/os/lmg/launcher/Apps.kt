package os.lmg.launcher

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.*

private fun Activity.dp(x: Int) = (x * resources.displayMetrics.density).toInt()

class CalcActivity : Activity() {
    private var cur = "0"; private var acc = 0.0; private var op: Char? = null; private var fresh = true
    private lateinit var disp: TextView
    override fun onCreate(b: Bundle?) {
        super.onCreate(b); window.statusBarColor = Color.BLACK
        val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL; col.setBackgroundColor(Color.BLACK); col.setPadding(dp(14), dp(34), dp(14), dp(14))
        disp = TextView(this); disp.textSize = 48f; disp.setTextColor(Color.WHITE); disp.gravity = Gravity.END or Gravity.BOTTOM; disp.text = "0"; disp.maxLines = 1; disp.layoutDirection = View.LAYOUT_DIRECTION_LTR
        col.addView(disp, LinearLayout.LayoutParams(-1, 0, 1.1f))
        val g = GridLayout(this); g.columnCount = 4; g.rowCount = 5
        listOf("C", "⌫", "%", "÷", "7", "8", "9", "×", "4", "5", "6", "−", "1", "2", "3", "+", "±", "0", ".", "=").forEach { k ->
            g.addView(nbtn(this, k) { press(k) }, GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply { width = 0; height = 0; setMargins(dp(4), dp(4), dp(4), dp(4)) })
        }
        col.addView(g, LinearLayout.LayoutParams(-1, 0, 3f)); setContentView(col)
    }
    private fun fmt(x: Double): String = if (x.isNaN()) "خطا" else if (x == Math.floor(x) && Math.abs(x) < 1e12) x.toLong().toString() else x.toString().take(12)
    private fun calc() {
        val v = cur.toDoubleOrNull() ?: 0.0
        if (op != null && !fresh) { acc = when (op) { '+' -> acc + v; '−' -> acc - v; '×' -> acc * v; else -> if (v == 0.0) Double.NaN else acc / v }; cur = fmt(acc) } else acc = v
    }
    private fun press(k: String) {
        when {
            k[0].isDigit() -> { cur = if (fresh || cur == "0") k else cur + k; fresh = false }
            k == "." -> { if (fresh) { cur = "0."; fresh = false } else if (!cur.contains('.')) cur += "." }
            k == "C" -> { cur = "0"; acc = 0.0; op = null; fresh = true }
            k == "⌫" -> cur = if (cur.length > 1) cur.dropLast(1) else "0"
            k == "±" -> cur = if (cur.startsWith("-")) cur.drop(1) else if (cur != "0") "-$cur" else cur
            k == "%" -> cur = fmt((cur.toDoubleOrNull() ?: 0.0) / 100)
            k == "=" -> { calc(); op = null; fresh = true }
            else -> { calc(); op = k[0]; fresh = true }
        }
        disp.text = cur
    }
}

class NotesActivity : Activity() {
    private lateinit var e: EditText
    override fun onCreate(b: Bundle?) {
        super.onCreate(b); window.statusBarColor = Color.BLACK
        val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL; col.setBackgroundColor(Color.BLACK); col.setPadding(dp(14), dp(34), dp(14), dp(14))
        val t = TextView(this); t.text = "یادداشت"; t.textSize = 22f; t.setTextColor(NEON); t.setPadding(0, 0, 0, dp(10)); col.addView(t)
        e = EditText(this); e.setText(getSharedPreferences("lmg", 0).getString("note", "")); e.hint = "اینجا بنویسید…"; e.setHintTextColor(0xFF3A5A7A.toInt())
        e.setTextColor(Color.WHITE); e.textSize = 16f; e.gravity = Gravity.TOP; e.setPadding(dp(12), dp(12), dp(12), dp(12))
        e.background = GradientDrawable().apply { setColor(0xFF050D18.toInt()); setStroke(2, 0xFF1E5FA8.toInt()); cornerRadius = dp(12).toFloat() }
        col.addView(e, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(col)
    }
    override fun onPause() { super.onPause(); getSharedPreferences("lmg", 0).edit().putString("note", e.text.toString()).apply() }
}

class ClockActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private var on = false; private var t0 = 0L; private var acc = 0L
    private lateinit var sw: TextView
    private val tick = object : Runnable { override fun run() { sw.text = fmt(acc + SystemClock.elapsedRealtime() - t0); h.postDelayed(this, 50) } }
    private fun fmt(ms: Long) = String.format("%02d:%02d.%d", ms / 60000, ms / 1000 % 60, ms / 100 % 10)
    override fun onCreate(b: Bundle?) {
        super.onCreate(b); window.statusBarColor = Color.BLACK
        val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL; col.gravity = Gravity.CENTER_HORIZONTAL; col.setBackgroundColor(Color.BLACK); col.setPadding(dp(14), dp(60), dp(14), dp(14))
        val c = TextClock(this); c.format24Hour = "HH:mm:ss"; c.format12Hour = "HH:mm:ss"; c.textSize = 56f; c.setTextColor(Color.WHITE); c.setShadowLayer(20f, 0f, 0f, 0xFF1E9BFF.toInt()); col.addView(c)
        val d = TextClock(this); d.format24Hour = "EEEE d MMMM"; d.format12Hour = "EEEE d MMMM"; d.textSize = 15f; d.setTextColor(NEON); col.addView(d)
        sw = TextView(this); sw.text = fmt(0); sw.textSize = 44f; sw.setTextColor(NEON); sw.setPadding(0, dp(50), 0, dp(20)); sw.layoutDirection = View.LAYOUT_DIRECTION_LTR; col.addView(sw)
        val row = LinearLayout(this); row.gravity = Gravity.CENTER
        val go = nbtn(this, "شروع") {}
        go.setOnClickListener { if (on) { acc += SystemClock.elapsedRealtime() - t0; h.removeCallbacks(tick); on = false; go.text = "ادامه" } else { t0 = SystemClock.elapsedRealtime(); on = true; h.post(tick); go.text = "توقف" } }
        val rs = nbtn(this, "صفر") { h.removeCallbacks(tick); on = false; acc = 0; sw.text = fmt(0); go.text = "شروع" }
        row.addView(go, LinearLayout.LayoutParams(dp(130), dp(56)).apply { setMargins(dp(8), 0, dp(8), 0) }); row.addView(rs, LinearLayout.LayoutParams(dp(130), dp(56)).apply { setMargins(dp(8), 0, dp(8), 0) })
        col.addView(row); setContentView(col)
    }
    override fun onDestroy() { super.onDestroy(); h.removeCallbacks(tick) }
}
