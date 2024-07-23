package tech.bogomolov.incomingsmsgateway.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import tech.bogomolov.incomingsmsgateway.R;
public class ConfigurationActivity extends PreferenceActivity
{
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		addPreferencesFromResource(R.xml.preferences);
	}

	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
	{
		Resources res = getResources();
		if (res.getString(R.string.key_send).equals(preference.getKey()))
		{
			NotificationManager mgr = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
			Notification.Builder nb = new Notification.Builder(this);

			nb.setContentTitle(res.getString(R.string.notification_title));
			nb.setContentText(res.getString(R.string.notification_text));
			nb.setSmallIcon(R.drawable.mask);

			BitmapDrawable largeIconDrawable = (BitmapDrawable) res.getDrawable(R.drawable.icon);
			Bitmap largeIconBitmap = largeIconDrawable.getBitmap();
			nb.setLargeIcon(largeIconBitmap);

			mgr.notify(0, nb.build());
			return false;
		}

		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}
}