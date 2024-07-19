package tech.bogomolov.incomingsmsgateway.client;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import tech.bogomolov.incomingsmsgateway.R;
final class BitmapHelper
{
	public final static Bitmap getPackageIcon(Context context, String packageName, int id)
	{
		if (id == 0)
			return null;

		try
		{
			Context remoteContext = context.createPackageContext(packageName, 0);
			Drawable icon = remoteContext.getResources().getDrawable(id);
			if (icon == null)
				return null;

			return ((BitmapDrawable)icon).getBitmap();
		}
		catch (Exception ex)
		{
			return null;
		}
	}

	public final static Bitmap ensureSize(Bitmap bitmap, int maxWidth, int maxHeight)
	{
		if (bitmap.getWidth() > maxWidth || bitmap.getHeight() > maxHeight)
			return Bitmap.createScaledBitmap(bitmap, maxWidth, maxHeight, true);
		else
			return bitmap;
	}

	public final static byte[] getBytes(Bitmap bitmap)
	{
		ByteArrayOutputStream strm = null;
		try
		{
			strm = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.PNG, 0, strm);
			return strm.toByteArray();
		}
		finally
		{
			if (strm != null)
			{
				try
				{
					strm.close();
				}
				catch (IOException ex) {}
			}
		}
	}

	public final static String getBase64(Bitmap bitmap)
	{
		return Base64.encodeToString(getBytes(bitmap), Base64.NO_WRAP);
	}

	public final static String getDataUri(Bitmap bitmap)
	{
		return "data:image/png;base64," + getBase64(bitmap);
	}
}
