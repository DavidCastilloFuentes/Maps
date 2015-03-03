package com.maps.gps;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.maps.interfaces.LocationChanged;

public class GPSTracker implements LocationListener {
	public static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
	public static final long MIN_TIME_BW_UPDATES = 1000 * 10;
	boolean isNetworkProviderTrackEnabled = false;
	boolean isGPSProviderTrackEnabled = false;
	boolean isNetworkEnabled = false;
	boolean isGPSEnabled = false;
	Context mContext;
	Location location;
	LocationChanged mHandler;
	LocationManager locationManager;

	public GPSTracker(Context context, LocationChanged handler) {
		mContext = context;
		mHandler = handler;
		isNetworkProviderTrackEnabled = false;
		isGPSProviderTrackEnabled = false;
	}

	public Location getLocation() {
		try {
			locationManager = (LocationManager) mContext
					.getSystemService(Context.LOCATION_SERVICE);

			// getting GPS status
			isGPSEnabled = locationManager
					.isProviderEnabled(LocationManager.GPS_PROVIDER);

			Log.v("GPS:: isGPSEnabled", "=" + isGPSEnabled);

			// getting network status
			isNetworkEnabled = locationManager
					.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

			Log.v("NETWORK:: isNetworkEnabled", "=" + isNetworkEnabled);

			if (isNetworkEnabled) {
				if (!isNetworkProviderTrackEnabled) {
					isNetworkProviderTrackEnabled = true;
					locationManager.requestLocationUpdates(
							LocationManager.NETWORK_PROVIDER,
							MIN_TIME_BW_UPDATES,
							MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
				}

				location = locationManager
						.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			}

			// if GPS Enabled get lat/long using GPS Services
			if (isGPSEnabled) {
				if (location == null) {
					if (!isGPSProviderTrackEnabled) {
						isGPSProviderTrackEnabled = true;
						locationManager.requestLocationUpdates(
								LocationManager.GPS_PROVIDER,
								MIN_TIME_BW_UPDATES,
								MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
					}

					Log.d("GPS Enabled", "GPS Enabled");
					location = locationManager
							.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				}
			}
		} catch (Exception e) {
			Log.i("Erorr", "Errror while loading location(" + e + ")");
		}

		if (location == null) {
			location = new Location("");
			location = new Location("reverseGeocoded");
			location.setLatitude(33.893546);
			location.setLongitude(-84.457123);
			location.setAltitude(0);
		}

		return location;
	}

	public void stopUsingGPS() {
		if (locationManager != null)
			locationManager.removeUpdates(this);

		isGPSProviderTrackEnabled = false;
		isNetworkProviderTrackEnabled = false;
	}

	@Override
	public void onLocationChanged(Location location) {
		if (mHandler != null)
			mHandler.onLocationChanged(location);
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
}
