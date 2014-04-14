package io.evercam.android;

import io.evercam.android.utils.AppData;
import io.evercam.android.utils.Commons;
import io.evercam.android.utils.Constants;
import io.evercam.android.utils.UIUtils;

import com.bugsense.trace.BugSenseHandler;
import io.evercam.android.R;
import com.google.analytics.tracking.android.EasyTracker;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

/*
 * Main starting activity. 
 * Checks whether user should login first or load the cameras straight away
 * */
public class MainActivity extends Activity
{
	private static final String TAG = "evercamapp";
	private static boolean enableLogs = true;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		try
		{
			super.onCreate(savedInstanceState);

			if (Constants.isAppTrackingEnabled)
			{
				BugSenseHandler.initAndStartSession(this, Constants.bugsense_ApiKey);
			}

			setContentView(R.layout.mainactivitylayout);

			if (isReleaseNotePageShowed())
			{
				startApplication();
			}
			else
			{
				Intent notesIntent = new Intent(MainActivity.this, ReleaseNotesActivity.class);
				startActivity(notesIntent);
				this.finish();
			}

		}
		catch (Exception ex)
		{
			UIUtils.GetAlertDialog(MainActivity.this, "Error Occured", ex.toString()).show();
			if (enableLogs) Log.i(TAG, Log.getStackTraceString(ex));
		}
	}

	private void startApplication()
	{
		try
		{
			if (!Commons.isOnline(this))
			{
				try
				{
					UIUtils.GetAlertDialog(MainActivity.this,
							getString(R.string.msg_network_not_connected),
							getString(R.string.msg_try_network_again),
							new DialogInterface.OnClickListener(){
								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									dialog.dismiss();
									MainActivity.this.finish();
								}
							}).show();
					return;
				}
				catch (Exception ex)
				{
					if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(ex);
				}
			}
			else
			{

				// get the username and password saved in application and pass
				// to
				// CambaApiManager so that they can be used at the time of login
				// authentication
			//	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

//				AppData.AppUserEmail = sharedPrefs.getString("AppUserEmail", null);
//				AppData.AppUserPassword = sharedPrefs.getString("AppUserPassword", null);
				// if username and password not found, pass the same to login
				// activity
//				if (AppData.AppUserEmail == null || AppData.AppUserEmail.equals("")
//						|| AppData.AppUserPassword == null || AppData.AppUserPassword.equals(""))
				if(AppData.defaultUser == null)
				{
					Intent login = new Intent(MainActivity.this, LoginActivity.class);
					startActivityForResult(login, LoginActivity.loginVerifyRequestCode);
				}
				else
				// username password found. pass to cams activity and verify if
				// the
				// username password is valid and get the cameras data
				{
					startCamerasActivity();
				}
			}
		}
		catch (Exception ex)
		{
			UIUtils.GetAlertDialog(MainActivity.this, "Error Occured", ex.toString()).show();
			if (enableLogs) Log.i(TAG, Log.getStackTraceString(ex));
		}

	}

	// login activity result. perform the acction according to result...
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		try
		{
			// Some simple checks to ensure that we are recieving results for
			// our desired intention and that it was successfull
			if (requestCode == LoginActivity.loginVerifyRequestCode
					&& resultCode == LoginActivity.loginResultSuccessCode)
			{
				startCamerasActivity();
			}
			else
			{
				MainActivity.this.finish();
			}
		}
		catch (Exception ex)
		{
			UIUtils.GetAlertDialog(MainActivity.this, "Error Occured", ex.toString()).show();
			if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(ex);
		}
	}

	private void startCamerasActivity()
	{
		int notificationID = 0;
		try
		{
			String strNotificationID = this.getIntent().getStringExtra(
					Constants.GCMNotificationIDString);

			if (strNotificationID != null && !strNotificationID.equals("")) notificationID = Integer
					.parseInt(strNotificationID);

			Log.i(TAG, "main activitiy strNotificationID [" + strNotificationID
					+ "], notificationID [" + notificationID + "]");

		}
		catch (Exception e)
		{
		}

		if (CamerasActivity.activity != null)
		{
			try
			{
				CamerasActivity.activity.finish();
			}
			catch (Exception e)
			{
				Log.e(TAG, e.toString(), e);
			}
		}

		Intent i = new Intent(this, CamerasActivity.class);
		i.putExtra(Constants.GCMNotificationIDString, notificationID);
		this.startActivity(i);

		MainActivity.this.finish();

	}

	@Override
	public void onStart()
	{
		super.onStart();

		if (Constants.isAppTrackingEnabled)
		{
			EasyTracker.getInstance().activityStart(this);
			if (Constants.isAppTrackingEnabled) BugSenseHandler.startSession(this);
		}
	}

	@Override
	public void onStop()
	{
		super.onStop();

		if (Constants.isAppTrackingEnabled)
		{
			EasyTracker.getInstance().activityStop(this);
			if (Constants.isAppTrackingEnabled) BugSenseHandler.closeSession(this);
		}
	}

	private boolean isReleaseNotePageShowed()
	{
		int versionCode = 0;
		boolean isReleaseNotesShown = false;
		try
		{
			PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			versionCode = packageInfo.versionCode;
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			isReleaseNotesShown = sharedPrefs.getBoolean(
					this.getString(R.string.is_release_notes_shown) + versionCode, false);
		}
		catch (NameNotFoundException e)
		{
			Log.e("evercamapp", e.getMessage());
		}
		return ((isReleaseNotesShown && versionCode != 0) ? true : false);
	}
}