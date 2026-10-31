package os.lmg.launcher

import android.app.Activity
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.*

fun addRecent(c: Context, cn: ComponentName) {
    val p = c.getSharedPreferences("lmg", 0)
    val l = (p.getString("recent", "") ?: "").split(",").filter { it.isNotEmpty() && it != cn.flattenToString() }.toMutableList()
    l.add(0, cn.flattenToString()); p.edit().putString("recent", l.take(12).joinToString(",")).apply()
}

class RecentsActivity : Activity() {
    private class Rec(val label: CharSequence, val icon: Drawable?, val intent: Intent, val key: String, val pkg: String)
    private lateinit var list: LinearLayout
    private lateinit var empty: TextView
    private lateinit var access: TextView
    private fun d(x: Int) = (x * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b); window.statusBarColor = Color.BLACK
        val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL; col.setBackgroundColor(0xFF010308.toInt()); col.setPadding(d(14), d(34), d(14), d(14))
        val t = TextView(this); t.text = "برنامه‌های اخیر"; t.textSize = 22f; t.setTextColor(Color.WHITE); t.setShadowLayer(14f, 0f, 0f, 0xFF1E9BFF.toInt()); t.setPadding(0, 0, 0, d(10)); col.addView(t)
        list = LinearLayout(this); list.orientation = LinearLayout.VERTICAL
        val sv = ScrollView(this); sv.addView(list); col.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))
        empty = TextView(this); empty.text = "برنامه‌ای باز نیست"; empty.setTextColor(0xFF5B7A99.toInt()); empty.gravity = Gravity.CENTER; empty.setPadding(0, d(40), 0, d(40)); col.addView(empty)
        access = TextView(this); access.text = "برای دیدن همهٔ برنامه‌های اخیر، دسترسی را فعال کنید"; access.setTextColor(NEON); access.textSize = 12f; access.gravity = Gravity.CENTER; access.setPadding(0, d(8), 0, d(8))
        access.setOnClickListener { try { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) } catch (e: Exception) { } }
        col.addView(access)
        col.addView(nbtn(this, "بستن همه") { closeAll() }, LinearLayout.LayoutParams(-1, d(52)))
        setContentView(col)
    }

    override fun onResume() { super.onResume(); load() }

    private fun hasUsage(): Boolean = try {
        val ao = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        ao.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) { false }

    private fun recFor(cn: ComponentName): Rec? = try {
        val ai = packageManager.getActivityInfo(cn, 0)
        Rec(ai.loadLabel(packageManager), ai.loadIcon(packageManager), Intent(Intent.ACTION_MAIN).setComponent(cn).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), cn.flattenToString(), cn.packageName)
    } catch (e: Exception) { null }

    private fun load() {
        list.removeAllViews()
        val items = ArrayList<Rec>()
        (getSharedPreferences("lmg", 0).getString("recent", "") ?: "").split(",").filter { it.isNotEmpty() }.forEach { s -> ComponentName.unflattenFromString(s)?.let { cn -> recFor(cn)?.let { items.add(it) } } }
        val usage = hasUsage(); access.visibility = if (usage) View.GONE else View.VISIBLE
        if (usage) try {
            val um = getSystemService("usagestats") as UsageStatsManager
            val now = System.currentTimeMillis()
            um.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 6 * 3600000L, now)?.filter { it.lastTimeUsed > now - 6 * 3600000L && it.packageName != packageName }?.sortedByDescending { it.lastTimeUsed }?.forEach { u ->
                if (items.none { it.pkg == u.packageName } && !(u.packageName in HIDDEN && false)) {
                    val li = packageManager.getLaunchIntentForPackage(u.packageName)
                    if (li != null) li.component?.let { cn -> recFor(cn)?.let { items.add(it) } }
                }
            }
        } catch (e: Exception) { }
        items.take(15).forEach { add(it) }
        empty.visibility = if (list.childCount == 0) View.VISIBLE else View.GONE
    }

    private fun kill(r: Rec) {
        val p = getSharedPreferences("lmg", 0)
        p.edit().putString("recent", (p.getString("recent", "") ?: "").split(",").filter { it.isNotEmpty() && it != r.key }.joinToString(",")).apply()
        if (r.pkg != packageName) try { (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).killBackgroundProcesses(r.pkg) } catch (e: Exception) { }
    }

    private fun dismiss(v: View, r: Rec, dir: Float) {
        v.animate().translationX(dir * v.width).alpha(0f).setDuration(200).withEndAction {
            list.removeView(v); kill(r); empty.visibility = if (list.childCount == 0) View.VISIBLE else View.GONE
        }.start()
    }

    private fun closeAll() {
        for (i in 0 until list.childCount) { val v = list.getChildAt(i); v.animate().translationX(v.width.toFloat()).alpha(0f).setStartDelay(i * 50L).setDuration(220).start() }
        list.postDelayed({
            val p = getSharedPreferences("lmg", 0); val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            (p.getString("recent", "") ?: "").split(",").filter { it.isNotEmpty() }.forEach { s -> ComponentName.unflattenFromString(s)?.let { if (it.packageName != packageName) try { am.killBackgroundProcesses(it.packageName) } catch (e: Exception) { } } }
            p.edit().putString("recent", "").apply(); list.removeAllViews(); empty.visibility = View.VISIBLE
        }, 400)
    }

    private fun add(r: Rec) {
        val card = LinearLayout(this); card.gravity = Gravity.CENTER_VERTICAL; card.setPadding(d(10), d(8), d(10), d(8))
        card.background = GradientDrawable().apply { setColor(0xFF061224.toInt()); setStroke(2, 0xFF1E5FA8.toInt()); cornerRadius = d(14).toFloat() }
        card.addView(HexView(this, r.icon), d(54), d(61))
        val tx = TextView(this); tx.text = r.label; tx.textSize = 16f; tx.setTextColor(Color.WHITE); tx.setPadding(d(12), 0, d(12), 0); card.addView(tx, LinearLayout.LayoutParams(0, -2, 1f))
        val x = TextView(this); x.text = "✕"; x.textSize = 22f; x.setTextColor(NEON); x.setPadding(d(12), d(6), d(12), d(6)); x.setOnClickListener { dismiss(card, r, 1f) }; card.addView(x)
        card.setOnTouchListener(object : View.OnTouchListener {
            var x0 = 0f; var moved = false
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { x0 = e.rawX; moved = false }
                    MotionEvent.ACTION_MOVE -> { val dx = e.rawX - x0; if (Math.abs(dx) > d(12)) { moved = true; v.parent.requestDisallowInterceptTouchEvent(true) }; if (moved) { v.translationX = dx; v.alpha = Math.max(.2f, 1 - Math.abs(dx) / v.width) } }
                    MotionEvent.ACTION_UP -> { val dx = e.rawX - x0; if (moved) { if (Math.abs(dx) > v.width / 3) dismiss(v, r, if (dx > 0) 1f else -1f) else v.animate().translationX(0f).alpha(1f).setDuration(150).start() } else { startActivity(r.intent); finish() } }
                    MotionEvent.ACTION_CANCEL -> v.animate().translationX(0f).alpha(1f).setDuration(150).start()
                }
                return true
            }
        })
        list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = d(8) })
    }
}
