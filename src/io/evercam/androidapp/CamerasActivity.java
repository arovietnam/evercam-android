package io.evercam.androidapp;

import java.util.List;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.os.Bundle;
import android.os.Handler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.*;

import io.evercam.androidapp.custom.AboutDialog;
import io.evercam.androidapp.custom.CameraLayout;
import io.evercam.androidapp.dal.DbAppUser;
import io.evercam.androidapp.dal.DbCamera;
import io.evercam.androidapp.dto.AppUser;
import io.evercam.androidapp.dto.EvercamCamera;
import io.evercam.androidapp.dto.ImageLoadingStatus;
import io.evercam.androidapp.slidemenu.*;
import io.evercam.androidapp.tasks.LoadCameraListTask;
import io.evercam.androidapp.utils.AppData;
import io.evercam.androidapp.utils.Constants;
import io.evercam.androidapp.utils.PrefsManager;
import io.evercam.androidapp.utils.UIUtils;

import com.bugsense.trace.BugSenseHandler;
import io.evercam.androidapp.R;
import com.google.analytics.tracking.android.EasyTracker;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

public class CamerasActivity extends ParentActivity implements
		SlideMenuInterface.OnSlideMenuItemClickListener
{
	public static CamerasActivity activity = null;
	public MenuItem refresh;

	private static final String TAG = "evercamapp-CamerasActivity";

	private SlideMenu slideMenu;
	private int totalCamerasInGrid = 0;
	private int slideoutMenuAnimationTime = 255;
	private boolean isUsersAccountsActivityStarted = false;
	private static int camerasPerRow = 2;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (Constants.isAppTrackingEnabled)
		{
			BugSenseHandler.initAndStartSession(this, Constants.bugsense_ApiKey);
		}

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		try
		{
			if (this.getActionBar() != null)
			{
				this.getActionBar().setHomeButtonEnabled(true);
				this.getActionBar().setDisplayShowTitleEnabled(false);
				this.getActionBar().setIcon(R.drawable.ic_device_access_storage);
			}

			setContentView(R.layout.camslayoutwithslide);

			addUsersToDropdownActionBar();

			slideMenu = (SlideMenu) findViewById(R.id.slideMenu);
			slideMenu.init(this, R.menu.slide, this, slideoutMenuAnimationTime);

			int notificationID = 0;
			try
			{
				activity = this;

				notificationID = this.getIntent().getIntExtra(Constants.GCMNotificationIDString, 0);
				this.getIntent().putExtra(Constants.GCMNotificationIDString, 0);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			if (notificationID > 0)
			{
				CamerasActivity.this.onSlideMenuItemClick(notificationID);
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, e.toString(), e);
			UIUtils.getAlertDialog(
					CamerasActivity.this,
					"Error Occured",
					Constants.ErrorMessageGeneric + e.toString() + "::"
							+ Log.getStackTraceString(e)).show();
			if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		try
		{
			// draw the options defined in the following file
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.camsmenulayout, menu);

			refresh = menu.findItem(R.id.menurefresh);

			if (refresh != null
					&& (AppData.evercamCameraList == null || AppData.evercamCameraList.size() == 0))
			{
				refresh.setActionView(null);
				refresh.setActionView(R.layout.actionbar_indeterminate_progress);
			}
			return true;
		}
		catch (Exception ex)
		{
			Log.e(TAG, ex.toString());
			if (Constants.isAppTrackingEnabled)
			{
				BugSenseHandler.sendException(ex);
			}
		}
		return true;
	}

	// Tells that the item has been selected from the menu. Now check and get
	// the selected item and perform the relevent action
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		try
		{
			switch (item.getItemId())
			{
			case R.id.menurefresh: // need to refresh the application
				if (refresh != null) refresh
						.setActionView(R.layout.actionbar_indeterminate_progress);

				LoadCameraListTask loadTask = new LoadCameraListTask(AppData.defaultUser,
						CamerasActivity.this);
				loadTask.reload = true; // be default do not refesh until there
										// is
				// any change in cameras in database
				loadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

				return true;

			case android.R.id.home:
				slideMenu.show();

			default:
				return super.onOptionsItemSelected(item);
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, e.toString(), e);
			UIUtils.getAlertDialog(
					this,
					"Error Occured",
					"Some error occured while saving your options. Technical details are: "
							+ e.toString());
			if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);
		}
		return super.onOptionsItemSelected(item);

	}

	@Override
	public void onSlideMenuItemClick(int itemId)
	{
		try
		{
			if (slideMenu.isShown())
			{
				slideMenu.hide();
			}

			switch (itemId)
			{
			case R.id.slidemenu_logout:
				logOutUser();

				break;

			case R.id.slidemenu_about:

				new Handler().postDelayed(new Runnable(){
					@Override
					public void run()
					{
						startActivity(new Intent(CamerasActivity.this, AboutDialog.class));
					}
				}, slideoutMenuAnimationTime);

				break;

			case R.id.slidemenu_settings:

				new Handler().postDelayed(new Runnable(){
					@Override
					public void run()
					{
						startActivity(new Intent(CamerasActivity.this, CameraPrefsActivity.class));
					}
				}, slideoutMenuAnimationTime);

				break;

			case R.id.slidemenu_manage:

				new Handler().postDelayed(new Runnable(){
					@Override
					public void run()
					{
						startActivity(new Intent(CamerasActivity.this, ManageAccountsActivity.class));
						isUsersAccountsActivityStarted = true;
					}
				}, slideoutMenuAnimationTime);
				break;

			// default: // starting the notification activity
			//
			// NotificationActivity.NotificationID = itemId;
			// new
			// MarkNotificationAsReadTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
			// itemId + "");
			// new Handler().postDelayed(new Runnable(){
			// @Override
			// public void run()
			// {
			// Intent i = new Intent(new Intent(CamerasActivity.this,
			// NotificationActivity.class));
			// startActivity(i);
			// }
			// }, slideoutMenuAnimationTime);
			// break;

			}
		}
		catch (Exception e)
		{
			Log.e(TAG, e.toString(), e);
			UIUtils.getAlertDialog(
					this,
					"Error Occured",
					"Some error occured while saving your options. Technical details are: "
							+ e.toString());
		}
	}

	@Override
	public void onRestart()
	{
		super.onRestart();
		try
		{
			if (isUsersAccountsActivityStarted)
			{
				isUsersAccountsActivityStarted = false;
				addUsersToDropdownActionBar();
			}

			int camsOldValue = camerasPerRow;
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			camerasPerRow = PrefsManager.getCameraPerRow(sharedPrefs, 2);

			if (camsOldValue != camerasPerRow)
			{
				removeAllCameraViews();
				addAllCameraViews(false); // do not reload cameras beacuse it
											// may be an activity for orentation
											// changed or notification might
											// have arrived.
			}

		}
		catch (Exception e)
		{
			if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);
		}
	}

	@Override
	public void onBackPressed()
	{

	}

	private void addUsersToDropdownActionBar()
	{
		boolean taskStarted = false;

		while (!taskStarted)
		{
			try
			{
				stopAllCameraViews();

				if (AppData.defaultUser == null)
				{
					startActivity(new Intent(this, MainActivity.class));
					CamerasActivity.this.finish();
					return;
				}

				new UserLoadingTask().execute();

				taskStarted = true;
			}
			catch (Exception e)
			{
				Log.e(TAG, e.getMessage(), e);
				if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);
			}
		}

	}

	// Stop All Camera Views
	private void stopAllCameraViews()
	{
		io.evercam.androidapp.custom.FlowLayout camsLineView = (io.evercam.androidapp.custom.FlowLayout) this
				.findViewById(R.id.camsLV);
		for (int count = 0; count < camsLineView.getChildCount(); count++)
		{
			LinearLayout linearLayout = (LinearLayout) camsLineView.getChildAt(count);
			CameraLayout cameraLayout = (CameraLayout) linearLayout.getChildAt(0);
			cameraLayout.stopAllActivity();
		}
	}

	boolean resizeCameras()
	{
		try
		{
			Display display = getWindowManager().getDefaultDisplay();
			int screen_width = display.getWidth();

			io.evercam.androidapp.custom.FlowLayout camsLineView = (io.evercam.androidapp.custom.FlowLayout) this
					.findViewById(R.id.camsLV);
			for (int i = 0; i < camsLineView.getChildCount(); i++)
			{
				LinearLayout pview = (LinearLayout) camsLineView.getChildAt(i);
				CameraLayout cameraLayout = (CameraLayout) pview.getChildAt(0); // CameraLayout
				// is on
				// 0th
				// index

				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
						android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
						android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
				params.width = ((i + 1 % camerasPerRow == 0) ? (screen_width - (i % camerasPerRow)
						* (screen_width / camerasPerRow)) : screen_width / camerasPerRow);
				params.height = (int) (params.width / (1.25));
				cameraLayout.setLayoutParams(params);
			}
			return true;
		}
		catch (Exception e)
		{
			Log.e(TAG, e.toString() + "::" + Log.getStackTraceString(e));
			UIUtils.getAlertDialog(CamerasActivity.this, "Error Occured",
					Constants.ErrorMessageGeneric).show();
			if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);

		}
		return false;
	}

	// Remove all the cameras so that all activities being performed can be
	// stopped
	public boolean removeAllCameraViews()
	{
		try
		{
			stopAllCameraViews();

			io.evercam.androidapp.custom.FlowLayout camsLineView = (io.evercam.androidapp.custom.FlowLayout) this
					.findViewById(R.id.camsLV);
			camsLineView.removeAllViews();

			totalCamerasInGrid = 0;

			return true;
		}
		catch (Exception e)
		{
			Log.e(TAG, e.toString() + "::" + Log.getStackTraceString(e));
			UIUtils.getAlertDialog(CamerasActivity.this, "Error Occured",
					Constants.ErrorMessageGeneric).show();
			if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);

		}
		return false;
	}

	// Add all the cameras as per the rules
	public boolean addAllCameraViews(boolean reloadImages)
	{
		try
		{
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			camerasPerRow = PrefsManager.getCameraPerRow(sharedPrefs, camerasPerRow);

			io.evercam.androidapp.custom.FlowLayout camsLineView = (io.evercam.androidapp.custom.FlowLayout) this
					.findViewById(R.id.camsLV);

			Display display = getWindowManager().getDefaultDisplay();
			int screen_width = display.getWidth();

			int index = 0;
			totalCamerasInGrid = 0;

			for (EvercamCamera evercamCamera : AppData.evercamCameraList)
			{
				LinearLayout cameraListLayout = new LinearLayout(this);

				int indexPlus = index + 1;

				if (reloadImages) evercamCamera.loadingStatus = ImageLoadingStatus.not_started;
				CameraLayout cameraLayout = new CameraLayout(this, evercamCamera);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
						android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
						android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
				params.width = ((indexPlus % camerasPerRow == 0) ? (screen_width - (index % camerasPerRow)
						* (screen_width / camerasPerRow))
						: screen_width / camerasPerRow);
				params.height = (int) (params.width / (1.25));
				cameraLayout.setLayoutParams(params);

				cameraListLayout.addView(cameraLayout);
				camsLineView.addView(cameraListLayout,
						new io.evercam.androidapp.custom.FlowLayout.LayoutParams(0, 0));
				index++;
				totalCamerasInGrid++;
			}

			if (this.getActionBar() != null) this.getActionBar().setHomeButtonEnabled(true);

			// startgCMRegisterActions();

			if (refresh != null) refresh.setActionView(null);

			return true;
		}
		catch (Exception e)
		{
			Log.e(TAG, e.toString(), e);
			UIUtils.getAlertDialog(CamerasActivity.this, "Error Occured",
					Constants.ErrorMessageGeneric).show();
			if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);

		}
		return false;
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		try
		{
			stopGcmRegisterActions();
			removeAllCameraViews();
		}
		catch (Exception e)
		{
			Log.e(TAG, e.toString(), e);
		}
	}

	boolean mHandleMessageReceiverRegistered = false;

	//
	// private final void startgCMRegisterActions()
	// {
	// try
	// {
	// RegisterTask = new RegisterGCMAlertsServiceTask();
	// RegisterTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
	//
	// registerReceiver(mHandleMessageReceiver, new
	// IntentFilter("CambaGCMAlert"));
	//
	// mHandleMessageReceiverRegistered = true;
	//
	// Log.i(TAG,
	// "registerReceiver(mHandleMessageReceiver,new IntentFilter(\"CambaGCMAlert\"));");
	// }
	// catch (Exception e)
	// {
	// if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);
	// }
	//
	// }

	private final void stopGcmRegisterActions()
	{

		Log.i(TAG, "StopGcmRegisterActions called");

		if (mHandleMessageReceiverRegistered) // unregister only if registered
												// otherwise
												// illegalArgumentException
		unregisterReceiver(mHandleMessageReceiver);

	}

	private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent)
		{

			Log.i(TAG, "AlertMessage Received ");

			String AlertMessage = intent.getStringExtra("AlertMessage");
			Log.i(TAG, "AlertMessage [" + AlertMessage + "]");

			String ApiCamera = intent.getStringExtra("ApiCamera");
			Log.i(TAG, "ApiCamera [" + ApiCamera + "]");
		}
	};

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		try
		{
			super.onConfigurationChanged(newConfig);

			resizeCameras();

		}
		catch (Exception e)
		{
			if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);
		}
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

	private void logOutUser()
	{
		try
		{
			// delete saved username and password
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			PrefsManager.removeUserEmail(sharedPrefs);

			// clear realtime default app data
			AppData.defaultUser = null;
			AppData.evercamCameraList.clear();

			// delete app user
			DbAppUser dbUser = new DbAppUser(this);
			List<AppUser> list = dbUser.getAllAppUsers(10000);

			if (list != null && list.size() > 0)
			{
				for (AppUser user : list)
				{
					dbUser.deleteAppUserByEmail(user.getEmail());
				}
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "Error: delete user" + e.toString());
		}
		startActivity(new Intent(this, MainActivity.class));
	}

	private class UserLoadingTask extends AsyncTask<String, String, String[]>
	{
		int defaultUserIndex = 0;

		@Override
		protected String[] doInBackground(String... arg0)
		{
			try
			{
				DbAppUser dbUser = new DbAppUser(CamerasActivity.this);
				AppData.appUsers = dbUser.getAllAppUsers(1000);

				final String[] userEmailArray = new String[AppData.appUsers.size()];

				for (int count = 0; count < AppData.appUsers.size(); count++)
				{
					userEmailArray[count] = AppData.appUsers.get(count).getEmail();
					if (AppData.appUsers.get(count).getIsDefault())
					{
						defaultUserIndex = count;
					}
				}

				return userEmailArray;

			}
			catch (Exception e)
			{
				Log.e(TAG, e.getMessage(), e);
				if (Constants.isAppTrackingEnabled)
				{
					BugSenseHandler.sendException(e);
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(String[] userEmailArray)
		{
			try
			{
				ArrayAdapter<String> dropdownListAdapter = new ArrayAdapter<String>(
						CamerasActivity.this, android.R.layout.simple_spinner_dropdown_item,
						userEmailArray);
				CamerasActivity.this.getActionBar().setNavigationMode(
						ActionBar.NAVIGATION_MODE_LIST);
				OnNavigationListener navigationListener = new OnNavigationListener(){
					@Override
					public boolean onNavigationItemSelected(int itemPosition, long itemId)
					{
						try
						{
							// set all current users default to false
							for (AppUser user : AppData.appUsers)
							{
								user.setIsDefault(false);
							}

							// set all db app users as false
							DbAppUser dbUser = new DbAppUser(CamerasActivity.this);
							dbUser.updateAllIsDefaultFalse();

							// set selected user's default to true
							AppUser user = AppData.appUsers.get(itemPosition);
							user.setIsDefault(true);

							dbUser.updateAppUser(user);
							AppData.defaultUser = user;

							// load local cameras for default user
							AppData.evercamCameraList = new DbCamera(CamerasActivity.this)
									.getCamerasByOwner(user.getUsername(), 500);

							removeAllCameraViews();
							addAllCameraViews(true);

							// start the task for default user to refresh camera
							// list
							new LoadCameraListTask(AppData.defaultUser, CamerasActivity.this)
									.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
							if (totalCamerasInGrid == 0 && refresh != null)
							{
								refresh.setActionView(null);
								refresh.setActionView(R.layout.actionbar_indeterminate_progress);
							}
						}
						catch (Exception e)
						{
							Log.e(TAG, e.getMessage(), e);
							if (Constants.isAppTrackingEnabled)
							{
								BugSenseHandler.sendException(e);
							}
						}
						return false;
					}
				};

				getActionBar().setListNavigationCallbacks(dropdownListAdapter, navigationListener);
				getActionBar().setSelectedNavigationItem(defaultUserIndex);

			}
			catch (Exception e)
			{
				Log.e(TAG, e.getMessage(), e);
				if (Constants.isAppTrackingEnabled) BugSenseHandler.sendException(e);
			}
		}
	}
}