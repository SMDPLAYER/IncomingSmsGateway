package tech.bogomolov.incomingsmsgateway.ui

import android.Manifest
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import tech.bogomolov.incomingsmsgateway.R
import tech.bogomolov.incomingsmsgateway.sms.ForwardingConfig
import tech.bogomolov.incomingsmsgateway.sms.ListAdapter
import tech.bogomolov.incomingsmsgateway.sms.SmsReceiverService

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
        setContentView(R.layout.activity_main)

        findViewById<FloatingActionButton>(R.id.btnAdd).setOnClickListener {
            startActivity(Intent(this, QrCodeActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.openNotificationAccess).setOnClickListener {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
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