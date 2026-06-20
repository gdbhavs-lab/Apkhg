package {{PACKAGE}}

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONObject

/**
 * Network Monitor - مراقب الشبكة
 * يوفر مراقبة حالة الاتصال في الوقت الحقيقي
 */
class NetworkMonitor(private val context: Context) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var webView: WebView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var jsCallback: String? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    fun setWebView(view: WebView) {
        webView = view
    }
    
    /**
     * الحصول على حالة الاتصال الحالية
     */
    @JavascriptInterface
    fun getStatus(): String {
        val network = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(network)
        
        return when {
            caps == null -> "offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            else -> "unknown"
        }
    }
    
    /**
     * التحقق من الاتصال بالإنترنت
     */
    @JavascriptInterface
    fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(network)
        return caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    /**
     * الحصول على سرعة التحميل (Kbps)
     */
    @JavascriptInterface
    fun getDownloadSpeed(): Int {
        val network = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(network)
        return caps?.linkDownstreamBandwidthKbps ?: 0
    }
    
    /**
     * الحصول على سرعة الرفع (Kbps)
     */
    @JavascriptInterface
    fun getUploadSpeed(): Int {
        val network = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(network)
        return caps?.linkUpstreamBandwidthKbps ?: 0
    }
    
    /**
     * الحصول على معلومات الشبكة كاملة
     */
    @JavascriptInterface
    fun getNetworkInfo(): String {
        val network = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(network)
        
        val json = JSONObject().apply {
            put("status", getStatus())
            put("connected", isConnected())
            put("downloadSpeed", caps?.linkDownstreamBandwidthKbps ?: 0)
            put("uploadSpeed", caps?.linkUpstreamBandwidthKbps ?: 0)
            put("metered", caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false)
            put("vpn", caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true)
        }
        
        return json.toString()
    }
    
    /**
     * بدء مراقبة تغييرات الشبكة
     */
    @JavascriptInterface
    fun startMonitoring(callback: String) {
        jsCallback = callback
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                notifyChange("connected")
            }
            
            override fun onLost(network: Network) {
                notifyChange("disconnected")
            }
            
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                notifyChange("changed")
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }
    
    /**
     * إيقاف مراقبة الشبكة
     */
    @JavascriptInterface
    fun stopMonitoring() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) { }
        }
        networkCallback = null
        jsCallback = null
    }
    
    /**
     * التحقق من إمكانية الوصول لرابط معين
     */
    @JavascriptInterface
    fun canReach(url: String): Boolean {
        return try {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "HEAD"
            connection.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }
    
    private fun notifyChange(event: String) {
        jsCallback?.let { callback ->
            val info = getNetworkInfo()
            handler.post {
                webView?.evaluateJavascript("$callback('$event', $info)", null)
            }
        }
    }
    
    fun destroy() {
        stopMonitoring()
    }
}
