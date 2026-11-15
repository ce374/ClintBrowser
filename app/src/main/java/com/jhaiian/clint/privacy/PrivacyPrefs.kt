package com.jhaiian.clint.privacy

import android.content.SharedPreferences

object PrivacyPrefs {

    const val STRIP_TRACKING_PARAMS = "strip_tracking_params"
    const val CLEAR_ON_EXIT = "clear_on_exit"
    const val CLEAR_ON_EXIT_HISTORY = "clear_on_exit_history"
    const val CLEAR_ON_EXIT_COOKIES = "clear_on_exit_cookies"
    const val CLEAR_ON_EXIT_CACHE = "clear_on_exit_cache"
    const val CLEAR_ON_EXIT_TABS = "clear_on_exit_tabs"
    const val APP_LOCK = "app_lock"
    const val APP_LOCK_TIMEOUT = "app_lock_timeout_seconds"
    const val SCREENSHOT_BLOCKING = "screenshot_blocking"
    const val INCOGNITO_KEYBOARD = "incognito_keyboard"
    const val CLIPBOARD_CLEAR_SECONDS = "clipboard_auto_clear_seconds"
    const val WEBRTC_PROTECTION = "webrtc_protection"
    const val GLOBAL_PRIVACY_CONTROL = "global_privacy_control"
    const val EXIT_CLEAR_PENDING = "exit_clear_pending"

    // Anti-fingerprinting protections (document-start JS injection).
    const val CANVAS_FINGERPRINT_PROTECTION = "canvas_fingerprint_protection"
    const val WEBGL_FINGERPRINT_PROTECTION = "webgl_fingerprint_protection"
    const val AUDIO_FINGERPRINT_PROTECTION = "audio_fingerprint_protection"
    const val SCREEN_FINGERPRINT_PROTECTION = "screen_fingerprint_protection"
    // Encrypted DNS (DNS-over-HTTPS) for the app's own HTTP clients.
    const val ENCRYPTED_DNS = "encrypted_dns"

    const val DEFAULT_STRIP_TRACKING_PARAMS = true
    const val DEFAULT_CLEAR_ON_EXIT = false
    const val DEFAULT_CLEAR_ON_EXIT_HISTORY = true
    const val DEFAULT_CLEAR_ON_EXIT_COOKIES = true
    const val DEFAULT_CLEAR_ON_EXIT_CACHE = true
    const val DEFAULT_CLEAR_ON_EXIT_TABS = false
    const val DEFAULT_APP_LOCK = false
    const val DEFAULT_APP_LOCK_TIMEOUT = 60
    const val DEFAULT_SCREENSHOT_BLOCKING = false
    const val DEFAULT_INCOGNITO_KEYBOARD = true
    const val DEFAULT_CLIPBOARD_CLEAR_SECONDS = 0
    const val DEFAULT_WEBRTC_PROTECTION = false
    const val DEFAULT_GLOBAL_PRIVACY_CONTROL = true
    // Fingerprinting protections default ON — they are low-risk and high privacy value.
    const val DEFAULT_CANVAS_FINGERPRINT_PROTECTION = true
    const val DEFAULT_WEBGL_FINGERPRINT_PROTECTION = true
    const val DEFAULT_AUDIO_FINGERPRINT_PROTECTION = true
    const val DEFAULT_SCREEN_FINGERPRINT_PROTECTION = true
    // Encrypted DNS defaults OFF — it can break connectivity on captive portals / enterprise
    // networks, so it is opt-in rather than forced.
    const val DEFAULT_ENCRYPTED_DNS = false

    val appLockTimeouts = listOf(0, 60, 300, 900)
    val clipboardDelays = listOf(0, 30, 60, 300)

    fun closesTabsOnExit(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(CLEAR_ON_EXIT, DEFAULT_CLEAR_ON_EXIT) &&
            prefs.getBoolean(CLEAR_ON_EXIT_TABS, DEFAULT_CLEAR_ON_EXIT_TABS)
}
