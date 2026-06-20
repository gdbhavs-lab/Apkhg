package {{PACKAGE}}

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Coroutines Manager - إدارة العمليات المتزامنة
 * يوفر واجهة JavaScript لتنفيذ العمليات في الخلفية
 */
class CoroutinesManager(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val jobs = mutableMapOf<String, Job>()
    private var webView: WebView? = null
    private val handler = Handler(Looper.getMainLooper())
    
    fun setWebView(view: WebView) {
        webView = view
    }
    
    /**
     * تنفيذ طلب HTTP في الخلفية
     * @param taskId معرف المهمة
     * @param url الرابط
     * @param callback اسم دالة JavaScript للاستجابة
     */
    @JavascriptInterface
    fun httpGet(taskId: String, url: String, callback: String) {
        val job = scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    fetchUrl(url)
                }
                callJs(callback, taskId, result, null)
            } catch (e: Exception) {
                callJs(callback, taskId, null, e.message)
            }
        }
        jobs[taskId] = job
    }
    
    /**
     * تنفيذ طلب POST في الخلفية
     */
    @JavascriptInterface
    fun httpPost(taskId: String, url: String, data: String, callback: String) {
        val job = scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    postUrl(url, data)
                }
                callJs(callback, taskId, result, null)
            } catch (e: Exception) {
                callJs(callback, taskId, null, e.message)
            }
        }
        jobs[taskId] = job
    }

    /**
     * تأخير تنفيذ عملية
     */
    @JavascriptInterface
    fun delay(taskId: String, millis: Long, callback: String) {
        val job = scope.launch {
            kotlinx.coroutines.delay(millis)
            callJs(callback, taskId, "completed", null)
        }
        jobs[taskId] = job
    }
    
    /**
     * تنفيذ عمليات متعددة بالتوازي
     */
    @JavascriptInterface
    fun parallel(taskId: String, urls: String, callback: String) {
        val urlList = urls.split(",").map { it.trim() }
        val job = scope.launch {
            try {
                val results = urlList.map { url ->
                    async(Dispatchers.IO) { fetchUrl(url) }
                }.awaitAll()
                callJs(callback, taskId, results.joinToString("|||"), null)
            } catch (e: Exception) {
                callJs(callback, taskId, null, e.message)
            }
        }
        jobs[taskId] = job
    }
    
    /**
     * إلغاء مهمة محددة
     */
    @JavascriptInterface
    fun cancel(taskId: String): Boolean {
        return jobs[taskId]?.let {
            it.cancel()
            jobs.remove(taskId)
            true
        } ?: false
    }
    
    /**
     * إلغاء جميع المهام
     */
    @JavascriptInterface
    fun cancelAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }
    
    /**
     * الحصول على عدد المهام النشطة
     */
    @JavascriptInterface
    fun activeCount(): Int {
        return jobs.count { it.value.isActive }
    }
    
    /**
     * التحقق من حالة مهمة
     */
    @JavascriptInterface
    fun isActive(taskId: String): Boolean {
        return jobs[taskId]?.isActive ?: false
    }
    
    private fun fetchUrl(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        return connection.inputStream.bufferedReader().use { it.readText() }
    }
    
    private fun postUrl(url: String, data: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        connection.outputStream.use { it.write(data.toByteArray()) }
        return connection.inputStream.bufferedReader().use { it.readText() }
    }
    
    private fun callJs(callback: String, taskId: String, result: String?, error: String?) {
        val jsResult = result?.replace("\"", "\\\"")?.replace("\n", "\\n") ?: "null"
        val jsError = error?.replace("\"", "\\\"") ?: "null"
        val js = "$callback('$taskId', \"$jsResult\", ${if (error != null) "\"$jsError\"" else "null"})"
        
        handler.post {
            webView?.evaluateJavascript(js, null)
        }
    }
    
    fun destroy() {
        scope.cancel()
        jobs.clear()
    }
}
