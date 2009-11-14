package com.atteo.langleo_trial.activities;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.atteo.langleo_trial.R;
import com.atteo.langleo_trial.models.Collection;
import com.atteo.langleo_trial.models.List;
import com.atteo.silo.StorableCollection;

public class SelectList extends ListActivity {
	private ArrayList<HashMap<String, String>> datalist;
	private SimpleAdapter adapter;

	private Collection collection;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.select_list);
		ListView list = getListView();
		list.setItemsCanFocus(false);
		list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		Intent intent = getIntent();
		collection = new Collection();
		collection.loadBundle(intent.getBundleExtra("collection"));
		collection.load();

		Button button = (Button) findViewById(R.id.select_file_ok);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				ListView list = getListView();
				ArrayList<Integer> lists = new ArrayList<Integer>();
				int len = list.getCount();
				
				boolean found = false;
				for (int i = 0; i < len; i++)
					if (list.isItemChecked(i)) {
						lists.add(Integer.valueOf(datalist.get(i).get("id")));
						found = true;
					}
				if (!found)
					return;
				int array[] = new int[lists.size()];
				len = lists.size();
				for (int i = 0; i < len; i++)
					array[i] = lists.get(i);
				intent.putExtra("lists", array);
				setResult(RESULT_OK, intent);
				finish();
			}

		});

		button = (Button) findViewById(R.id.select_file_select_all);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ListView l = getListView();
				int len = l.getCount();
				for (int i = 0; i < len; i++) {
					l.setItemChecked(i, true);
				}
			}
		});

		button = (Button) findViewById(R.id.select_file_deselect_all);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ListView l = getListView();
				int len = l.getCount();
				for (int i = 0; i < len; i++) {
					l.setItemChecked(i, false);
				}
			}

		});

		refreshList();
	}

	public void refreshList() {
		datalist = new ArrayList<HashMap<String, String>>();

		StorableCollection lists = collection.getLists();

		lists.orderByInPlace("name");
		lists.iterate();
		List l;

		while ((l = lists.next()) != null) {
			HashMap<String, String> map = new HashMap<String, String>();
			map.put("id", String.valueOf(l.getId()));
			map.put("name", l.getName());
			datalist.add(map);
		}

		ListView list = getListView();

		adapter = new SimpleAdapter(this, datalist, R.layout.select_list_item,
				new String[] { "name" },
				new int[] { R.id.select_list_item_name });
		adapter.setViewBinder(new myBinder());
		list.setAdapter(adapter);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.select_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {

		switch (menuItem.getItemId()) {
		case R.id.select_list_help:
			showHelp();
			break;
		}
		return true;
	}

	private void showHelp() {
		Intent intent = new Intent(this, Help.class);
		intent.putExtra("part", "select_list");
		startActivity(intent);
	}
	
	private class myBinder implements SimpleAdapter.ViewBinder {

		@Override
		public boolean setViewValue(View view, Object data,
				String textRepresentation) {
			((CheckedTextView)view).setText(textRepresentation);
			return true;
		}
		
	}

}
