package cl.profe.mixaomitv

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    render()
}
    private val defaults = listOf("YouTube", "IPTV Stream Player", "Zapping", "Netflix", "Prime Video", "HBO Max", "Play Store")
    private val prefs by lazy { getSharedPreferences("cyberpunk_launcher", MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private val cyan = 0xFF00F5FF.toInt()
    private val pink = 0xFFFF3CAC.toInt()
    private val muted = 0xFF9BAFC7.toInt()
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private fun dp(n: Int) = (n * resources.displayMetrics.density + .5f).toInt()
    private fun bg(fill: Int, radius: Int, border: Int = Color.TRANSPARENT, thickness: Int = 0) = GradientDrawable().apply {
        setColor(fill); cornerRadius = dp(radius).toFloat()
        if (thickness > 0) setStroke(dp(thickness), border)
    }
    private fun text(value: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false) = TextView(this).apply {
        this.text = value; textSize = size; setTextColor(color); includeFontPadding = false
        typeface = Typeface.create(if (bold) "sans-serif-condensed" else "sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        gravity = Gravity.CENTER_VERTICAL
    }
    private fun items(): MutableList<String> {
        val stored = prefs.getString("order", null)
        return if (stored == null) defaults.toMutableList() else stored.split("\u001f").filter { it.isNotEmpty() }.toMutableList()
    }
    private fun save(items: List<String>) { prefs.edit().putString("order", items.joinToString("\u001f")).apply() }
    private fun installed(): List<Pair<String, ComponentName>> {
        val pm = packageManager
        val map = linkedMapOf<String, ComponentName>()
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            @Suppress("DEPRECATION")
            for (info in pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0)) {
                if (info.activityInfo.packageName != packageName) {
                    val cn = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
                    map[cn.flattenToString()] = cn
                }
            }
        }
        return map.values.map { cn ->
            val label = try { pm.getActivityInfo(cn, 0).loadLabel(pm).toString() } catch (_: Exception) { cn.packageName }
            label to cn
        }.sortedBy { it.first.lowercase(Locale.ROOT) }
    }
    private fun target(name: String, all: List<Pair<String, ComponentName>>): ComponentName? {
        prefs.getString("target_$name", null)?.let { ComponentName.unflattenFromString(it)?.let { cn -> return cn } }
        val packages = when (name) {
            "YouTube" -> listOf("com.google.android.youtube.tv", "com.google.android.youtube")
            "Netflix" -> listOf("com.netflix.ninja", "com.netflix.mediaclient")
            "Prime Video" -> listOf("com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient")
            "HBO Max" -> listOf("com.wbd.stream", "com.hbo.hbonow")
            "Play Store" -> listOf("com.android.vending")
            else -> emptyList()
        }
        all.firstOrNull { it.second.packageName in packages }?.let { return it.second }
        val tokens = when (name) {
            "IPTV Stream Player" -> listOf("iptv", "stream player")
            "Prime Video" -> listOf("prime video", "amazon prime")
            "HBO Max" -> listOf("hbo", "max")
            "Play Store" -> listOf("play store", "google play")
            else -> listOf(name.lowercase(Locale.ROOT))
        }
        return all.firstOrNull { (label, _) -> tokens.any { label.lowercase(Locale.ROOT).contains(it) } }?.second
    }
    private fun open(name: String) {
        val cn = target(name, installed()) ?: run { assign(name); return }
        try {
            val intent = packageManager.getLaunchIntentForPackage(cn.packageName)
                ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER).setComponent(cn)
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { assign(name) }
    }
    private fun assign(name: String) {
        val all = installed()
        if (all.isEmpty()) { Toast.makeText(this, "No se encontraron aplicaciones", Toast.LENGTH_LONG).show(); return }
        AlertDialog.Builder(this).setTitle("VINCULAR · $name")
            .setItems(all.map { it.first }.toTypedArray()) { _, index ->
                prefs.edit().putString("target_$name", all[index].second.flattenToString()).apply(); render()
            }.setNegativeButton("Cancelar", null).show()
    }
    private fun addApp() {
        val all = installed()
        val existing = items().mapNotNull { target(it, all)?.flattenToString() }.toSet()
        val choices = all.filter { it.second.flattenToString() !in existing }
        if (choices.isEmpty()) { Toast.makeText(this, "No hay más aplicaciones para agregar", Toast.LENGTH_LONG).show(); return }
        AlertDialog.Builder(this).setTitle("+ AGREGAR APLICACIÓN")
            .setItems(choices.map { it.first }.toTypedArray()) { _, index ->
                val (label, cn) = choices[index]
                val names = items()
                var name = label
                var suffix = 2
                while (name in names) { name = "$label $suffix"; suffix++ }
                names.add(name); save(names)
                prefs.edit().putString("target_$name", cn.flattenToString()).apply()
                render()
            }.setNegativeButton("Cancelar", null).show()
    }
    private fun manage(name: String) {
        AlertDialog.Builder(this).setTitle("GESTIONAR · $name")
            .setItems(arrayOf("Abrir aplicación", "Cambiar aplicación vinculada", "Quitar de MiXiaomiTV")) { _, index ->
                when (index) {
                    0 -> open(name)
                    1 -> assign(name)
                    2 -> AlertDialog.Builder(this).setTitle("¿Quitar $name?")
                        .setMessage("Solo se eliminará el acceso del launcher. La aplicación seguirá instalada.")
                        .setPositiveButton("Quitar") { _, _ ->
                            val names = items(); names.remove(name); save(names)
                            prefs.edit().remove("target_$name").apply(); render()
                        }.setNegativeButton("Cancelar", null).show()
                }
            }.setNegativeButton("Cancelar", null).show()
    }
    private fun focusable(view: View, normal: GradientDrawable, selected: GradientDrawable) {
        view.isFocusable = true
        view.background = normal
        view.setOnFocusChangeListener { v, focus ->
            v.background = if (focus) selected else normal
            v.animate().scaleX(if (focus) 1.045f else 1f).scaleY(if (focus) 1.045f else 1f).setDuration(140).start()
            v.elevation = dp(if (focus) 15 else 2).toFloat()
        }
    }
    private fun render() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(48), dp(25), dp(48), dp(20))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF15102D.toInt(), 0xFF080C1D.toInt(), 0xFF080917.toInt()))
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(text("◈  MiXiaomiTV", 27f, cyan, true))
        brand.addView(text("N E O N   D I S T R I C T   /   H O M E", 10f, muted), LinearLayout.LayoutParams(-1, dp(25)))
        top.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        val timebox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.RIGHT }
        clock = text("", 29f, Color.WHITE, true).apply { gravity = Gravity.RIGHT }
        date = text("", 11f, muted).apply { gravity = Gravity.RIGHT }
        timebox.addView(clock); timebox.addView(date)
        top.addView(timebox)
        root.addView(top)
        root.addView(View(this).apply { background = bg(cyan, 1) }, LinearLayout.LayoutParams(-1, dp(2)).apply { topMargin = dp(10) })
        val hero = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        hero.addView(text("TU MUNDO. TUS REGLAS.", 30f, Color.WHITE, true))
        hero.addView(text("SISTEMA ONLINE    //    ELIGE TU DESTINO", 12f, pink, true), LinearLayout.LayoutParams(-1, dp(32)))
        root.addView(hero, LinearLayout.LayoutParams(-1, 0, .68f))
        val titleRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        titleRow.addView(text("▰  ACCESOS DIRECTOS", 13f, cyan, true), LinearLayout.LayoutParams(0, dp(33), 1f))
        titleRow.addView(text("MANTÉN OK PARA EDITAR", 10f, muted))
        root.addView(titleRow)
        val scroller = ScrollView(this).apply { isFillViewport = true; isFocusable = false }
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val apps = items()
        val all = installed()
        val tiles = apps.map { it to false } + listOf("+ AGREGAR APP" to true)
        var first: View? = null
        for (rowStart in tiles.indices step 4) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (col in 0..3) {
                val idx = rowStart + col
                if (idx >= tiles.size) { row.addView(View(this), LinearLayout.LayoutParams(0, dp(116), 1f)); continue }
                val (name, add) = tiles[idx]
                val tile = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                    setPadding(dp(7), dp(10), dp(7), dp(6))
                }
                val normal = bg(0xFF141B34.toInt(), 14, if (add) pink else 0xFF34405F.toInt(), 1)
                val selected = bg(0xFF24344C.toInt(), 14, if (add) pink else cyan, 3)
                focusable(tile, normal, selected)
                if (add) {
                    tile.addView(text("+", 42f, pink, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, dp(57)))
                } else {
                    val cn = target(name, all)
                    val drawable = try { if (cn != null) packageManager.getActivityIcon(cn) else null } catch (_: Exception) { null }
                    if (drawable != null) {
                        tile.addView(ImageView(this).apply { setImageDrawable(drawable); scaleType = ImageView.ScaleType.FIT_CENTER }, LinearLayout.LayoutParams(dp(57), dp(57)))
                    } else {
                        tile.addView(text(name.take(1).uppercase(Locale.ROOT), 32f, cyan, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, dp(57)))
                    }
                }
                tile.addView(text(name, if (name.length > 16) 12f else 14f, Color.WHITE, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, dp(32)))
                tile.setOnClickListener { if (add) addApp() else open(name) }
                if (!add) {
                    tile.setOnLongClickListener { manage(name); true }
                    tile.setOnKeyListener { _, key, event ->
                        if ((key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) && event.action == KeyEvent.ACTION_DOWN && event.repeatCount >= 8) {
                            manage(name); true
                        } else false
                    }
                }
                row.addView(tile, LinearLayout.LayoutParams(0, dp(116), 1f).apply { setMargins(dp(6), dp(6), dp(6), dp(6)) })
                if (first == null) first = tile
            }
            grid.addView(row)
        }
        scroller.addView(grid)
        root.addView(scroller, LinearLayout.LayoutParams(-1, 0, 2.65f))
        val bottom = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        bottom.addView(text("● ONLINE    /    OK ABRIR    /    OK LARGO EDITAR", 11f, muted), LinearLayout.LayoutParams(0, -1, 1f))
        val settings = text("⚙  AJUSTES TV", 12f, Color.WHITE, true).apply { gravity = Gravity.CENTER }
        focusable(settings, bg(0xFF24172F.toInt(), 10, pink, 1), bg(0xFF543054.toInt(), 10, pink, 3))
        settings.setOnClickListener { openSettings() }
        bottom.addView(settings, LinearLayout.LayoutParams(dp(150), dp(43)))
        root.addView(bottom, LinearLayout.LayoutParams(-1, dp(58)))
        setContentView(root)
        first?.post { first?.requestFocus() }
        updateClock()
    }
    private fun updateClock() {
        if (!::clock.isInitialized) return
        val now = Date()
        clock.text = SimpleDateFormat("HH:mm", Locale("es", "CL")).format(now)
        date.text = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "CL")).format(now)
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ updateClock() }, 30000)
    }
    private fun openSettings() {
        try { startActivity(Intent(Settings.ACTION_SETTINGS)) }
        catch (_: Exception) { Toast.makeText(this, "Abre los ajustes desde el control", Toast.LENGTH_LONG).show() }
    }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy() }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) { openSettings(); return true }
        return super.onKeyDown(keyCode, event)
    }
}
