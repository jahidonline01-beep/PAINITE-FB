# Painite FB PRO — Native Android (Kotlin)

Fixed top navbar + all feature buttons. Facebook opens in **native WebView under the navbar**.

## Navbar buttons (all working)

| Button | Action |
|--------|--------|
| **Create Facebook** | WebView → registration / FB (under bar) |
| **USA Name Generator** | Generate USA profile, copy |
| **ADD FD** | Official pages list → open in WebView under bar |
| **2FA Code** | Secret → live 6-digit code, copy |
| **E-mail Inbox** | mail.fb.tools in WebView under bar |
| **Cookies & UID** | Import cookies → CookieManager → FB Home under bar |
| **CLEAR DATA** | Wipe cookies + storage |
| **LOGOUT** | Clear session → login page |
| Theme (sun icon) | Dark / light |

FB sub-bar (when Create tab): Create Account, Log In, Home, Reload, Back

## Build
Open in Android Studio → Sync → Run / Build APK  
Or GitHub Actions on `main`.

`applicationId`: com.painitefb.app
