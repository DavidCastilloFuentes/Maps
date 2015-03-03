package com.maps.screens;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.SearchView.SearchAutoComplete;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.maps.adapters.PlacesAutoCompleteAdapter;
import com.maps.beans.GooglePlace;
import com.maps.gps.GPSTracker;
import com.maps.interfaces.LocationChanged;
import com.maps.utils.Utils;

public class Main extends ActionBarActivity implements OnClickListener,
		LocationChanged, OnItemClickListener {
	LatLng mPosition;
	GoogleMap mMap;
	Marker mCurrent;
	GPSTracker mGPS;
	WakeLock mWakeLock;
	boolean mCenterOnLocation;
	LocationManager mLocationManager;
	PlacesAutoCompleteAdapter autoComplete;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		getSupportActionBar().setDisplayShowHomeEnabled(false);
		getSupportActionBar().setDisplayShowTitleEnabled(false);

		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "PMM");
		autoComplete = new PlacesAutoCompleteAdapter(this, R.layout.list_item);
	}

	@Override
	public void onResume() {
		super.onResume();
		mWakeLock.acquire();
		openGPS();
	}

	@Override
	public void onPause() {
		super.onPause();

		if (canToggleGPS())
			turnGPSOff();

		cancelTracker();

		mWakeLock.release();
	}

	protected void openGPS() {

		if (!isEnabledLocationFixWithProvider(LocationManager.GPS_PROVIDER)
				&& !isEnabledLocationFixWithProvider(LocationManager.NETWORK_PROVIDER)) {
			if (canToggleGPS()) {
				turnGPSOn();
			} else {
				showMessageNoGPS();
				return;
			}
		}

		mCenterOnLocation = true;

		centerOnLocation(getGPSLocation(), new CancelableCallback() {
			@Override
			public void onFinish() {

			}

			@Override
			public void onCancel() {
			}
		});
	}

	protected LatLng getGPSLocation() {
		if (mGPS == null)
			mGPS = new GPSTracker(this, this);

		Location loc = mGPS.getLocation();

		mPosition = new LatLng(loc.getLatitude(), loc.getLongitude());

		getMap();

		if (mMap != null) {
			if (mCurrent == null) {
				mCurrent = mMap.addMarker(new MarkerOptions()
						.icon(BitmapDescriptorFactory
								.fromResource(R.drawable.ic_launcher))
						.position(mPosition).title("Here we are!"));
			} else {
				mCurrent.setPosition(mPosition);
			}
		}

		return mPosition;
	}

	public void centerOnLocation(LatLng position,
			GoogleMap.CancelableCallback callback) {

		if (mCenterOnLocation && mMap != null) {
			mCenterOnLocation = false;
			mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 12),
					1500, callback);
		}
	}

	protected GoogleMap getMap() {
		if (mMap == null)
			mMap = ((com.google.android.gms.maps.SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map)).getMap();
		return mMap;
	}

	protected void loadScreenLocation(String title, double latitude,
			double longitude) {

		getMap();

		if (mMap == null) {
			Toast.makeText(this, "No Maps support is available on this device",
					Toast.LENGTH_LONG).show();
			return;
		}

		mPosition = new LatLng(latitude, longitude);

		CameraPosition cameraPosition = new CameraPosition.Builder()

		/* Sets the location */
		.target(mPosition)

		/* Sets the zoom */
		.zoom(14)

		/* Sets the orientation of the camera to east */
		.bearing(90)

		/* Sets the tilt of the camera to 30 degrees */
		.tilt(30)

		/* Creates a CameraPosition from the builder */
		.build();

		mMap.animateCamera(CameraUpdateFactory
				.newCameraPosition(cameraPosition));

		if (mCurrent != null)
			mCurrent.remove();

		mCurrent = mMap.addMarker(new MarkerOptions()
				.position(mPosition)
				.title(title)
				.icon(BitmapDescriptorFactory
						.fromResource(R.drawable.ic_launcher)));
	}

	private boolean canToggleGPS() {
		PackageManager pacman = getPackageManager();
		PackageInfo pacInfo = null;

		try {
			pacInfo = pacman.getPackageInfo("com.android.settings",
					PackageManager.GET_RECEIVERS);
		} catch (NameNotFoundException e) {
			return false; // package not found
		}

		if (pacInfo != null) {
			for (ActivityInfo actInfo : pacInfo.receivers) {
				// test if recevier is exported. if so, we can toggle GPS.
				if (actInfo.name
						.equals("com.android.settings.widget.SettingsAppWidgetProvider")
						&& actInfo.exported) {
					return true;
				}
			}
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	private void turnGPSOn() {
		String provider = Settings.Secure.getString(this.getContentResolver(),
				Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

		Log.i("Main", "PRENDERÉ EL GPS.");

		if (!provider.contains("gps")) {
			final Intent poke = new Intent();
			poke.setClassName("com.android.settings",
					"com.android.settings.widget.SettingsAppWidgetProvider");
			poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
			poke.setData(Uri.parse("3"));

			sendBroadcast(poke);
		}
	}

	@SuppressWarnings("deprecation")
	private void turnGPSOff() {
		String provider = Settings.Secure.getString(this.getContentResolver(),
				Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
		Log.i("Main", "APAGARE EL GPS.");
		if (provider.contains("gps")) {
			final Intent poke = new Intent();
			poke.setClassName("com.android.settings",
					"com.android.settings.widget.SettingsAppWidgetProvider");
			poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
			poke.setData(Uri.parse("3"));
			sendBroadcast(poke);
		}
	}

	private void showMessageNoGPS() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.activate_geolocation))
				.setCancelable(false)
				.setPositiveButton(getString(R.string._yes_),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog,
									final int id) {
								Intent intent = new Intent(
										Settings.ACTION_LOCATION_SOURCE_SETTINGS);
								startActivity(intent);
								dialog.dismiss();
							}
						})
				.setNegativeButton(getString(R.string._no_),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog,
									final int id) {
								dialog.cancel();
							}
						});

		AlertDialog alert = builder.create();
		alert.show();
	}

	public void cancelTracker() {
		if (mGPS != null)
			mGPS.stopUsingGPS();
	}

	@Override
	public void onLocationChanged(Location location) {
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);

		MenuItem item = menu.findItem(R.id.action_search);
		SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

		if (searchView != null) {
			SearchAutoComplete autoCompleteTextView = (SearchAutoComplete) searchView
					.findViewById(R.id.search_src_text);
			if (autoCompleteTextView != null) {
				autoCompleteTextView.setAdapter(autoComplete);
				autoCompleteTextView.setOnItemClickListener(this);
			}

			searchView.setOnQueryTextListener(new OnQueryTextListener() {
				@Override
				public boolean onQueryTextSubmit(String input) {
					return false;
				}

				@Override
				public boolean onQueryTextChange(String input) {
					autoComplete.getFilter().filter(input);
					return false;
				}
			});
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_exit) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	protected boolean isEnabledLocationFixWithProvider(String gpsProvider) {
		return mLocationManager.isProviderEnabled(gpsProvider);
	}

	@Override
	public void onClick(View v) {
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int position,
			long id) {
		GooglePlace place = autoComplete.getItem(position);
		loadLocation(place.getpDescription(), this, place.getpPlaceId());
	}

	protected void loadLocation(final String title, final Context context,
			String reference) {
		AsyncTask<String, Void, LatLng> task = new AsyncTask<String, Void, LatLng>() {

			@Override
			protected LatLng doInBackground(String... location) {
				return Utils
						.getLocation(context,
								context.getString(R.string.api_server_key),
								location[0]);
			}

			@Override
			protected void onPostExecute(LatLng location) {
				if (location != null)
					loadScreenLocation(title, location.latitude,
							location.longitude);
			}
		};
		task.execute(reference);
	}
}
