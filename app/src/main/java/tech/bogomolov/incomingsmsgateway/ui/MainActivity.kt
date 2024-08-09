package tech.bogomolov.incomingsmsgateway.ui

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import tech.bogomolov.incomingsmsgateway.R
import tech.bogomolov.incomingsmsgateway.sms.ForwardingConfig
import tech.bogomolov.incomingsmsgateway.sms.ForwardingConfig.getDefaultJsonHeaders
import tech.bogomolov.incomingsmsgateway.sms.ListAdapter
import tech.bogomolov.incomingsmsgateway.sms.SmsReceiverService
import java.net.MalformedURLException
import java.net.URL

class MainActivity : AppCompatActivity() {
    private var listAdapter: ListAdapter? = null

    private val PERMISSION_CODE: Int = 0
    private val PERMISSION_CODE_NOTIFICATION: Int = 0
    private val PERMISSION_CODE_CAMERA: Int = 124
    override fun onResume() {
        super.onResume()
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECEIVE_SMS
            ) != PackageManager.PERMISSION_GRANTED
            ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECEIVE_SMS),
                PERMISSION_CODE
            )
        } else {
       showList()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Включить экран и предотвратить его отключение
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)

        findViewById<FloatingActionButton>(R.id.btnAdd).setOnClickListener {
            startActivity(Intent(this, QrCodeActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.openNotificationAccess).setOnClickListener {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        }
        saveTestConfigs()
    }

    fun saveTestConfigs(){
//        val configSberBankPUSH = populateConfig(
//            this,
//            ForwardingConfig(this),
//            "org.telegram.messenger",
//           "+998913684839",
//            "http://trade.confettipay.com/webhook/set-sms-pochtabank",
//            "ZJoXveLFpM6jjaiEX1bQtieWPTqEoEdp",
//        )
//        configSberBankPUSH?.save()
//        val configSberBankSMS = populateConfig(
//            this,
//            ForwardingConfig(this),
//            "+998913684839",
//           "+998913684839",
//            "http://trade.confettipay.com/webhook/set-sms-sber",
//            "mlJRP6vXBhT_vPyMP_N47-b_8ScXB3kG",
//        )
//        configSberBankSMS?.save()
    }

    fun populateConfig(
        context: Context,
        config: ForwardingConfig,
        from:String,
        number:String,
        url:String,
        token:String
    ): ForwardingConfig? {
        var sender = from
        if (TextUtils.isEmpty(sender)) {
            sender = "*"
        }

        if (TextUtils.isEmpty(url)) {
            Toast.makeText(context,context.getString(R.string.error_empty_url), Toast.LENGTH_SHORT).show()
            return null
        }
        try {
            URL(url)
        } catch (e: MalformedURLException) {
            Toast.makeText(context,context.getString(R.string.error_wrong_url), Toast.LENGTH_SHORT).show()
            return null
        }

        config.simSlot = 0//Any sim


        config.sender = sender
        config.url = url
        config.template = "{\n  \"from\":\"%from%\",\n  \"text\":\"%text%\",\n  \"iso\":\"%iso%\",\n  \"token\":\"$token\",\n  \"number\":\"$number\"\n}"
        config.headers = getDefaultJsonHeaders()
        config.retriesNumber = 10
        config.ignoreSsl = true
        config.chunkedMode = true

        return config
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSION_CODE) {
            return
        }
        for (i in permissions.indices) {
            if (
                permissions[i] != Manifest.permission.RECEIVE_SMS &&
                permissions[i] != Manifest.permission.POST_NOTIFICATIONS &&
                permissions[i] != Manifest.permission.CAMERA
                ) {
                continue
            }

            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                showList()
            } else {
                showInfo(resources.getString(R.string.permission_needed))
            }

            return
        }
    }
    private fun showList() {
        showInfo("")

        val listview = findViewById<ListView>(R.id.listView)

        val configs = ForwardingConfig.getAll(this)

        listAdapter = ListAdapter(configs, this)
        listview.adapter = listAdapter

        val fab = findViewById<FloatingActionButton>(R.id.btnAdd)
        fab.setOnClickListener(this.showAddDialog())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_CODE_NOTIFICATION
                )
                return
            }
        }

        if (!isServiceRunning()) {
            startService()
        }
    }

    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (SmsReceiverService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun startService() {
        val appContext = applicationContext
        val intent = Intent(this, SmsReceiverService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }
    }

    private fun showInfo(text: String) {
        val notice = findViewById<TextView>(R.id.info_notice)
        notice.visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
        notice.text = text
    }

    private fun showAddDialog(): View.OnClickListener {
        return View.OnClickListener { v: View? ->
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    PERMISSION_CODE_CAMERA
                )
            } else {
                startActivity( Intent(this,QrCodeActivity::class.java));
            }
        }
    }
}