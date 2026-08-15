package com.painitefb.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.webkit.CookieManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.util.regex.Pattern

class CookiesActivity : AppCompatActivity() {

    private lateinit var etCookies: EditText
    private lateinit var tvUid: TextView
    private lateinit var tvActive: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cookies)

        etCookies = findViewById(R.id.etCookies)
        tvUid = findViewById(R.id.tvUid)
        tvActive = findViewById(R.id.tvActiveCookies)

        findViewById<ImageButton>(R.id.btnClose).setOnClickListener { finish() }

        refreshDisplay()

        findViewById<MaterialButton>(R.id.btnCopyUid).setOnClickListener {
            val uid = tvUid.text.toString()
            if (uid == "N/A" || uid.isBlank()) {
                Toast.makeText(this, "No UID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            copy(uid)
            Toast.makeText(this, "UID copied", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnCopyCookies).setOnClickListener {
            val c = tvActive.text.toString()
            if (c.isBlank()) {
                Toast.makeText(this, "No cookies", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            copy(c)
            Toast.makeText(this, "Cookies copied", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnApplyCookies).setOnClickListener {
            val raw = etCookies.text.toString().trim()
            if (raw.isEmpty()) {
                Toast.makeText(this, "Paste cookies first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Save + mark for MainActivity to inject & open FB home under navbar
            getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
                .edit()
                .putString(MainActivity.KEY_COOKIES, raw)
                .putBoolean("cookies_updated", true)
                .apply()

            injectNow(raw)
            refreshDisplay()
            Toast.makeText(this, "Cookies saved — opening Facebook under navbar", Toast.LENGTH_SHORT).show()
            finish() // back to MainActivity → onResume loads Home with cookies
        }

        findViewById<MaterialButton>(R.id.btnClearCookies).setOnClickListener {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
                .edit()
                .remove(MainActivity.KEY_COOKIES)
                .apply()
            etCookies.setText("")
            refreshDisplay()
            Toast.makeText(this, "Cookies cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun injectNow(cookieStr: String) {
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        val domains = listOf(
            "https://m.facebook.com",
            "https://www.facebook.com",
            "https://facebook.com"
        )
        cookieStr.split(";")
            .map { it.trim() }
            .filter { it.contains("=") }
            .forEach { pair ->
                domains.forEach { d ->
                    try { cm.setCookie(d, pair) } catch (_: Exception) {}
                }
            }
        cm.flush()
    }

    private fun refreshDisplay() {
        val prefs = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
        var cookies = prefs.getString(MainActivity.KEY_COOKIES, null)
        if (cookies.isNullOrBlank()) {
            cookies = CookieManager.getInstance().getCookie("https://m.facebook.com")
                ?: CookieManager.getInstance().getCookie("https://www.facebook.com")
        }
        tvActive.text = cookies ?: "(empty)"
        tvUid.text = extractUid(cookies) ?: "N/A"
    }

    private fun extractUid(cookies: String?): String? {
        if (cookies.isNullOrBlank()) return null
        val p = Pattern.compile("c_user=(\\d+)", Pattern.CASE_INSENSITIVE)
        val m = p.matcher(cookies)
        if (m.find()) return m.group(1)
        val p2 = Pattern.compile("i_user=(\\d+)", Pattern.CASE_INSENSITIVE)
        val m2 = p2.matcher(cookies)
        if (m2.find()) return m2.group(1)
        return null
    }

    private fun copy(text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("painite", text))
    }
}
