package com.jhaiian.clint.privacy

import android.content.Context
import androidx.preference.PreferenceManager
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Encrypted DNS (DNS-over-HTTPS) resolver for the app's own OkHttp clients.
 *
 * Architecture note: Android WebView resolves page-load DNS through the system resolver and
 * does not expose a per-WebView DoH hook, so this cannot redirect DNS for the page bytes that
 * WebView fetches itself. What it *does* cover is every HTTP request the app issues directly
 * (Quiver Guard passthrough/rewrites, the download manager, etc.) — those clients install
 * [resolver] and therefore resolve over HTTPS when the toggle is on.
 *
 * For page-load DNS, enable Android's system-wide Private DNS (Settings › Network › Private
 * DNS); WebView honours that. Together with this toggle, both app and system traffic can be
 * encrypted.
 *
 * The resolver always falls back to the system DNS on any failure or when the preference is
 * off, so it can never break connectivity.
 */
object EncryptedDns {

    private const val DOH_ENDPOINT = "https://cloudflare-dns.com/dns-query"
    // System resolver implemented directly so we don't depend on Dns.SYSTEM, whose
    // access pattern differs between OkHttp major versions.
    private val systemDns: Dns = Dns { hostname ->
        InetAddress.getAllByName(hostname).toList()
    }

    private const val TYPE_A = 1
    private const val TYPE_AAAA = 28

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun isEnabled(): Boolean {
        val ctx = appContext ?: return false
        return PreferenceManager.getDefaultSharedPreferences(ctx)
            .getBoolean(PrivacyPrefs.ENCRYPTED_DNS, PrivacyPrefs.DEFAULT_ENCRYPTED_DNS)
    }

    /**
     * The [Dns] instance to install on OkHttp clients. Reads the preference at lookup time,
     * so toggling the setting requires no client rebuild.
     */
    val resolver: Dns = Dns { hostname ->
        if (!isEnabled()) {
            return@Dns systemDns.lookup(hostname)
        }
        // Never DoH-resolve the DoH provider itself (would recurse) or local-looking hosts.
        if (hostname.equals("cloudflare-dns.com", ignoreCase = true) ||
            hostname.equals("dns.google", ignoreCase = true) ||
            hostname.equals("1.1.1.1", ignoreCase = true) ||
            hostname.equals("localhost", ignoreCase = true)
        ) {
            return@Dns systemDns.lookup(hostname)
        }
        try {
            val resolved = resolveViaDoH(hostname)
            if (resolved.isNotEmpty()) resolved else systemDns.lookup(hostname)
        } catch (e: Exception) {
            systemDns.lookup(hostname)
        }
    }

    // Plain client with NO custom Dns — uses the system resolver to reach the DoH endpoint,
    // so there is no recursion. Short timeouts so fallback is quick on captive/blocked networks.
    private val bootstrapClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun resolveViaDoH(hostname: String): List<InetAddress> {
        val results = ArrayList<InetAddress>()
        for (type in intArrayOf(TYPE_A, TYPE_AAAA)) {
            val url = "$DOH_ENDPOINT?name=" +
                java.net.URLEncoder.encode(hostname, "UTF-8") + "&type=$type"
            val req = Request.Builder()
                .url(url)
                .header("accept", "application/dns-json")
                .get()
                .build()
            try {
                bootstrapClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val body = resp.body?.string() ?: return@use
                    val json = JSONObject(body)
                    if (json.optInt("Status", -1) != 0) return@use
                    val answers = json.optJSONArray("Answer") ?: return@use
                    for (i in 0 until answers.length()) {
                        val ans = answers.optJSONObject(i) ?: continue
                        // For A/AAAA the Answer "data" is an IP literal.
                        val data = ans.optString("data")
                        if (data.isBlank()) continue
                        try {
                            results.add(InetAddress.getByName(data))
                        } catch (e: Exception) {
                            // Not a parseable IP literal — skip.
                        }
                    }
                }
            } catch (e: Exception) {
                // Try the next record type / fall back to system DNS at the call site.
            }
        }
        return results
    }
}
