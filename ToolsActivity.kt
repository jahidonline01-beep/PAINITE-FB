package com.painitefb.app

import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class ToolsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tools)

        findViewById<ImageButton>(R.id.btnClose).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnClearAll).setOnClickListener {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE).edit().clear().apply()
            Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show()
            val i = Intent(this, MainActivity::class.java)
            i.putExtra("start_url", MainActivity.URL_REGISTER)
            i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(i)
            finish()
        }

        findViewById<MaterialButton>(R.id.btnOpenRegister).setOnClickListener {
            val i = Intent(this, MainActivity::class.java)
            i.putExtra("start_url", MainActivity.URL_REGISTER)
            i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(i)
            finish()
        }

        findViewById<MaterialButton>(R.id.btnOpenHome).setOnClickListener {
            val i = Intent(this, MainActivity::class.java)
            i.putExtra("start_url", MainActivity.URL_HOME)
            i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(i)
            finish()
        }
    }
}
