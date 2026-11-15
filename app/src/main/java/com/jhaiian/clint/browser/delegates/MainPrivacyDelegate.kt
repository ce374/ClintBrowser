package com.jhaiian.clint.browser.delegates

import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.webview.loadJsAsset
import com.jhaiian.clint.privacy.PrivacyPrefs
import com.jhaiian.clint.tabs.BrowserTab

private val privacyScriptHandlers = HashMap<String, List<ScriptHandler>>()

internal fun MainActivity.isGlobalPrivacyControlActive(): Boolean =
    prefs.getBoolean(PrivacyPrefs.GLOBAL_PRIVACY_CONTROL, PrivacyPrefs.DEFAULT_GLOBAL_PRIVACY_CONTROL)

internal fun MainActivity.addPrivacyScripts(tab: BrowserTab) {
    removePrivacyScripts(tab)
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return
    val scripts = ArrayList<String>()
    if (isGlobalPrivacyControlActive()) scripts.add("global_privacy_control.js")
    if (prefs.getBoolean(PrivacyPrefs.WEBRTC_PROTECTION, PrivacyPrefs.DEFAULT_WEBRTC_PROTECTION)) scripts.add("webrtc_protection.js")
    if (prefs.getBoolean(PrivacyPrefs.CANVAS_FINGERPRINT_PROTECTION, PrivacyPrefs.DEFAULT_CANVAS_FINGERPRINT_PROTECTION)) scripts.add("canvas_protection.js")
    if (prefs.getBoolean(PrivacyPrefs.WEBGL_FINGERPRINT_PROTECTION, PrivacyPrefs.DEFAULT_WEBGL_FINGERPRINT_PROTECTION)) scripts.add("webgl_protection.js")
    if (prefs.getBoolean(PrivacyPrefs.AUDIO_FINGERPRINT_PROTECTION, PrivacyPrefs.DEFAULT_AUDIO_FINGERPRINT_PROTECTION)) scripts.add("audio_protection.js")
    if (prefs.getBoolean(PrivacyPrefs.SCREEN_FINGERPRINT_PROTECTION, PrivacyPrefs.DEFAULT_SCREEN_FINGERPRINT_PROTECTION)) scripts.add("screen_protection.js")
    if (scripts.isEmpty()) return
    privacyScriptHandlers[tab.id] = scripts.map {
        WebViewCompat.addDocumentStartJavaScript(tab.webView, loadJsAsset(it), setOf("*"))
    }
}

internal fun MainActivity.removePrivacyScripts(tab: BrowserTab) {
    privacyScriptHandlers.remove(tab.id)?.forEach { it.remove() }
}

/**
 * Encrypted DNS (DNS-over-HTTPS) is applied at the OkHttp client layer via [com.jhaiian.clint.privacy.EncryptedDns.resolver],
 * which reads the preference live at lookup time. Toggling it therefore takes effect for
 * subsequent app-initiated HTTP requests without rebuilding clients. We reload the active
 * tab so the user sees the change reflected and any in-flight sub-resources re-request.
 */
internal fun MainActivity.applyEncryptedDnsSettings() {
    reloadActiveTabWithHeaders()
}
