package com.maps.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.maps.beans.GooglePlace;
import com.maps.screens.R;
import com.maps.utils.Utils;

public class PlacesAutoCompleteAdapter extends ArrayAdapter<GooglePlace>
		implements Filterable {
	LayoutInflater inflater;
	ArrayList<GooglePlace> resultList;

	public PlacesAutoCompleteAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);

		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	private static class Holder {
		TextView description;
	}

	@Override
	public View getView(int position, View childView, ViewGroup parent) {
		Holder holder;
		GooglePlace place;

		if (childView == null || !(childView.getTag() instanceof Holder)) {
			childView = inflater.inflate(R.layout.list_item, parent, false);
			holder = new Holder();
			holder.description = (TextView) childView
					.findViewById(R.id.description);
			childView.setTag(holder);
		} else {
			holder = (Holder) childView.getTag();
		}

		place = getItem(position);
		holder.description.setText(place.getpDescription());

		return childView;
	}

	@Override
	public int getCount() {
		return resultList.size();
	}

	@Override
	public GooglePlace getItem(int index) {
		return resultList.get(index);
	}

	@Override
	public Filter getFilter() {
		Filter filter = new Filter() {
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults filterResults = new FilterResults();

				if (constraint != null) {
					resultList = Utils.autocomplete(getContext(), getContext()
							.getString(R.string.api_server_key), constraint
							.toString());

					filterResults.values = resultList;
					filterResults.count = resultList.size();
				}

				return filterResults;
			}

			@Override
			protected void publishResults(CharSequence constraint,
					FilterResults results) {
				if (results != null && results.count > 0) {
					notifyDataSetChanged();
				} else {
					notifyDataSetInvalidated();
				}
			}
		};
		return filter;
	}
}
