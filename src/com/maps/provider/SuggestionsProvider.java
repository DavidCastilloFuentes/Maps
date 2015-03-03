package com.maps.provider;

import android.app.SearchManager;
import android.content.SearchRecentSuggestionsProvider;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class SuggestionsProvider extends SearchRecentSuggestionsProvider {
	public static final String AUTHORITY = "com.maps.provider.SuggestionsProvider";
	public static final int MODE = DATABASE_MODE_QUERIES;

	public SuggestionsProvider() {
		setupSuggestions(AUTHORITY, MODE);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		String query = uri.getLastPathSegment();

		Log.i("SEARCH", "Query: " + query);

		if (SearchManager.SUGGEST_URI_PATH_QUERY.equals(query)) {
			// user hasn't entered anything
			// thus return a default cursor
		} else {
			// query contains the users search
			// return a cursor with appropriate data
		}

		return null;
	}
}
