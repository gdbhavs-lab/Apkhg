package {{PACKAGE}}

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.net.URL

/**
 * Smart Notifications - إشعارات ذكية
 * يوفر واجهة JavaScript لإدارة الإشعارات
 */
class SmartNotifications(private val context: Context) {
    
    companion object {
        private const val DEFAULT_CHANNEL_ID = "default_channel"
        private const val DEFAULT_CHANNEL_NAME = "Default"
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private val scheduledJobs = mutableMapOf<Int, Job>()
    
    init {
        createDefaultChannel()
    }
    
    fun setWebView(view: WebView) {
        webView = view
    }
    
    private fun createDefaultChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DEFAULT_CHANNEL_ID,
                DEFAULT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * إنشاء قناة إشعارات جديدة
     */
    @JavascriptInterface
    fun createChannel(channelId: String, name: String, importance: Int): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val imp = when (importance) {
                    0 -> NotificationManager.IMPORTANCE_MIN
                    1 -> NotificationManager.IMPORTANCE_LOW
                    2 -> NotificationManager.IMPORTANCE_DEFAULT
                    3 -> NotificationManager.IMPORTANCE_HIGH
                    else -> NotificationManager.IMPORTANCE_DEFAULT
                }
                val channel = NotificationChannel(channelId, name, imp)
                notificationManager.createNotificationChannel(channel)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * عرض إشعار بسيط
     */
    @JavascriptInterface
    fun show(id: Int, title: String, message: String): Boolean {
        return showWithChannel(id, title, message, DEFAULT_CHANNEL_ID)
    }
    
    /**
     * عرض إشعار في قناة محددة
     */
    @JavascriptInterface
    fun showWithChannel(id: Int, title: String, message: String, channelId: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val pendingIntent = PendingIntent.getActivity(
                context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            
            notificationManager.notify(id, notification)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * عرض إشعار مع شريط تقدم
     */
    @JavascriptInterface
    fun showProgress(id: Int, title: String, progress: Int, max: Int): Boolean {
        return try {
            val notification = NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText("$progress%")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setProgress(max, progress, false)
                .setOngoing(progress < max)
                .build()
            
            notificationManager.notify(id, notification)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * عرض إشعار مع نص طويل
     */
    @JavascriptInterface
    fun showBigText(id: Int, title: String, shortText: String, longText: String): Boolean {
        return try {
            val notification = NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(shortText)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setStyle(NotificationCompat.BigTextStyle().bigText(longText))
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify(id, notification)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * عرض إشعار مع صورة من URL
     */
    @JavascriptInterface
    fun showImage(id: Int, title: String, message: String, imageUrl: String) {
        scope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val url = URL(imageUrl)
                    BitmapFactory.decodeStream(url.openConnection().getInputStream())
                }
                
                val notification = NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setLargeIcon(bitmap)
                    .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
                    .setAutoCancel(true)
                    .build()
                
                notificationManager.notify(id, notification)
            } catch (e: Exception) {
                show(id, title, message)
            }
        }
    }
    
    /**
     * جدولة إشعار
     */
    @JavascriptInterface
    fun schedule(id: Int, title: String, message: String, delayMs: Long): Boolean {
        return try {
            cancelScheduled(id)
            
            val job = scope.launch {
                delay(delayMs)
                show(id, title, message)
                scheduledJobs.remove(id)
            }
            scheduledJobs[id] = job
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * إلغاء إشعار مجدول
     */
    @JavascriptInterface
    fun cancelScheduled(id: Int): Boolean {
        return scheduledJobs[id]?.let {
            it.cancel()
            scheduledJobs.remove(id)
            true
        } ?: false
    }
    
    /**
     * إلغاء إشعار
     */
    @JavascriptInterface
    fun cancel(id: Int) {
        notificationManager.cancel(id)
        cancelScheduled(id)
    }
    
    /**
     * إلغاء جميع الإشعارات
     */
    @JavascriptInterface
    fun cancelAll() {
        notificationManager.cancelAll()
        scheduledJobs.values.forEach { it.cancel() }
        scheduledJobs.clear()
    }
    
    /**
     * الحصول على عدد الإشعارات النشطة
     */
    @JavascriptInterface
    fun getActiveCount(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.activeNotifications.size
        } else 0
    }
    
    /**
     * التحقق من تفعيل الإشعارات
     */
    @JavascriptInterface
    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }
    
    fun destroy() {
        scheduledJobs.values.forEach { it.cancel() }
        scheduledJobs.clear()
        scope.cancel()
    }
}
