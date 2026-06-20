package {{PACKAGE}}

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONArray
import org.json.JSONObject

/**
 * Device Info Manager - معلومات الجهاز الشاملة
 * يوفر واجهة JavaScript للحصول على معلومات الجهاز
 */
class DeviceInfoManager(private val context: Context) {
    
    private var webView: WebView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var batteryReceiver: BroadcastReceiver? = null
    private var batteryCallback: String? = null
    
    fun setWebView(view: WebView) {
        webView = view
    }
    
    /**
     * الحصول على معلومات الجهاز الكاملة
     */
    @JavascriptInterface
    fun getInfo(): String {
        return JSONObject().apply {
            put("manufacturer", Build.MANUFACTURER)
            put("brand", Build.BRAND)
            put("model", Build.MODEL)
            put("device", Build.DEVICE)
            put("product", Build.PRODUCT)
            put("hardware", Build.HARDWARE)
            put("sdkInt", Build.VERSION.SDK_INT)
            put("release", Build.VERSION.RELEASE)
            put("securityPatch", if (Build.VERSION.SDK_INT >= 23) Build.VERSION.SECURITY_PATCH else "N/A")
            put("bootloader", Build.BOOTLOADER)
            put("fingerprint", Build.FINGERPRINT)
        }.toString()
    }

    /**
     * الحصول على مستوى البطارية
     */
    @JavascriptInterface
    fun getBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
    
    /**
     * التحقق من حالة الشحن
     */
    @JavascriptInterface
    fun isCharging(): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.isCharging
    }
    
    /**
     * الحصول على معلومات البطارية الكاملة
     */
    @JavascriptInterface
    fun getBatteryInfo(): String {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        return JSONObject().apply {
            put("level", bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY))
            put("charging", bm.isCharging)
            put("temperature", (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0)
            put("voltage", (intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000.0)
            put("health", getBatteryHealth(intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, 0) ?: 0))
            put("plugged", getPluggedType(intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0))
        }.toString()
    }
    
    /**
     * الحصول على معلومات الذاكرة
     */
    @JavascriptInterface
    fun getMemoryInfo(): String {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        
        return JSONObject().apply {
            put("availableMB", mi.availMem / 1048576)
            put("totalMB", mi.totalMem / 1048576)
            put("usedMB", (mi.totalMem - mi.availMem) / 1048576)
            put("percentUsed", ((mi.totalMem - mi.availMem) * 100 / mi.totalMem).toInt())
            put("lowMemory", mi.lowMemory)
            put("threshold", mi.threshold / 1048576)
        }.toString()
    }
    
    /**
     * الحصول على معلومات التخزين
     */
    @JavascriptInterface
    fun getStorageInfo(): String {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        
        val totalMB = (totalBlocks * blockSize) / 1048576
        val availableMB = (availableBlocks * blockSize) / 1048576
        val usedMB = totalMB - availableMB
        
        return JSONObject().apply {
            put("totalMB", totalMB)
            put("availableMB", availableMB)
            put("usedMB", usedMB)
            put("percentUsed", (usedMB * 100 / totalMB).toInt())
        }.toString()
    }
    
    /**
     * الحصول على معلومات الشاشة
     */
    @JavascriptInterface
    @Suppress("DEPRECATION")
    fun getScreenInfo(): String {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(dm)
        
        return JSONObject().apply {
            put("widthPixels", dm.widthPixels)
            put("heightPixels", dm.heightPixels)
            put("density", dm.density)
            put("densityDpi", dm.densityDpi)
            put("scaledDensity", dm.scaledDensity)
            put("xdpi", dm.xdpi)
            put("ydpi", dm.ydpi)
        }.toString()
    }
    
    /**
     * الحصول على قائمة المستشعرات
     */
    @JavascriptInterface
    fun getSensors(): String {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = sm.getSensorList(Sensor.TYPE_ALL)
        
        val arr = JSONArray()
        sensors.forEach { sensor ->
            arr.put(JSONObject().apply {
                put("name", sensor.name)
                put("vendor", sensor.vendor)
                put("type", sensor.type)
                put("version", sensor.version)
                put("power", sensor.power)
                put("resolution", sensor.resolution)
            })
        }
        return arr.toString()
    }
    
    /**
     * بدء مراقبة تغييرات البطارية
     */
    @JavascriptInterface
    fun onBatteryChanged(callback: String) {
        stopBatteryMonitor()
        batteryCallback = callback
        
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                val percent = (level * 100 / scale)
                val charging = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
                
                val json = JSONObject().apply {
                    put("level", percent)
                    put("charging", charging)
                }.toString()
                
                    handler.post {
                        webView?.evaluateJavascript("$callback($json)", null)
                    }
                }
            }
            
            context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }
        
        /**
         * إيقاف مراقبة البطارية
         */
        @JavascriptInterface
        fun stopBatteryMonitor() {
            batteryReceiver?.let {
                try { context.unregisterReceiver(it) } catch (e: Exception) {}
            }
            batteryReceiver = null
            batteryCallback = null
        }
        
        private fun getBatteryHealth(health: Int): String {
            return when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "over_voltage"
                BatteryManager.BATTERY_HEALTH_COLD -> "cold"
                else -> "unknown"
            }
        }
        
        private fun getPluggedType(plugged: Int): String {
            return when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "ac"
                BatteryManager.BATTERY_PLUGGED_USB -> "usb"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
                else -> "none"
            }
        }
        
        fun destroy() {
            stopBatteryMonitor()
        }
    }
