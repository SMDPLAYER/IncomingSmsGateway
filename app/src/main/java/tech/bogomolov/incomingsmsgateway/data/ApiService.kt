package tech.bogomolov.incomingsmsgateway.data

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.chuckerteam.chucker.api.ChuckerInterceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url
import tech.bogomolov.incomingsmsgateway.sms.ForwardingConfig
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

interface ApiService {
    @Headers("User-agent: SMS Forwarder App")
    @POST
    fun sendNotification(@Url url: String, @Body body: RequestBody): Call<ResponseBody>
}


object RetrofitClient {
    fun getClient(baseUrl: String, context: Context): Retrofit {

        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
            }

            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
            }

            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                return arrayOf()
            }
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        val sslSocketFactory = sslContext.socketFactory


        val client = OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .addInterceptor(ChuckerInterceptor.Builder(context).build())
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    }
}


class RequestWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

        @Volatile
        private var runCount = 0
    private val maxRetries = 10
    companion object {
        const val DATA_URL = "DATA_URL"
        const val DATA_TEXT = "DATA_TEXT"
        const val DATA_HEADERS = "DATA_HEADERS"
        const val DATA_IGNORE_SSL = "DATA_IGNORE_SSL"
        const val DATA_CHUNKED_MODE = "DATA_CHUNKED_MODE"
        const val DATA_MAX_RETRIES = "DATA_MAX_RETRIES"
        const val RESPONSE_BODY = "RESPONSE_BODY"
        const val RESPONSE_STATUS_CODE = "RESPONSE_STATUS_CODE"
    }

    override fun doWork(): Result {
        runCount++
        if (runCount > maxRetries) {
            return Result.failure()
        }
        val url = inputData.getString(DATA_URL).orEmpty()
        val text = inputData.getString(DATA_TEXT)
        Log.e("TTT_RequestWorker", "url: $url")
        val baseUrl = url.substringBeforeLast("/") + "/"
        val postUrl = url.substringAfterLast("/")
        Log.e("TTT_RequestWorker", "baseUrl: $baseUrl")
        Log.e("TTT_RequestWorker", "postUrl: $postUrl")
        val apiService = RetrofitClient.getClient(baseUrl, applicationContext).create(ApiService::class.java)

        val requestBody = RequestBody.create(MediaType.parse("application/json"), text!!)
        val call = apiService.sendNotification(postUrl,requestBody)

        return try {
            val response: Response<ResponseBody> = call.execute()
            val statusCode = response.code()
            if (statusCode < 200 || statusCode >= 300){
                return doWork()
            }
            val responseBody = response.body()?.string() ?: ""

            val outputData = Data.Builder()
                .putString(RESPONSE_BODY, responseBody)
                .putInt(RESPONSE_STATUS_CODE, statusCode)
                .build()

            Result.success(outputData)
        } catch (e: IOException) {
            e.printStackTrace()
        return Result.failure()
        }
    }
}

fun callWebHook(
    config: ForwardingConfig,
    sender: String,
    content: String,
    timeStamp: Long,
    context: Context
) {

    val message = config.prepareMessage(sender, content, timeStamp)

    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val data = Data.Builder()
        .putString(RequestWorker.DATA_URL, config.url)
        .putString(RequestWorker.DATA_TEXT, message)
        .putString(RequestWorker.DATA_HEADERS, config.headers)
        .putBoolean(RequestWorker.DATA_IGNORE_SSL, config.ignoreSsl)
        .putBoolean(RequestWorker.DATA_CHUNKED_MODE, config.chunkedMode)
        .putInt(RequestWorker.DATA_MAX_RETRIES, config.retriesNumber)
        .build()

    val workRequest = OneTimeWorkRequest.Builder(RequestWorker::class.java)
        .setConstraints(constraints)
        .setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
            TimeUnit.MILLISECONDS
        )
        .setInputData(data)
        .build()

    WorkManager.getInstance(context).enqueue(workRequest)
}