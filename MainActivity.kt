package com.painitefb.app

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebChromeClient.FileChooserParams
import android.webkit.ValueCallback
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.button.MaterialButton
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private lateinit var webViewFb: WebView
    private lateinit var webViewMail: WebView
    private lateinit var webViewOverlay: WebView
    private lateinit var panelOverlay: View
    private lateinit var navChips: LinearLayout
    private lateinit var navScroll: HorizontalScrollView
    private lateinit var panelHome: View
    private lateinit var panelUsa: View
    private lateinit var panelAddFd: View
    private lateinit var panel2fa: View
    private lateinit var panelCookies: View
    private lateinit var panelMail: View
    private lateinit var progressFb: ProgressBar
    private var filePathCallback: ValueCallback<Array<android.net.Uri>>? = null
    private val FILE_CHOOSER_REQ = 19001

    private val handler = Handler(Looper.getMainLooper())
    private var totpRunnable: Runnable? = null
    private var usaFemale = true
    private var dragFrom = -1
    private var fbEverLoaded = false
    private var fpInjectedForUrl = ""
    private var mailEverLoaded = false
    private var mailZoom = 70
    private var isDark = false
    private var isDesktop = false

    enum class Tab(val label: String, val icon: String, val color: Int) {
        CREATE("Create", "👤", 0xFF0866FF.toInt()),
        USA("Name", "✦", 0xFF7C3AED.toInt()),
        ADD_FD("Friends", "➕", 0xFF059669.toInt()),
        TWO_FA("2FA", "🔑", 0xFFD97706.toInt()),
        EMAIL("Mail", "✉", 0xFF4F46E5.toInt()),
        COOKIES("Cookies", "🍪", 0xFF2563EB.toInt()),
        CLEAR("Clear", "🗑", 0xFFDC2626.toInt()),
        LOGOUT("Home", "🏠", 0xFFEA580C.toInt())
    }

    private var order = mutableListOf(
        Tab.CREATE, Tab.USA, Tab.ADD_FD, Tab.TWO_FA, Tab.EMAIL, Tab.COOKIES, Tab.CLEAR, Tab.LOGOUT
    )
    private var active = Tab.CREATE

    data class Page(
        val name: String, val role: String, val followers: String,
        val url: String, val official: Boolean, val photo: String = ""
    )

    private val pages = listOf(
        Page("Mark Zuckerberg", "Founder & CEO at Meta", "119M", "https://m.facebook.com/zuck", false, "https://graph.facebook.com/4/picture?type=large"),
        Page("Facebook App", "Official Facebook", "214M", "https://m.facebook.com/facebook", true, "https://graph.facebook.com/facebook/picture?type=large"),
        Page("Meta", "Meta Technologies", "85M", "https://m.facebook.com/meta", true, "https://graph.facebook.com/meta/picture?type=large"),
        Page("Lionel Messi", "World Football Legend", "116M", "https://m.facebook.com/leomessi", false, "https://graph.facebook.com/leomessi/picture?type=large"),
        Page("Cristiano Ronaldo", "Professional Footballer", "170M", "https://m.facebook.com/Cristiano", false, "https://graph.facebook.com/Cristiano/picture?type=large"),
        Page("Elon Musk", "Entrepreneur", "42M", "https://m.facebook.com/elonmusk", false, "https://graph.facebook.com/elonmusk/picture?type=large"),
        Page("Bill Gates", "Gates Foundation", "40M", "https://m.facebook.com/BillGates", false, "https://graph.facebook.com/BillGates/picture?type=large"),
        Page("Dwayne Johnson", "Actor", "62M", "https://m.facebook.com/DwayneJohnson", false, "https://graph.facebook.com/DwayneJohnson/picture?type=large"),
        Page("Taylor Swift", "Artist", "78M", "https://m.facebook.com/TaylorSwift", false, "https://graph.facebook.com/TaylorSwift/picture?type=large"),
        Page("Instagram", "Official Page", "65M", "https://m.facebook.com/instagram", true, "https://graph.facebook.com/instagram/picture?type=large")
    )

    companion object {
        const val URL_REGISTER = "https://m.facebook.com/reg/"
        const val URL_LOGIN = "https://m.facebook.com/login/"
        const val URL_HOME = "https://m.facebook.com/"
        const val URL_MAIL = "https://mail.fb.tools/"
        const val TG = "https://t.me/JAHID_1"
        const val PREFS = "painite_fb_prefs"
        const val KEY_COOKIES = "fb_cookies"
        const val KEY_SECRET = "two_fa_secret"
        const val KEY_DARK = "dark_mode"
        const val KEY_ORDER = "nav_order"
        const val KEY_DESKTOP = "desktop_mode"
        // Chrome-like mobile UA (matches real Chrome on Android)
        // Samsung Galaxy S26 Ultra (SM-S938B) + Via Browser
        const val UA_MOBILE =
            "Mozilla/5.0 (Linux; Android 15; SM-S938B Build/AP3A.240905.015.A2) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/27.0 Chrome/122.0.6261.119 Mobile Safari/537.36"
        const val UA_DESKTOP =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isDark = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_DARK, false)
        isDesktop = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_DESKTOP, false)
        // Apply night mode only once at start (not on every toggle — avoids recreate)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        setContentView(R.layout.activity_main)

        // 1) Bind views + paint HOME UI immediately (no WebView wait)
        webViewFb = findViewById(R.id.webViewFb)
        webViewMail = findViewById(R.id.webViewMail)
        webViewOverlay = findViewById(R.id.webViewOverlay)
        panelOverlay = findViewById(R.id.panelOverlay)
        navChips = findViewById(R.id.navChips)
        navScroll = findViewById(R.id.navScroll)
        panelHome = findViewById(R.id.panelHome)
        panelUsa = findViewById(R.id.panelUsa)
        panelAddFd = findViewById(R.id.panelAddFd)
        panel2fa = findViewById(R.id.panel2fa)
        panelCookies = findViewById(R.id.panelCookies)
        panelMail = findViewById(R.id.panelMail)
        progressFb = findViewById(R.id.progressFb)

        loadOrder()
        applyThemeColors()
        buildNav()
        setupHome()
        setupTop()
        findViewById<MaterialButton>(R.id.btnOverlayClose).setOnClickListener {
            closeOverlay()
        }

        // Hide all panels first
        panelHome.visibility = View.GONE
        panelUsa.visibility = View.GONE
        panelAddFd.visibility = View.GONE
        panel2fa.visibility = View.GONE
        panelCookies.visibility = View.GONE
        panelMail.visibility = View.GONE
        webViewFb.visibility = View.GONE
        panelOverlay.visibility = View.GONE
        active = Tab.CREATE
        progressFb.visibility = View.GONE

        // 2) WebView + session AFTER first frame, then route by login state
        window.decorView.post {
            CookieManager.getInstance().setAcceptCookie(true)
            setupWebViewFb()
            setupWebViewMail()
            setupWebViewOverlay()
            setupUsa()
            setupAddFd()
            setup2fa()
            setupCookies()
            setupMailZoom()
            restoreCookies()
            CookieManager.getInstance().flush()

            // 100%: logged in → Facebook home | not logged in → registration
            if (isLoggedIn()) {
                hideContent()
                webViewFb.visibility = View.VISIBLE
                val home = if (isDesktop) "https://www.facebook.com/" else "https://m.facebook.com/"
                loadFb(home)
                active = Tab.CREATE
            } else {
                hideContent()
                webViewFb.visibility = View.VISIBLE
                val reg = if (isDesktop) "https://www.facebook.com/r.php" else URL_REGISTER
                loadFb(reg)
                active = Tab.CREATE
            }
            buildNav()

            if (!mailEverLoaded) {
                webViewMail.loadUrl(URL_MAIL)
                mailEverLoaded = true
            }
        }
    }

    private fun setupTop() {
        updateDesktopBtn()
        try {
            findViewById<android.widget.ImageView>(R.id.imgLogo).setOnClickListener {
                if (webViewFb.visibility == View.VISIBLE) {
                    webViewFb.reload()
                    toast("Refreshing…")
                } else if (isLoggedIn()) {
                    openFacebook(if (isDesktop) "https://www.facebook.com/" else URL_HOME)
                }
            }
        } catch (_: Exception) {}
        findViewById<MaterialButton>(R.id.btnDesktop).setOnClickListener {
            isDesktop = !isDesktop
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_DESKTOP, isDesktop).apply()
            updateDesktopBtn()
            applyDesktopToFb(reload = webViewFb.visibility == View.VISIBLE)
        }
        findViewById<MaterialButton>(R.id.btnTheme).setOnClickListener {
            isDark = !isDark
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_DARK, isDark).apply()
            findViewById<MaterialButton>(R.id.btnTheme).text = if (isDark) "☀️" else "🌙"
            applyThemeColors()
            buildNav()
            if (active == Tab.ADD_FD) setupAddFd()
            if (active == Tab.COOKIES) refreshCookiesUi()
        }
        // initial icon
        findViewById<MaterialButton>(R.id.btnTheme).text = if (isDark) "☀️" else "🌙"
        findViewById<ImageButton>(R.id.btnTelegram).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TG)))
        }
    }

    private fun updateDesktopBtn() {
        val b = findViewById<MaterialButton>(R.id.btnDesktop)
        b.text = if (isDesktop) "🖥️" else "📱"
        b.textSize = if (isDesktop) 16f else 20f
        b.alpha = 1f
    }

    /** Smooth PC ↔ Mob: keep cookies, no blank flash */
    /** Fast Mob ↔ PC switch — one navigation, no blank delay */
    private fun applyDesktopToFb(reload: Boolean) {
        val ua = if (isDesktop) UA_DESKTOP else UA_MOBILE
        val zoom = if (isDesktop) 90 else 70
        webViewFb.settings.userAgentString = ua
        webViewFb.settings.textZoom = zoom
        webViewFb.settings.cacheMode = WebSettings.LOAD_DEFAULT
        try {
            webViewOverlay.settings.userAgentString = ua
            webViewOverlay.settings.textZoom = zoom
        } catch (_: Exception) {}
        if (!reload) return

        val cur = webViewFb.url
        val home = if (isDesktop) "https://www.facebook.com/" else "https://m.facebook.com/"
        if (cur.isNullOrBlank() || cur == "about:blank") {
            webViewFb.visibility = View.VISIBLE
            webViewFb.loadUrl(home)
            return
        }
        val next = when {
            isDesktop && ("m.facebook.com" in cur || "mbasic.facebook.com" in cur) ->
                cur.replace("m.facebook.com", "www.facebook.com")
                    .replace("mbasic.facebook.com", "www.facebook.com")
            !isDesktop && "www.facebook.com" in cur ->
                cur.replace("www.facebook.com", "m.facebook.com")
            else -> {
                // Same page host already matches mode — soft reload with new UA only
                cur
            }
        }
        webViewFb.visibility = View.VISIBLE
        progressFb.visibility = View.VISIBLE
        // Cancel in-flight request then load once (fast path)
        webViewFb.stopLoading()
        if (next == cur) {
            // Force UA apply: reload same URL once
            webViewFb.reload()
        } else {
            webViewFb.loadUrl(next)
        }
    }

    @Suppress("DEPRECATION")
    private fun applyWebDark() {
        val mode = if (isDark) {
            android.webkit.WebSettings.FORCE_DARK_ON
        } else {
            android.webkit.WebSettings.FORCE_DARK_OFF
        }
        try {
            webViewFb.settings.forceDark = mode
            webViewMail.settings.forceDark = mode
            webViewOverlay.settings.forceDark = mode
        } catch (_: Exception) {}
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            try {
                webViewFb.settings.isAlgorithmicDarkeningAllowed = isDark
                webViewMail.settings.isAlgorithmicDarkeningAllowed = isDark
                webViewOverlay.settings.isAlgorithmicDarkeningAllowed = isDark
            } catch (_: Exception) {}
        }
        injectFullPageDark(webViewFb)
        injectFullPageDark(webViewOverlay)
    }

    /** Full-page dark (not only titles) via invert + forceDark */
    private fun injectFullPageDark(view: WebView?) {
        if (view == null) return
        if (!isDark) {
            view.evaluateJavascript(
                "(function(){try{var s=document.getElementById('pf-dark');if(s)s.remove();document.documentElement.classList.remove('pf-inv');document.documentElement.style.colorScheme='light';document.documentElement.style.filter='';}catch(e){}})();",
                null
            )
            return
        }
        view.evaluateJavascript(
            """
            (function(){
              try {
                document.documentElement.style.colorScheme='dark';
                var s=document.getElementById('pf-dark');
                if(!s){s=document.createElement('style');s.id='pf-dark';
                  (document.head||document.documentElement).appendChild(s);}
                s.textContent=[
                  'html.pf-inv{filter:invert(1) hue-rotate(180deg)!important;background:#fff!important;}',
                  'html.pf-inv img,html.pf-inv video,html.pf-inv picture,html.pf-inv svg,',
                  'html.pf-inv [style*="background-image"]{filter:invert(1) hue-rotate(180deg)!important;}'
                ].join('');
                document.documentElement.classList.add('pf-inv');
              }catch(e){}
            })();
            """.trimIndent(),
            null
        )
    }

    private fun applyThemeColors() {
        val rootBg = if (isDark) 0xFF0B1220.toInt() else 0xFFF0F2F5.toInt()
        val surface = if (isDark) 0xFF1E293B.toInt() else 0xFFFFFFFF.toInt()
        val textPri = if (isDark) 0xFFF1F5F9.toInt() else 0xFF0F172A.toInt()
        findViewById<View>(R.id.root).setBackgroundColor(rootBg)
        navScroll.setBackgroundColor(0xFFFDECEC.toInt()) // light red rubber
        listOf(panelHome, panelUsa, panelAddFd, panel2fa, panelCookies, panelMail).forEach {
            it.setBackgroundColor(rootBg)
        }
        // WebViews stay white for FB readability (FB has its own theme)
        webViewFb.setBackgroundColor(Color.WHITE)
        webViewMail.setBackgroundColor(if (isDark) 0xFF111827.toInt() else Color.WHITE)
        try {
            findViewById<TextView>(R.id.tvUid).setTextColor(
                if (isDark) Color.parseColor("#38BDF8") else Color.parseColor("#0866FF")
            )
            findViewById<EditText>(R.id.etCookies).setTextColor(textPri)
        } catch (_: Exception) {}
        applyWebDark()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebViewFb() {
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        cm.setAcceptThirdPartyCookies(webViewFb, true)

        webViewFb.setBackgroundColor(Color.WHITE)
        webViewFb.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            loadsImagesAutomatically = true
            blockNetworkImage = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = if (isDesktop) UA_DESKTOP else UA_MOBILE
            textZoom = if (isDesktop) 90 else 70
            // Prefer cache for instant back/forward; network for fresh
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            allowContentAccess = true
            allowFileAccess = true
            setGeolocationEnabled(true)
            // Faster layout / less lag
            @Suppress("DEPRECATION")
            setRenderPriority(WebSettings.RenderPriority.HIGH)
        }
        webViewFb.setInitialScale(0)
        webViewFb.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webViewFb.overScrollMode = View.OVER_SCROLL_NEVER
        // Keep renderer process high priority for smooth scrolling
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            try {
                webViewFb.setRendererPriorityPolicy(
                    android.webkit.WebView.RENDERER_PRIORITY_IMPORTANT, false
                )
            } catch (_: Exception) {}
        }

        webViewFb.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                // Block non-http schemes (sflvaws://, fb://, intent://, market:// …)
                // These cause ERR_UNKNOWN_URL_SCHEME blank/error pages
                if (isCustomScheme(url)) {
                    return true // consume — stay on current page
                }
                if (isAccountCentreUrl(url)) {
                    openOverlay(url, "Accounts Centre")
                    return true
                }
                return false
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                if (request?.isForMainFrame != true) return
                val failing = request.url?.toString() ?: ""
                val desc = error?.description?.toString() ?: ""
                if (isCustomScheme(failing) ||
                    desc.contains("ERR_UNKNOWN_URL_SCHEME", true) ||
                    desc.contains("UNKNOWN_URL_SCHEME", true)
                ) {
                    view?.post {
                        val fallback = when {
                            isLoggedIn() ->
                                if (isDesktop) "https://www.facebook.com/" else "https://m.facebook.com/"
                            isDesktop -> "https://www.facebook.com/login/"
                            else -> "https://m.facebook.com/login/"
                        }
                        // Don't loop if already loading fallback
                        if (view.url != fallback) view.loadUrl(fallback)
                    }
                }
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): android.webkit.WebResourceResponse? {
                val url = request?.url?.toString()
                if (isAdUrl(url)) {
                    return android.webkit.WebResourceResponse(
                        "text/plain", "utf-8",
                        java.io.ByteArrayInputStream(ByteArray(0))
                    )
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                if (url != null && isCustomScheme(url)) {
                    val fallback = if (isLoggedIn()) {
                        if (isDesktop) "https://www.facebook.com/" else "https://m.facebook.com/"
                    } else {
                        if (isDesktop) "https://www.facebook.com/login/" else "https://m.facebook.com/login/"
                    }
                    view?.stopLoading()
                    view?.loadUrl(fallback)
                    return
                }
                progressFb.visibility = View.VISIBLE
                if (url != null && isCaptchaUrl(url)) {
                    view?.settings?.textZoom = 62
                } else if (isDesktop) {
                    view?.settings?.textZoom = 90
                    view?.settings?.userAgentString = UA_DESKTOP
                } else {
                    view?.settings?.textZoom = 70
                    view?.settings?.userAgentString = UA_MOBILE
                }
                if (url != null && (url.contains("/reg") || url.contains("r.php") || url.contains("signup"))) {
                    injectChromeFingerprint(view)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressFb.visibility = View.GONE
                persistCookies()
                if (url != null && isCaptchaUrl(url)) {
                    view?.settings?.textZoom = 62
                    injectCaptchaFit(view)
                } else {
                    view?.settings?.textZoom = if (isDesktop) 90 else 70
                }
                // Inject fingerprint only when URL host changes (faster)
                val host = try { android.net.Uri.parse(url ?: "").host ?: "" } catch (_: Exception) { "" }
                if (host.isNotEmpty() && host != fpInjectedForUrl) {
                    fpInjectedForUrl = host
                    injectChromeFingerprint(view)
                }
                injectZoomViewport(view)
                if (isDark) applyWebDark()
            }
        }

        webViewFb.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressFb.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }

            // Photo / file / video upload (Facebook composer, stories, etc.)
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<android.net.Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                requestUploadPerms()
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback
                val intent = try {
                    fileChooserParams?.createIntent()
                } catch (_: Exception) { null } ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                // Also offer camera capture when images accepted
                val gallery = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
                val chooser = Intent.createChooser(intent, "Upload file / photo").apply {
                    putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(gallery))
                }
                return try {
                    startActivityForResult(chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), FILE_CHOOSER_REQ)
                    true
                } catch (_: Exception) {
                    this@MainActivity.filePathCallback = null
                    false
                }
            }

            override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                request?.grant(request.resources)
            }
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: android.webkit.GeolocationPermissions.Callback?
            ) {
                callback?.invoke(origin, true, false)
            }
            override fun onJsBeforeUnload(
                view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?
            ): Boolean {
                result?.confirm()
                return true
            }
            override fun onJsAlert(
                view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?
            ): Boolean {
                // Don't auto-dismiss real alerts; still confirm to unblock UI
                result?.confirm(); return true
            }
            override fun onJsConfirm(
                view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?
            ): Boolean {
                val m = message ?: ""
                // Only auto-OK for leave-page prompts (upload flow)
                if (m.contains("leave", true) || m.contains("navigate", true) ||
                    m.contains("saved", true) || m.contains("Changes", false)) {
                    result?.confirm(); return true
                }
                // Login / other confirms: let default (cancel safer than forced login nav)
                result?.cancel(); return true
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                if (view == null || resultMsg == null) return false
                val temp = WebView(view.context)
                temp.settings.javaScriptEnabled = true
                temp.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(v: WebView?, request: WebResourceRequest?): Boolean {
                        val u = request?.url?.toString() ?: return true
                        if (isAccountCentreUrl(u)) {
                            openOverlay(u, "Accounts Centre")
                        } else {
                            view.post { view.loadUrl(u) }
                        }
                        return true
                    }
                    override fun onPageFinished(v: WebView?, url: String?) {
                        if (!url.isNullOrBlank() && url != "about:blank") {
                            if (isAccountCentreUrl(url)) {
                                openOverlay(url, "Accounts Centre")
                            } else {
                                view.post { view.loadUrl(url) }
                            }
                        }
                    }
                }
                val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
                transport.webView = temp
                resultMsg.sendToTarget()
                return true
            }
        }
    }


    /** Silent ad-block (Samsung Internet style) — no UI */
    private val adHosts = setOf(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "pagead2.googlesyndication.com", "adservice.google.com",
        "facebook.com/tr", "facebook.com/ads", "an.facebook.com",
        "ads-api.twitter.com", "ads.linkedin.com",
        "scorecardresearch.com", "adnxs.com", "adsrvr.org",
        "advertising.com", "taboola.com", "outbrain.com",
        "moatads.com", "amazon-adsystem.com", "adsafeprotected.com"
    )

    private fun isAdUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val u = url.lowercase()
        // Never block main Facebook document navigation
        if (u.contains("m.facebook.com/") && !u.contains("/ads") && !u.contains("doubleclick")) {
            if (!u.contains("tr?") && !u.contains("/ads/")) return false
        }
        return adHosts.any { u.contains(it) }
    }

    private fun isCustomScheme(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val u = url.lowercase()
        if (u.startsWith("http://") || u.startsWith("https://") || u.startsWith("about:")) return false
        // sflvaws:// login_via_app, fb://, intent://, market://, tel:, mailto: etc.
        return true
    }

    private fun isAccountCentreUrl(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("accountscenter") ||
            u.contains("account_center") ||
            u.contains("accounts.facebook") ||
            u.contains("personal_info") ||
            u.contains("password_change") ||
            u.contains("security_settings") ||
            u.contains("/settings/") && (u.contains("account") || u.contains("security") || u.contains("privacy")) ||
            u.contains("facebook.com/settings")
    }

    private fun isCaptchaUrl(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("captcha") || u.contains("checkpoint") ||
            u.contains("confirm") && u.contains("human") ||
            u.contains("/security/") || u.contains("challenge")
    }

    private fun closeOverlay() {
        panelOverlay.visibility = View.GONE
        try { webViewOverlay.stopLoading() } catch (_: Exception) {}
        // Restore Facebook panel under
        if (active == Tab.CREATE || fbEverLoaded) {
            hideContent()
            webViewFb.visibility = View.VISIBLE
        }
    }

        private fun openOverlay(url: String, title: String) {
        runOnUiThread {
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webViewOverlay, true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webViewFb, true)
            CookieManager.getInstance().flush()
            val all = listOf(
                CookieManager.getInstance().getCookie("https://www.facebook.com") ?: "",
                CookieManager.getInstance().getCookie("https://m.facebook.com") ?: ""
            ).maxByOrNull { it.length } ?: ""
            if (all.isNotBlank()) {
                try {
                    CookieManager.getInstance().setCookie("https://accountscenter.facebook.com", all)
                } catch (_: Exception) {}
                CookieManager.getInstance().flush()
            }
            findViewById<TextView>(R.id.tvOverlayTitle).text = title
            panelOverlay.visibility = View.VISIBLE
            webViewOverlay.settings.userAgentString = if (isDesktop) UA_DESKTOP else UA_MOBILE
            webViewOverlay.settings.cacheMode = WebSettings.LOAD_DEFAULT
            webViewOverlay.settings.setSupportZoom(true)
            webViewOverlay.settings.builtInZoomControls = true
            webViewOverlay.settings.displayZoomControls = false
            webViewOverlay.settings.useWideViewPort = true
            webViewOverlay.settings.loadWithOverviewMode = true
            webViewOverlay.settings.textZoom = if (isDesktop) 85 else 70
            var target = url
            if (target.contains("login.php", true) && target.contains("accountscenter", true)) {
                target = "https://accountscenter.facebook.com/"
            }
            val cur = webViewOverlay.url ?: ""
            if (cur.contains("accountscenter") && !cur.contains("login") && target.contains("accountscenter")) {
                if (cur != target) webViewOverlay.loadUrl(target)
            } else {
                webViewOverlay.loadUrl(target)
            }
        }
    }

    private fun setupWebViewOverlay() {
        CookieManager.getInstance().setAcceptThirdPartyCookies(webViewOverlay, true)
        webViewOverlay.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            textZoom = 70
            cacheMode = WebSettings.LOAD_DEFAULT
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = UA_MOBILE
            setSupportMultipleWindows(false)
        }
        webViewOverlay.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webViewOverlay.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (isCustomScheme(url)) return true
                return false
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                persistCookies()
                injectChromeFingerprint(view)
                injectZoomViewport(view)
                if (isDark) applyWebDark()
            }
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                if (request?.isForMainFrame != true) return
                val desc = error?.description?.toString() ?: ""
                if (desc.contains("ERR_TOO_MANY_REDIRECTS", true) || error?.errorCode == -9) {
                    view?.post {
                        CookieManager.getInstance().flush()
                        view.loadUrl("https://accountscenter.facebook.com/")
                    }
                }
            }
        }
        webViewOverlay.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<android.net.Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback
                requestUploadPerms()
                val intent = try { fileChooserParams?.createIntent() } catch (_: Exception) { null }
                    ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                return try {
                    startActivityForResult(Intent.createChooser(intent, "Upload"), FILE_CHOOSER_REQ)
                    true
                } catch (_: Exception) {
                    this@MainActivity.filePathCallback = null
                    false
                }
            }
            override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                request?.grant(request.resources)
            }
        }
    }

    /** Make captcha page smaller so VERIFY is fully visible */
    private fun injectCaptchaFit(view: WebView?) {
        view?.evaluateJavascript(
            """
            (function(){
              try {
                var m=document.querySelector('meta[name=viewport]');
                if(m){m.setAttribute('content','width=device-width,initial-scale=0.75,maximum-scale=2,user-scalable=yes');}
                else {
                  m=document.createElement('meta'); m.name='viewport';
                  m.content='width=device-width,initial-scale=0.75,maximum-scale=2,user-scalable=yes';
                  document.head.appendChild(m);
                }
                document.documentElement.style.zoom='0.85';
              }catch(e){}
            })();
            """.trimIndent(),
            null
        )
    }

    /** Hide "Open app" bottom bar (browser-style dismiss) */
    private fun hideOpenAppBanner(view: WebView?) {
        // Only hide explicit "Open app" install bars — never touch page content
        view?.evaluateJavascript(
            """
            (function(){
              try {
                var s=document.getElementById('pf-hide-openapp');
                if(!s){
                  s=document.createElement('style'); s.id='pf-hide-openapp';
                  s.textContent='a[href*="play.google.com"][href*="facebook"]{display:none!important;}';
                  document.documentElement.appendChild(s);
                }
              }catch(e){}
            })();
            """.trimIndent(),
            null
        )
        handler.postDelayed({ hideOpenAppBannerOnce(view) }, 400)
    }

    private fun hideOpenAppBannerOnce(view: WebView?) {
        view?.evaluateJavascript(
            """
            (function(){
              try {
                document.querySelectorAll('a,button,div,span').forEach(function(el){
                  var t=(el.innerText||'').trim().toLowerCase();
                  if(t==='open app'||t==='open in app'){
                    var p=el.parentElement; if(p) p.style.display='none'; el.style.display='none';
                  }
                });
              }catch(e){}
            })();
            """.trimIndent(),
            null
        )
    }

    /** Mild anti-webdriver fingerprint (cannot fully spoof, but closer to Chrome) */
    private fun injectZoomViewport(view: WebView?) {
        view?.settings?.setSupportZoom(true)
        view?.settings?.builtInZoomControls = true
        view?.settings?.displayZoomControls = false
        view?.settings?.useWideViewPort = true
        view?.settings?.loadWithOverviewMode = true
        view?.evaluateJavascript(
            """
            (function(){
              try {
                var m=document.querySelector('meta[name=viewport]');
                var c='width=device-width, initial-scale=1, minimum-scale=0.2, maximum-scale=5, user-scalable=yes';
                if(m){m.setAttribute('content',c);}
                else {
                  m=document.createElement('meta'); m.name='viewport'; m.content=c;
                  (document.head||document.documentElement).appendChild(m);
                }
                document.documentElement.style.touchAction='pan-x pan-y pinch-zoom';
              }catch(e){}
            })();
            """.trimIndent(),
            null
        )
    }

    private fun injectChromeFingerprint(view: WebView?) {
        val langJs = java.util.Locale.getDefault().toLanguageTag().let {
            if (it.startsWith("en")) "en-US" else it
        }
        val platform = if (isDesktop) "Win32" else "Linux armv8l"
        val touch = if (isDesktop) 0 else 5
        val js = """
            (function(){
              try {
                try{Object.defineProperty(navigator,'webdriver',{get:function(){return undefined}})}catch(e){}
                try{Object.defineProperty(navigator,'vendor',{get:function(){return 'Google Inc.'}})}catch(e){}
                try{Object.defineProperty(navigator,'language',{get:function(){return '$langJs'}})}catch(e){}
                try{Object.defineProperty(navigator,'languages',{get:function(){return['$langJs','en-US','en']}})}catch(e){}
                try{Object.defineProperty(navigator,'platform',{get:function(){return '$platform'}})}catch(e){}
                try{Object.defineProperty(navigator,'maxTouchPoints',{get:function(){return $touch}})}catch(e){}
                try{Object.defineProperty(navigator,'hardwareConcurrency',{get:function(){return 8}})}catch(e){}
                try{Object.defineProperty(navigator,'deviceMemory',{get:function(){return 8}})}catch(e){}
                if(!window.chrome){window.chrome={runtime:{},app:{}};}
                try{
                  if(navigator.connection){
                    Object.defineProperty(navigator.connection,'effectiveType',{get:function(){return '4g'}});
                  }
                }catch(e){}
                try{window.__PF_DEVICE__={brand:'Samsung',model:'SM-S938B',name:'Galaxy S26 Ultra',browser:'Samsung Internet 27',android:'15'};}catch(e){}
              }catch(e){}
            })();
        """.trimIndent()
        view?.evaluateJavascript(js, null)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebViewMail() {
        CookieManager.getInstance().setAcceptThirdPartyCookies(webViewMail, true)
        webViewMail.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            cacheMode = WebSettings.LOAD_DEFAULT
            loadsImagesAutomatically = true
            // Desktop UA so two-pane inbox (list + message) shows like PC
            userAgentString = UA_DESKTOP
            textZoom = 75
            @Suppress("DEPRECATION")
            setRenderPriority(WebSettings.RenderPriority.HIGH)
        }
        webViewMail.setInitialScale(1)
        webViewMail.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webViewMail.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.settings?.userAgentString = UA_DESKTOP
                view?.settings?.textZoom = mailZoom.coerceIn(50, 150).let {
                    // default view shows both panes
                    if (mailZoom == 100) 70 else it
                }
                injectZoomViewport(view)
                view?.evaluateJavascript(
                    """
                    (function(){
                      try {
                        var m=document.querySelector('meta[name=viewport]');
                        var c='width=1280, initial-scale=0.45, minimum-scale=0.25, maximum-scale=3, user-scalable=yes';
                        if(m) m.setAttribute('content',c);
                        else {
                          m=document.createElement('meta'); m.name='viewport'; m.content=c;
                          (document.head||document.documentElement).appendChild(m);
                        }
                        var s=document.getElementById('pf-mail');
                        if(!s){s=document.createElement('style');s.id='pf-mail';
                          (document.head||document.documentElement).appendChild(s);}
                        s.textContent='header,nav,.navbar,.topbar{display:none!important;height:0!important;} body{min-width:1100px!important;}';
                      }catch(e){}
                    })();
                    """.trimIndent(),
                    null
                )
            }
        }
        webViewMail.webChromeClient = WebChromeClient()
    }

    private fun setupMailZoom() {
        fun applyZoom() {
            webViewMail.settings.textZoom = mailZoom
            findViewById<TextView>(R.id.tvZoom).text = "$mailZoom%"
        }
        findViewById<MaterialButton>(R.id.btnZoomIn).setOnClickListener {
            if (mailZoom < 200) { mailZoom += 10; applyZoom() }
        }
        findViewById<MaterialButton>(R.id.btnZoomOut).setOnClickListener {
            if (mailZoom > 50) { mailZoom -= 10; applyZoom() }
        }
        findViewById<MaterialButton>(R.id.btnMailReload).setOnClickListener { webViewMail.reload() }
    }

    private fun buildNav() {
        navChips.removeAllViews()
        val chipBg = if (isDark) 0xFF1E293B.toInt() else Color.WHITE
        order.forEachIndexed { index, tab ->
            val btn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "${tab.icon}  ${tab.label}"
                textSize = 13.5f
                isAllCaps = false
                minimumHeight = 0
                minHeight = 0
                setPadding(42, 28, 42, 28)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.marginEnd = 10 }
                setCornerRadius(50)
                if (tab == active && tab != Tab.CLEAR && tab != Tab.LOGOUT) {
                    setBackgroundColor(tab.color)
                    setTextColor(Color.WHITE)
                    // Full-body glow indicator
                    setStrokeColor(ColorStateList.valueOf(0x99FFFFFF.toInt()))
                    setStrokeWidth(4)
                    elevation = 8f
                } else {
                    setBackgroundColor(chipBg)
                    setTextColor(tab.color)
                    setStrokeColor(ColorStateList.valueOf(tab.color))
                    setStrokeWidth(1)
                    elevation = 0f
                }
                setOnClickListener { onNavClick(tab) }
                setOnLongClickListener {
                    dragFrom = index
                    navScroll.requestDisallowInterceptTouchEvent(true)
                    true
                }
                setOnTouchListener { _, e ->
                    if (dragFrom < 0) return@setOnTouchListener false
                    when (e.action) {
                        MotionEvent.ACTION_MOVE -> {
                            navScroll.requestDisallowInterceptTouchEvent(true)
                            val w = navScroll.width
                            // Smooth edge auto-scroll for far chips
                            when {
                                e.rawX < 100 -> navScroll.smoothScrollBy(-60, 0)
                                e.rawX > w - 100 -> navScroll.smoothScrollBy(60, 0)
                            }
                            for (i in 0 until navChips.childCount) {
                                val child = navChips.getChildAt(i)
                                val loc = IntArray(2)
                                child.getLocationOnScreen(loc)
                                if (e.rawX >= loc[0] && e.rawX <= loc[0] + child.width && i != dragFrom) {
                                    val item = order.removeAt(dragFrom)
                                    order.add(i, item)
                                    dragFrom = i
                                    saveOrder()
                                    buildNav()
                                    return@setOnTouchListener true
                                }
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            dragFrom = -1
                            navScroll.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    true
                }
            }
            navChips.addView(btn)
        }
    }

    private fun onNavClick(tab: Tab) {
        when (tab) {
            Tab.CLEAR -> confirmThen("Clear all data?", "Cookies, cache and session will be removed.") { clearAll() }
            Tab.LOGOUT -> goFacebookHome()
            else -> showTab(tab)
        }
    }

    private fun confirmThen(title: String, msg: String, action: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("Confirm") { _, _ -> action() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTab(tab: Tab) {
        active = tab
        hideContent()
        // Keep Account Centre WebView state — only hide panel, no reload
        if (panelOverlay.visibility == View.VISIBLE) {
            panelOverlay.visibility = View.GONE
            // do not stopLoading / clear overlay
        }
        when (tab) {
            Tab.CREATE -> {
                if (isLoggedIn() || fbEverLoaded) {
                    webViewFb.visibility = View.VISIBLE
                    if (!fbEverLoaded) loadFb(URL_HOME)
                } else {
                    panelHome.visibility = View.VISIBLE
                }
            }
            Tab.USA -> panelUsa.visibility = View.VISIBLE
            Tab.ADD_FD -> panelAddFd.visibility = View.VISIBLE
            Tab.TWO_FA -> panel2fa.visibility = View.VISIBLE
            Tab.EMAIL -> {
                panelMail.visibility = View.VISIBLE
                if (!mailEverLoaded) {
                    webViewMail.loadUrl(URL_MAIL)
                    mailEverLoaded = true
                }
            }
            Tab.COOKIES -> {
                panelCookies.visibility = View.VISIBLE
                refreshCookiesUi()
            }
            else -> {}
        }
        buildNav()
    }

    private fun hideContent() {
        panelHome.visibility = View.GONE
        panelUsa.visibility = View.GONE
        panelAddFd.visibility = View.GONE
        panel2fa.visibility = View.GONE
        panelCookies.visibility = View.GONE
        panelMail.visibility = View.GONE
        webViewFb.visibility = View.GONE
    }

    private fun loadFb(url: String) {
        if (webViewFb.url == url) return
        webViewFb.loadUrl(url)
        fbEverLoaded = true
    }

    /** Login থাকলে প্রোফাইল খুলে Follow ট্যাপ; না থাকলে শুধু প্রোফাইল */
    private fun followOrOpenProfile(profileUrl: String) {
        active = Tab.CREATE
        hideContent()
        panelOverlay.visibility = View.GONE
        webViewFb.visibility = View.VISIBLE
        fbEverLoaded = true
        buildNav()
        webViewFb.loadUrl(profileUrl)
        if (isLoggedIn()) {
            handler.postDelayed({
                webViewFb.evaluateJavascript(
                    """
                    (function(){
                      try {
                        var nodes=document.querySelectorAll('a,button,[role="button"],div[role="button"]');
                        for (var i=0;i<nodes.length;i++){
                          var t=(nodes[i].innerText||nodes[i].textContent||'').trim().toLowerCase();
                          if(t==='follow'||t==='like'||t==='ফলো'||t.indexOf('follow')===0){
                            nodes[i].click(); return 'ok';
                          }
                        }
                      }catch(e){}
                      return 'none';
                    })();
                    """.trimIndent(),
                    null
                )
            }, 1600)
            toast("Following…")
        } else {
            toast("Profile opened — login to Follow")
        }
    }

    private fun openFacebook(url: String) {
        active = Tab.CREATE
        hideContent()
        panelOverlay.visibility = View.GONE
        webViewFb.visibility = View.VISIBLE
        loadFb(url)
        buildNav()
    }

    private fun isLoggedIn(): Boolean {
        val c = liveCookieString()
        return c.contains("c_user=") && c.contains("xs=")
    }

    private fun liveCookieString(): String {
        val a = CookieManager.getInstance().getCookie("https://m.facebook.com") ?: ""
        val b = CookieManager.getInstance().getCookie("https://www.facebook.com") ?: ""
        val c = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_COOKIES, "") ?: ""
        return listOf(a, b, c).maxByOrNull { it.length } ?: ""
    }

    private fun setupHome() {
        findViewById<MaterialButton>(R.id.btnCreateAccount).setOnClickListener {
            applyDesktopToFb(reload = false)
            val reg = if (isDesktop) "https://www.facebook.com/r.php" else URL_REGISTER
            openFacebook(reg)
        }
        findViewById<MaterialButton>(R.id.btnLogin).setOnClickListener {
            applyDesktopToFb(reload = false)
            val loginUrl = if (isDesktop) "https://www.facebook.com/login/" else URL_LOGIN
            openFacebook(loginUrl)
        }
    }

    private fun setupUsa() {
        val tvFull = findViewById<TextView>(R.id.tvFullName)
        val tvFirst = findViewById<TextView>(R.id.tvFirst)
        val tvLast = findViewById<TextView>(R.id.tvLast)
        fun gen() {
            val nameLine = UsaGenerator.generate(usaFemale).removePrefix("Name:").trim()
            val parts = nameLine.split(" ")
            tvFirst.text = parts.getOrElse(0) { "—" }
            tvLast.text = parts.drop(1).joinToString(" ").ifBlank { "—" }
            tvFull.text = nameLine
        }
        fun styleGender() {
            val f = findViewById<MaterialButton>(R.id.btnFemale)
            val m = findViewById<MaterialButton>(R.id.btnMale)
            val active = 0xFF7C3AED.toInt()
            val idle = 0xFFEDE4FF.toInt()
            f.strokeWidth = 0
            m.strokeWidth = 0
            f.elevation = 0f
            m.elevation = 0f
            if (usaFemale) {
                f.backgroundTintList = ColorStateList.valueOf(active)
                f.setTextColor(Color.WHITE)
                m.backgroundTintList = ColorStateList.valueOf(idle)
                m.setTextColor(0xFF6B21A8.toInt())
            } else {
                m.backgroundTintList = ColorStateList.valueOf(active)
                m.setTextColor(Color.WHITE)
                f.backgroundTintList = ColorStateList.valueOf(idle)
                f.setTextColor(0xFF6B21A8.toInt())
            }
        }
        findViewById<MaterialButton>(R.id.btnFemale).setOnClickListener { usaFemale = true; styleGender(); gen() }
        findViewById<MaterialButton>(R.id.btnMale).setOnClickListener { usaFemale = false; styleGender(); gen() }
        findViewById<MaterialButton>(R.id.btnGenUsa).setOnClickListener { gen() }
        findViewById<MaterialButton>(R.id.btnCopyFull).setOnClickListener {
            copy(tvFull.text.toString()); toast("✓ Copied: ${tvFull.text}")
        }
        findViewById<MaterialButton>(R.id.btnCopyFirst).setOnClickListener {
            copy(tvFirst.text.toString()); toast("✓ Copied: ${tvFirst.text}")
        }
        findViewById<MaterialButton>(R.id.btnCopyLast).setOnClickListener {
            copy(tvLast.text.toString()); toast("✓ Copied: ${tvLast.text}")
        }
        styleGender(); gen()
    }

    private fun setupAddFd() {
        fun fill(filter: String) {
            val list = findViewById<LinearLayout>(R.id.addFdList)
            list.removeAllViews()
            val data = when (filter) {
                "official" -> pages.filter { it.official }
                "celeb" -> pages.filter { !it.official }
                else -> pages
            }
            val titleColor = if (isDark) Color.WHITE else Color.parseColor("#0F172A")
            val subColor = if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
            val avatarColors = intArrayOf(
                0xFF0866FF.toInt(), 0xFF7C3AED.toInt(), 0xFF059669.toInt(),
                0xFFD97706.toInt(), 0xFFDB2777.toInt(), 0xFF0891B2.toInt()
            )
            data.forEachIndexed { idx, p ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(20, 20, 20, 20)
                    setBackgroundColor(if (isDark) 0xFF1E293B.toInt() else Color.WHITE)
                    elevation = 5f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = 12 }
                }
                val initials = p.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
                val avatar = android.widget.ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(104, 104).also { it.marginEnd = 16 }
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    val gd = GradientDrawable()
                    gd.shape = GradientDrawable.OVAL
                    gd.setColor(avatarColors[idx % avatarColors.size])
                    background = gd
                    clipToOutline = true
                    outlineProvider = object : android.view.ViewOutlineProvider() {
                        override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                            outline.setOval(0, 0, view.width, view.height)
                        }
                    }
                }
                loadProfilePhoto(avatar, p.photo)
                val info = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                info.addView(TextView(this).apply {
                    text = p.name; setTypeface(null, Typeface.BOLD); textSize = 16f; setTextColor(titleColor)
                })
                info.addView(TextView(this).apply {
                    text = p.role; textSize = 12f; setTextColor(subColor)
                })
                info.addView(TextView(this).apply {
                    text = "${p.followers} Followers"; textSize = 12f
                    setTextColor(0xFF0866FF.toInt()); setTypeface(null, Typeface.BOLD)
                })
                val follow = MaterialButton(this).apply {
                    text = "Follow"; isAllCaps = false; textSize = 13f
                    setBackgroundColor(0xFF0866FF.toInt()); setTextColor(Color.WHITE)
                    setCornerRadius(16)
                    setOnClickListener { followOrOpenProfile(p.url) }
                }
                row.addView(avatar); row.addView(info); row.addView(follow); list.addView(row)
            }
        }
        findViewById<MaterialButton>(R.id.tabAll).setOnClickListener { fill("all") }
        findViewById<MaterialButton>(R.id.tabOfficial).setOnClickListener { fill("official") }
        findViewById<MaterialButton>(R.id.tabCeleb).setOnClickListener { fill("celeb") }
        fill("all")
    }

    private fun setup2fa() {
        val et = findViewById<EditText>(R.id.etSecret)
        et.setText(getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_SECRET, "") ?: "")
        val tick = object : Runnable {
            override fun run() {
                val secret = et.text.toString()
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_SECRET, secret).apply()
                val clean = TotpHelper.normalizeSecret(secret)
                if (clean.isBlank()) {
                    findViewById<TextView>(R.id.tvCode).text = "— — — — — —"
                    findViewById<TextView>(R.id.tvTimer).text = "Paste Facebook 2FA secret key"
                } else {
                    val (code, rem) = TotpHelper.generate(clean)
                    // Display: 123 456 for readability
                    val shown = if (code.length == 6) "${code.substring(0,3)} ${code.substring(3)}" else code
                    findViewById<TextView>(R.id.tvCode).text = shown
                    findViewById<TextView>(R.id.tvTimer).text = "Refreshes in: ${rem}s"
                }
                if (panel2fa.visibility == View.VISIBLE) handler.postDelayed(this, 1000)
                else handler.postDelayed(this, 3000)
            }
        }
        totpRunnable = tick
        handler.post(tick)
        findViewById<MaterialButton>(R.id.btnClearSecret).setOnClickListener {
            et.setText("")
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(KEY_SECRET).apply()
        }
        findViewById<MaterialButton>(R.id.btnCopyCode).setOnClickListener {
            val secret = et.text.toString()
            val (code, _) = TotpHelper.generate(secret)
            if (code != "000000" || TotpHelper.normalizeSecret(secret).isNotBlank()) {
                copy(code)
                toast("✓ Copied: $code")
            } else {
                toast("Enter a valid 2FA key first")
            }
        }
    }

    private fun setupCookies() {
        findViewById<MaterialButton>(R.id.btnCopyUid).setOnClickListener {
            val u = findViewById<TextView>(R.id.tvUid).text.toString()
            if (u != "N/A") { copy(u); toast("✓ UID copied") }
        }
        findViewById<MaterialButton>(R.id.btnCopyCookies).setOnClickListener {
            val c = findViewById<TextView>(R.id.tvActiveCookies).text.toString()
            if (!c.startsWith("(Empty")) { copy(c); toast("✓ Cookies copied") }
        }
        findViewById<MaterialButton>(R.id.btnApplyCookies).setOnClickListener {
            val raw = findViewById<EditText>(R.id.etCookies).text.toString().trim()
            if (raw.isEmpty()) { toast("Paste cookies first"); return@setOnClickListener }
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_COOKIES, raw).apply()
            injectCookies(raw)
            CookieManager.getInstance().flush()
            refreshCookiesUi()
            toast("✓ Cookies applied")
            openFacebook(URL_HOME)
        }
    }

    private fun refreshCookiesUi() {
        val cookies = liveCookieString()
        val has = cookies.contains("c_user=")
        val tv = findViewById<TextView>(R.id.tvActiveCookies)
        val uid = findViewById<TextView>(R.id.tvUid)
        uid.setTextColor(if (isDark) Color.parseColor("#38BDF8") else Color.parseColor("#0866FF"))
        uid.textSize = 20f
        uid.setTypeface(null, Typeface.BOLD)
        findViewById<EditText>(R.id.etCookies).apply {
            setTextColor(if (isDark) Color.parseColor("#F1F5F9") else Color.parseColor("#0F172A"))
            setHintTextColor(Color.parseColor("#94A3B8"))
            setBackgroundColor(if (isDark) 0xFF0F172A.toInt() else 0xFFF1F5F9.toInt())
        }
        if (!has) {
            tv.text = "(Empty — no session)"
            tv.setTextColor(Color.parseColor("#94A3B8"))
            uid.text = "N/A"
        } else {
            tv.text = cookies
            tv.setTextColor(Color.parseColor("#34D399"))
            uid.text = extractUid(cookies) ?: "N/A"
        }
    }

    private fun clearAll() {
        // Instant blank — no residual page
        webViewFb.stopLoading()
        webViewFb.loadUrl("about:blank")
        webViewOverlay.stopLoading()
        webViewOverlay.loadUrl("about:blank")
        webViewMail.stopLoading()

        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        try { WebStorage.getInstance().deleteAllData() } catch (_: Exception) {}
        webViewFb.clearCache(true)
        webViewFb.clearHistory()
        webViewFb.clearFormData()
        webViewMail.clearCache(true)
        webViewMail.clearHistory()
        webViewOverlay.clearCache(true)
        webViewOverlay.clearHistory()
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply()
        fbEverLoaded = false
        mailEverLoaded = false
        fpInjectedForUrl = ""

        active = Tab.CREATE
        hideContent()
        panelOverlay.visibility = View.GONE
        webViewFb.visibility = View.VISIBLE
        webViewFb.setBackgroundColor(Color.WHITE)
        // Single load to register — avoids FB temp-block from rapid reloads
        webViewFb.settings.userAgentString = UA_MOBILE
        webViewFb.settings.textZoom = 70
        webViewFb.loadUrl(URL_REGISTER)
        fbEverLoaded = true
        buildNav()
        toast("All data cleared")
    }

    /** Home: Facebook home — one navigation only (no double-load block) */
    private fun goFacebookHome() {
        active = Tab.CREATE
        panelOverlay.visibility = View.GONE
        hideContent()
        webViewFb.visibility = View.VISIBLE
        progressFb.visibility = View.GONE
        val home = if (isDesktop) "https://www.facebook.com/" else "https://m.facebook.com/"
        val cur = webViewFb.url ?: ""
        // Already on feed/home → soft scroll, no full reload (prevents temp block)
        if (cur == home || (cur.contains("facebook.com") &&
                !cur.contains("login") && !cur.contains("reg") &&
                !cur.contains("checkpoint") && !cur.contains("accountscenter"))) {
            webViewFb.evaluateJavascript(
                "try{window.scrollTo(0,0);}catch(e){}",
                null
            )
        } else {
            webViewFb.loadUrl(home)
            fbEverLoaded = true
        }
        buildNav()
    }

    private fun logout() {
        val cm = CookieManager.getInstance()
        listOf("https://m.facebook.com", "https://www.facebook.com", "https://facebook.com").forEach { d ->
            listOf("c_user", "xs", "fr", "sb", "datr").forEach { name ->
                try { cm.setCookie(d, "$name=; Max-Age=0; path=/; domain=.facebook.com") } catch (_: Exception) {}
            }
        }
        cm.flush()
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(KEY_COOKIES).apply()
        active = Tab.CREATE
        hideContent()
        panelOverlay.visibility = View.GONE
        webViewFb.stopLoading()
        webViewFb.setBackgroundColor(Color.WHITE)
        webViewFb.visibility = View.VISIBLE
        progressFb.visibility = View.VISIBLE
        // Reset UA + zoom for clean login page
        webViewFb.settings.userAgentString = if (isDesktop) UA_DESKTOP else UA_MOBILE
        webViewFb.settings.textZoom = if (isDesktop) 90 else 70
        val login = if (isDesktop) "https://www.facebook.com/login/" else "https://m.facebook.com/login/"
        webViewFb.loadUrl(login)
        fbEverLoaded = true
        buildNav()
        toast("Logged out")
    }

    private fun persistCookies() {
        val c = CookieManager.getInstance().getCookie("https://m.facebook.com")
            ?: CookieManager.getInstance().getCookie("https://www.facebook.com")
            ?: return
        if (c.contains("c_user=")) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_COOKIES, c).apply()
        }
    }

    private fun restoreCookies() {
        val raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_COOKIES, null) ?: return
        injectCookies(raw)
    }

    private fun injectCookies(cookieStr: String) {
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        listOf("https://m.facebook.com", "https://www.facebook.com", "https://facebook.com").forEach { d ->
            cookieStr.split(";").map { it.trim() }.filter { it.contains("=") }.forEach { pair ->
                try {
                    cm.setCookie(d, pair)
                    cm.setCookie(d, "$pair; path=/")
                } catch (_: Exception) {}
            }
        }
        cm.flush()
    }

    private fun extractUid(cookies: String?): String? {
        if (cookies.isNullOrBlank()) return null
        val m = Pattern.compile("c_user=(\\d+)", Pattern.CASE_INSENSITIVE).matcher(cookies)
        if (m.find()) return m.group(1)
        return null
    }


    private fun requestUploadPerms() {
        val need = mutableListOf<String>()
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            need.add(android.Manifest.permission.CAMERA)
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                need.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                need.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (need.isNotEmpty()) {
            requestPermissions(need.toTypedArray(), 19002)
        }
    }

    private fun loadProfilePhoto(imageView: android.widget.ImageView, url: String) {
        if (url.isBlank()) return
        Thread {
            try {
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", UA_MOBILE)
                conn.connect()
                if (conn.responseCode in 200..399) {
                    val bmp = android.graphics.BitmapFactory.decodeStream(conn.inputStream)
                    if (bmp != null) runOnUiThread { imageView.setImageBitmap(bmp) }
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun loadOrder() {
        val raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_ORDER, null) ?: return
        val mapped = raw.split(",").mapNotNull { n -> Tab.entries.find { it.name == n } }
        if (mapped.size == order.size) { order.clear(); order.addAll(mapped) }
    }

    private fun saveOrder() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putString(KEY_ORDER, order.joinToString(",") { it.name }).apply()
    }

    private fun copy(text: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("painite", text))
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CHOOSER_REQ) {
            var results: Array<android.net.Uri>? = null
            if (resultCode == RESULT_OK) {
                results = FileChooserParams.parseResult(resultCode, data)
                if (results == null && data?.data != null) {
                    results = arrayOf(data.data!!)
                }
                // Persist URI permission for upload
                results?.forEach { uri ->
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Exception) {
                        try {
                            grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } catch (_: Exception) {}
                    }
                }
            }
            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
            // Keep session warm after picker
            CookieManager.getInstance().flush()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            panelOverlay.visibility == View.VISIBLE -> {
                if (webViewOverlay.canGoBack()) webViewOverlay.goBack()
                else closeOverlay()
            }
            webViewFb.visibility == View.VISIBLE && webViewFb.canGoBack() -> webViewFb.goBack()
            panelMail.visibility == View.VISIBLE && webViewMail.canGoBack() -> webViewMail.goBack()
            webViewFb.visibility == View.VISIBLE && !isLoggedIn() -> {
                hideContent(); panelHome.visibility = View.VISIBLE
                fbEverLoaded = false; active = Tab.CREATE; buildNav()
            }
            else -> super.onBackPressed()
        }
    }

    override fun onDestroy() {
        totpRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }

    override fun onPause() {
        persistCookies()
        CookieManager.getInstance().flush()
        super.onPause()
    }
}
