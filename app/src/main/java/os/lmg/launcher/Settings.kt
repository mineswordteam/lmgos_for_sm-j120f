package os.lmg.launcher

import android.app.Activity
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.view.*
import android.widget.*

class SettingsActivity : Activity() {
    private lateinit var col: LinearLayout
    private fun d(x: Int) = (x * resources.displayMetrics.density).toInt()
    override fun onCreate(b: Bundle?) {
        super.onCreate(b); window.statusBarColor = Color.BLACK
        val sv = ScrollView(this); sv.setBackgroundColor(Color.BLACK)
        col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL; col.setPadding(d(14), d(34), d(14), d(30)); sv.addView(col); setContentView(sv)
    }
    override fun onResume() { super.onResume(); build() }

    private fun sec(t: String) { val v = TextView(this); v.text = t; v.textSize = 13f; v.setTextColor(NEON); v.setPadding(d(4), d(22), 0, d(6)); col.addView(v) }

    private fun row(g: String, t: String, s: String?, right: View?, click: (() -> Unit)?) {
        val r = LinearLayout(this); r.gravity = Gravity.CENTER_VERTICAL; r.setPadding(d(8), d(8), d(8), d(8))
        r.background = GradientDrawable().apply { setColor(0xFF050D18.toInt()); cornerRadius = d(12).toFloat() }
        r.addView(HexView(this, null, g), d(42), d(48))
        val tx = LinearLayout(this); tx.orientation = LinearLayout.VERTICAL; tx.setPadding(d(12), 0, d(12), 0)
        val a = TextView(this); a.text = t; a.textSize = 15f; a.setTextColor(Color.WHITE); tx.addView(a)
        if (s != null) { val c = TextView(this); c.text = s; c.textSize = 11f; c.setTextColor(0xFF5B7A99.toInt()); tx.addView(c) }
        r.addView(tx, LinearLayout.LayoutParams(0, -2, 1f))
        if (right != null) r.addView(right, LinearLayout.LayoutParams(if (right is SeekBar) d(130) else -2, -2))
        if (click != null) r.setOnClickListener { click() }
        col.addView(r, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = d(6) })
    }

    private fun link(g: String, t: String, action: String) = row(g, t, null, null) {
        try { startActivity(Intent(action)) } catch (e: Exception) { Toast.makeText(this, "در دسترس نیست", Toast.LENGTH_SHORT).show() }
    }

    private fun tog(g: String, t: String, on: Boolean, ch: (Boolean) -> Unit) {
        var s = on; val p = TextView(this); p.textSize = 12f; p.setPadding(d(14), d(6), d(14), d(6)); p.gravity = Gravity.CENTER
        fun paint() {
            p.text = if (s) "روشن" else "خاموش"; p.setTextColor(if (s) Color.WHITE else 0xFF5B7A99.toInt())
            p.background = GradientDrawable().apply { setColor(if (s) 0xFF0B4A8F.toInt() else 0xFF0A1626.toInt()); setStroke(2, if (s) NEON else 0xFF1E3550.toInt()); cornerRadius = d(14).toFloat() }
        }
        paint(); row(g, t, null, p) { s = !s; paint(); ch(s) }
    }

    private fun slider(g: String, t: String, max: Int, cur: Int, ch: (Int) -> Unit) {
        val s = SeekBar(this); s.max = max; s.progress = cur
        s.progressTintList = android.content.res.ColorStateList.valueOf(NEON); s.thumbTintList = android.content.res.ColorStateList.valueOf(NEON)
        s.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(b: SeekBar, p: Int, u: Boolean) { if (u) ch(p) }
            override fun onStartTrackingTouch(b: SeekBar) {}
            override fun onStopTrackingTouch(b: SeekBar) {}
        })
        row(g, t, null, s, null)
    }

    private fun build() {
        col.removeAllViews()
        val t = TextView(this); t.text = "تنظیمات LMG"; t.textSize = 24f; t.setTextColor(Color.WHITE); t.setShadowLayer(14f, 0f, 0f, 0xFF1E9BFF.toInt()); col.addView(t)
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        sec("اتصال‌ها")
        tog("WiFi", "وای‌فای", wm.isWifiEnabled) { try { wm.isWifiEnabled = it } catch (e: Exception) { } }
        tog("BT", "بلوتوث", BluetoothAdapter.getDefaultAdapter()?.isEnabled == true) { try { val a = BluetoothAdapter.getDefaultAdapter(); if (it) a.enable() else a.disable() } catch (e: Exception) { } }
        link("WiFi", "تنظیمات وای‌فای", Settings.ACTION_WIFI_SETTINGS); link("BT", "تنظیمات بلوتوث", Settings.ACTION_BLUETOOTH_SETTINGS); link("SIM", "شبکهٔ موبایل", Settings.ACTION_DATA_ROAMING_SETTINGS)
        sec("صدا")
        slider("MUS", "صدای رسانه", am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), am.getStreamVolume(AudioManager.STREAM_MUSIC)) { am.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0) }
        slider("RING", "صدای زنگ", am.getStreamMaxVolume(AudioManager.STREAM_RING), am.getStreamVolume(AudioManager.STREAM_RING)) { try { am.setStreamVolume(AudioManager.STREAM_RING, it, 0) } catch (e: Exception) { } }
        slider("ALM", "آلارم", am.getStreamMaxVolume(AudioManager.STREAM_ALARM), am.getStreamVolume(AudioManager.STREAM_ALARM)) { am.setStreamVolume(AudioManager.STREAM_ALARM, it, 0) }
        link("SND", "تنظیمات کامل صدا", Settings.ACTION_SOUND_SETTINGS)
        sec("نمایشگر")
        if (Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(this)) row("LCD", "روشنایی", "برای تغییر روشنایی اینجا بزنید و اجازه بدهید", null) { startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))) }
        else slider("LCD", "روشنایی", 255, try { Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) } catch (e: Exception) { 128 }) {
            try { Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0); Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, Math.max(10, it)) } catch (e: Exception) { }
        }
        link("LCD", "تنظیمات کامل نمایشگر", Settings.ACTION_DISPLAY_SETTINGS)
        sec("LMG OS")
        val sp = getSharedPreferences("lmg", 0)
        tog("LOCK", "قفل صفحهٔ LMG", sp.getBoolean("lock", true)) { sp.edit().putBoolean("lock", it).apply() }
        link("HOME", "لانچر پیش‌فرض", Settings.ACTION_HOME_SETTINGS)
        sec("سیستم")
        link("BAT", "باتری", Intent.ACTION_POWER_USAGE_SUMMARY); link("MEM", "حافظه", Settings.ACTION_INTERNAL_STORAGE_SETTINGS); link("APP", "برنامه‌ها", Settings.ACTION_APPLICATION_SETTINGS)
        link("LANG", "زبان و ورودی", Settings.ACTION_LOCALE_SETTINGS); link("TIME", "تاریخ و ساعت", Settings.ACTION_DATE_SETTINGS); link("SEC", "امنیت", Settings.ACTION_SECURITY_SETTINGS); link("ALL", "همهٔ تنظیمات سیستم", Settings.ACTION_SETTINGS)
        sec("دربارهٔ تلفن")
        val mi = ActivityManager.MemoryInfo(); (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        val st = StatFs(Environment.getDataDirectory().path)
        row("INFO", Build.MANUFACTURER + " " + Build.MODEL, "LMG OS 0.3 · Android " + Build.VERSION.RELEASE + "\nرم: " + mi.totalMem / 1048576 + " مگابایت · حافظه: " + st.blockCountLong * st.blockSizeLong / 1073741824 + " گیگابایت\nکرنل: " + System.getProperty("os.version"), null, null)
    }
}
