package com.maps.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.maps.beans.GooglePlace;
import com.maps.config.Config;

public class Utils {

	public static ArrayList<GooglePlace> autocomplete(Context context,
			String apiKey, String input) {

		String LOG_TAG = "ExampleApp";
		ArrayList<GooglePlace> resultList = null;
		HttpURLConnection conn = null;
		StringBuilder jsonResults = new StringBuilder();
		String countryCode = getUserCountry(context);

		if (countryCode == null || countryCode.equals(""))
			countryCode = "us";

		try {
			StringBuilder sb = new StringBuilder(Config.GOOGLE_PLACES_API_BASE
					+ Config.TYPE_AUTOCOMPLETE + Config.OUT_JSON);
			sb.append("?sensor=false&key=" + apiKey);
			sb.append("&components=country:" + countryCode);
			sb.append("&input=" + URLEncoder.encode(input, "utf8"));

			URL url = new URL(sb.toString());
			conn = (HttpURLConnection) url.openConnection();
			InputStreamReader in = new InputStreamReader(conn.getInputStream());

			// Load the results into a StringBuilder
			int read;
			char[] buff = new char[1024];
			while ((read = in.read(buff)) != -1) {
				jsonResults.append(buff, 0, read);
			}
		} catch (MalformedURLException e) {
			Log.e(LOG_TAG, "Error processing Places API URL", e);
			return resultList;
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error connecting to Places API", e);
			return resultList;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		try {
			JSONObject jsonObj = new JSONObject(jsonResults.toString());
			JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");
			GooglePlace place;

			resultList = new ArrayList<GooglePlace>(predsJsonArray.length());
			for (int i = 0; i < predsJsonArray.length(); i++) {
				place = new GooglePlace();
				place.setpDescription(predsJsonArray.getJSONObject(i)
						.getString("description"));
				place.setpPlaceId(predsJsonArray.getJSONObject(i).getString(
						"place_id"));

				resultList.add(place);
			}
		} catch (JSONException e) {
			Log.e(LOG_TAG, "Cannot process JSON results", e);
		}

		return resultList;
	}

	public static LatLng getLocation(Context context, String apiKey,
			String placeId) {
		LatLng location = null;
		String LOG_TAG = "ExampleApp";
		HttpURLConnection conn = null;
		StringBuilder jsonResults = new StringBuilder();

		try {
			StringBuilder sb = new StringBuilder(Config.GOOGLE_PLACES_API_BASE
					+ Config.TYPE_DETAILS + Config.OUT_JSON);
			sb.append("?sensor=false&key=" + apiKey);
			sb.append("&placeid=" + URLEncoder.encode(placeId, "utf8"));

			URL url = new URL(sb.toString());
			conn = (HttpURLConnection) url.openConnection();
			InputStreamReader in = new InputStreamReader(conn.getInputStream());

			int read;
			char[] buff = new char[1024];
			while ((read = in.read(buff)) != -1) {
				jsonResults.append(buff, 0, read);
			}
		} catch (MalformedURLException e) {
			Log.e(LOG_TAG, "Error processing Places API URL", e);
			return location;
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error connecting to Places API", e);
			return location;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		try {
			JSONObject json = new JSONObject(jsonResults.toString())
					.getJSONObject("result").getJSONObject("geometry")
					.getJSONObject("location");
			location = new LatLng(json.getDouble("lat"), json.getDouble("lng"));
		} catch (JSONException e) {
			Log.e(LOG_TAG, "Cannot process JSON results", e);
		}

		return location;
	}

	public static String getUserCountry(Context context) {
		try {
			final TelephonyManager tm = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);
			final String simCountry = tm.getSimCountryIso();

			if (simCountry != null && simCountry.length() == 2) {
				// SIM country code is available
				return simCountry.toLowerCase(Locale.US);
			} else if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) {
				// device is not 3G (would be unreliable)
				String networkCountry = tm.getNetworkCountryIso();
				if (networkCountry != null && networkCountry.length() == 2) {
					// network country code is available
					return networkCountry.toLowerCase(Locale.US);
				}
			}
		} catch (Exception e) {
		}

		return null;
	}
}
