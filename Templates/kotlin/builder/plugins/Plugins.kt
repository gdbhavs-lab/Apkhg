package builder.plugins

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast

/**
 * WebPlugin interface to be implemented by Kotlin plugins.
 * Plugins get a chance to attach to the app and register JS interfaces.
 */
interface WebPlugin {
    /** The JavaScript interface name exposed to WebView (window.<name>) */
    fun name(): String

    /** Called on Activity.onCreate after WebView is constructed */
    fun attach(context: Context, webView: WebView)
}

/**
 * Sample plugin implementation.
 * Exposes simple functions to JS and registers itself on attach.
 *
 * Usage from JavaScript in the loaded page (if registered as a plugin):
 *   Sample.ping()            // returns "pong"
 *   Sample.showToast("Hi")  // shows native toast
 */
class SamplePlugin : WebPlugin {
    private lateinit var appContext: Context

    override fun name(): String = "Sample"

    @JavascriptInterface
    fun ping(): String = "pong"

    @JavascriptInterface
    fun showToast(message: String?) {
        val msg = message?.takeIf { it.isNotBlank() } ?: ""
        Toast.makeText(appContext, msg.ifBlank { "" }, Toast.LENGTH_SHORT).show()
    }

    override fun attach(context: Context, webView: WebView) {
        // Keep application context to avoid leaking the Activity
        appContext = context.applicationContext
        // Register this instance to JS as window.Sample
        webView.addJavascriptInterface(this, name())
    }
}

/**
 * Notes to enable plugins discovery via the build script:
 * - Create a folder under Tools\kotlin\plugins, e.g., Tools\\kotlin\\plugins\\SamplePlugin
 * - Inside it, create a file named plugin.properties with the content:
 *     class=builder.plugins.SamplePlugin
 * - Put your .kt source for the plugin there as well (or copy this SamplePlugin).
 * - Enable flags before building:
 *     set KOTLIN_ENABLED=true
 *     set PLUGINS_ENABLED=true
 *
 * The build script will compile Kotlin sources and, if plugin.properties files are found
 * under Tools\\kotlin\\plugins, it will reflectively load and register those classes.
 */
