package {{PACKAGE}}

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.Locale

/**
 * Location Tracker - تتبع الموقع المتقدم
 * يوفر واجهة JavaScript للحصول على الموقع وتتبعه
 */
class LocationTracker(private val context: Context) {
    
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var trackingListener: LocationListener? = null
    private var trackingCallback: String? = null
    
    fun setWebView(view: WebView) {
        webView = view
    }
    
    /**
     * الحصول على الموقع الحالي
     */
    @JavascriptInterface
    fun getCurrentPosition(callback: String) {
        if (!hasPermission()) {
            callJs(callback, null, "Location permission not granted")
            return
        }
        
        scope.launch {
            try {
                val location = withContext(Dispatchers.IO) {
                    getLastKnownLocation()
                }
                if (location != null) {
                    callJs(callback, locationToJson(location), null)
                } else {
                    callJs(callback, null, "Unable to get location")
                }
            } catch (e: Exception) {
                callJs(callback, null, e.message)
            }
        }
    }

    /**
     * بدء تتبع الموقع المستمر
     */
    @JavascriptInterface
    fun startTracking(intervalMs: Long, callback: String) {
        if (!hasPermission()) {
            callJs(callback, null, "Location permission not granted")
            return
        }
        
        stopTracking()
        trackingCallback = callback
        
        trackingListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                callJs(callback, locationToJson(location), null)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        
        try {
            val provider = getBestProvider()
            locationManager.requestLocationUpdates(
                provider,
                intervalMs,
                0f,
                trackingListener!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            callJs(callback, null, "Security exception: ${e.message}")
        }
    }
    
    /**
     * إيقاف تتبع الموقع
     */
    @JavascriptInterface
    fun stopTracking() {
        trackingListener?.let {
            try {
                locationManager.removeUpdates(it)
            } catch (e: Exception) {}
        }
        trackingListener = null
        trackingCallback = null
    }
    
    /**
     * حساب المسافة بين نقطتين (بالمتر)
     */
    @JavascriptInterface
    fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
    
    /**
     * تحويل إحداثيات إلى عنوان
     */
    @JavascriptInterface
    fun getAddress(lat: Double, lon: Double, callback: String) {
        scope.launch {
            try {
                val address = withContext(Dispatchers.IO) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        JSONObject().apply {
                            put("address", addr.getAddressLine(0) ?: "")
                            put("city", addr.locality ?: "")
                            put("country", addr.countryName ?: "")
                            put("postalCode", addr.postalCode ?: "")
                        }.toString()
                    } else null
                }
                callJs(callback, address, if (address == null) "Address not found" else null)
            } catch (e: Exception) {
                callJs(callback, null, e.message)
            }
        }
    }
    
    /**
     * التحقق من تفعيل GPS
     */
    @JavascriptInterface
    fun isGpsEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
    
    /**
     * التحقق من تفعيل الشبكة
     */
    @JavascriptInterface
    fun isNetworkEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    
    /**
     * الحصول على قائمة المزودين المتاحين
     */
    @JavascriptInterface
    fun getProviders(): String {
        return locationManager.getProviders(true).joinToString(",")
    }
    
    private fun hasPermission(): Boolean {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == 
               PackageManager.PERMISSION_GRANTED
    }
    
    private fun getBestProvider(): String {
        return when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> LocationManager.PASSIVE_PROVIDER
        }
    }
    
    private fun getLastKnownLocation(): Location? {
        return try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: SecurityException) {
            null
        }
    }
    
    private fun locationToJson(location: Location): String {
        return JSONObject().apply {
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("altitude", location.altitude)
            put("accuracy", location.accuracy)
            put("speed", location.speed)
            put("bearing", location.bearing)
            put("time", location.time)
            put("provider", location.provider)
        }.toString()
    }
    
    private fun callJs(callback: String, result: String?, error: String?) {
        val jsResult = result ?: "null"
        val jsError = if (error != null) "\"$error\"" else "null"
        handler.post {
            webView?.evaluateJavascript("$callback($jsResult, $jsError)", null)
        }
    }
    
    fun destroy() {
        stopTracking()
        scope.cancel()
    }
}
