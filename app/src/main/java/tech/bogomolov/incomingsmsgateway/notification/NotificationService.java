package tech.bogomolov.incomingsmsgateway.notification;



import static tech.bogomolov.incomingsmsgateway.data.ApiServiceKt.callWebHook;
import android.app.Notification;
import tech.bogomolov.incomingsmsgateway.R;
import tech.bogomolov.incomingsmsgateway.sms.ForwardingConfig;
import tech.bogomolov.incomingsmsgateway.sms.RequestWorker;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.lifecycle.Observer;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class NotificationService extends NotificationListenerService
{
	public void onCreate()
	{
		super.onCreate();
		PreferenceManager.setDefaultValues(this, tech.bogomolov.incomingsmsgateway.R.xml.preferences, false);
		Log.d("Notifikator", "Notification service created.");
	}

	public void onDestroy()
	{
		Log.d("Notifikator", "Notification service destroyed.");
		super.onDestroy();
	}

	public void onNotificationPosted(StatusBarNotification sbn)
	{
		String packageName = sbn.getPackageName();
		Notification notification = sbn.getNotification();


		final String from = notification.extras.getString(Notification.EXTRA_TITLE);
		final String text = notification.extras.getString(Notification.EXTRA_TEXT);

		ArrayList<ForwardingConfig> configs = ForwardingConfig.getAll(this);
		String sender = packageName;
		String asterisk = getString(R.string.asterisk);
		for (ForwardingConfig config : configs) {
			if (!sender.equals(config.getSender()) && !config.getSender().equals(asterisk)) {
				continue;
			}

			callWebHook(config,sender, from + " "+text, System.currentTimeMillis(),this);
			break;
		}

	}
}
