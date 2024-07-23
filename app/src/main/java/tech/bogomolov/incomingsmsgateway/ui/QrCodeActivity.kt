package tech.bogomolov.incomingsmsgateway.ui

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import org.json.JSONObject
import tech.bogomolov.incomingsmsgateway.R
import tech.bogomolov.incomingsmsgateway.sms.ForwardingConfig
import java.net.MalformedURLException
import java.net.URL

class QrCodeActivity : AppCompatActivity() {
    private var codeScanner: CodeScanner? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code)
        val scannerView = findViewById<CodeScannerView>(R.id.scanner_view)

        codeScanner = CodeScanner(this, scannerView)

        // Parameters (default values)
        codeScanner?.camera = CodeScanner.CAMERA_BACK // or CAMERA_FRONT or specific camera id
        codeScanner?.formats = CodeScanner.ALL_FORMATS // list of type BarcodeFormat,
        // ex. listOf(BarcodeFormat.QR_CODE)
        codeScanner?.autoFocusMode = AutoFocusMode.SAFE // or CONTINUOUS
        codeScanner?.scanMode = ScanMode.SINGLE // or CONTINUOUS or PREVIEW
        codeScanner?.isAutoFocusEnabled = true // Whether to enable auto focus or not
        codeScanner?.isFlashEnabled = false // Whether to enable flash or not

        // Callbacks
        codeScanner?.decodeCallback = DecodeCallback {
            runOnUiThread {
                val qrData = JSONObject(it.text)
               if (qrData.getString("type").equals("sms")){
                   val config = populateConfig(
                       this,
                       ForwardingConfig(this),
                       qrData.getString("from"),
                       qrData.getString("number"),
                       qrData.getString("url"),
                       qrData.getString("token"),
                   )
                   config?.save()

               }else{
                   val config = populateConfig(
                       this,
                       ForwardingConfig(this),
                       qrData.getString("from"),
                       qrData.getString("number"),
                       qrData.getString("url"),
                       qrData.getString("token"),
                   )
                   config?.save()
               }

                finish()
                Toast.makeText(this, "Scan result: ${it.text}", Toast.LENGTH_LONG).show()
            }
        }
        codeScanner?.errorCallback = ErrorCallback { // or ErrorCallback.SUPPRESS
            runOnUiThread {
                Toast.makeText(this, "Camera initialization error: ${it.message}",
                    Toast.LENGTH_LONG).show()
            }
        }

        scannerView.setOnClickListener {
            codeScanner?.startPreview()
        }
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
            Toast.makeText(context,context.getString(R.string.error_empty_url),Toast.LENGTH_SHORT).show()
            return null
        }
        try {
            URL(url)
        } catch (e: MalformedURLException) {
            Toast.makeText(context,context.getString(R.string.error_wrong_url),Toast.LENGTH_SHORT).show()
            return null
        }

        config.simSlot = 0//Any sim


        config.sender = sender
        config.url = url
        config.template = "{\n  \"from\":\"%from%\",\n  \"text\":\"%text%\",\n  \"iso\":\"%iso%\",\n  \"token\":\"$token\",\n  \"number\":\"$number\"\n}"
//        config.headers = getDefaultJsonHeaders()
        config.retriesNumber = 10
        config.ignoreSsl = true
        config.chunkedMode = true

        return config
    }

    override fun onResume() {
        super.onResume()
        codeScanner?.startPreview()
    }

    override fun onPause() {
        codeScanner?.releaseResources()
        super.onPause()
    }

}