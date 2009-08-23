package com.atteo.langleo.activities;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.atteo.langleo.Langleo;
import com.atteo.langleo.R;

public class SelectFile extends ListActivity {
	private ArrayList<HashMap<String, String>> datalist;
	private FileListAdapter adapter;

	private String currentDirectory;

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.select_list);
		ListView list = getListView();
		list.setItemsCanFocus(false);
		list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		if (savedInstanceState != null) {
			currentDirectory = savedInstanceState
					.getString("current_directory");
		} else {
			currentDirectory = Environment.getExternalStorageDirectory() + "/"
					+ Langleo.DIR_NAME;
		}

		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapter, View view,
					int position, long id) {
				if (!SelectFile.this.adapter.isDirectory(position))
					return;

				currentDirectory = new File(datalist.get(position).get(
						"fullpath")).getAbsolutePath();
				refreshList();
			}

		});

		Button button = (Button) findViewById(R.id.select_file_ok);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				ListView list = getListView();
				
				int len = list.getCount();
				boolean found = false;
				int type;
				HashMap<String, String> map;
				for (int i = 0; i < len; i++) {
					type = adapter.getItemViewType(i);
					if (type == adapter.VIEWTYPE_FILE && list.isItemChecked(i)) {
						found = true;
						map = datalist.get(i);
						Bundle b = new Bundle();
						b.putString("filename", map.get("filename"));
						b.putString("fullpath", map.get("fullpath"));
						intent.putExtra(map.get("filename"), b);
					}
						

				}
				if (!found)
					return;

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

	@Override
	protected void onSaveInstanceState(Bundle b) {
		b.putString("current_directory", currentDirectory);
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && datalist.size() > 0
				&& datalist.get(0).get("filename").equals("..")) {
			currentDirectory = new File(datalist.get(0).get("fullpath"))
					.getAbsolutePath();
			refreshList();
			return true;

		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.select_file, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {

		switch (menuItem.getItemId()) {
		case R.id.select_file_help:
			showHelp();
			break;
		}
		return true;
	}

	private void showHelp() {
		Intent intent = new Intent(this, Help.class);
		intent.putExtra("part", "select_file");
		startActivity(intent);
	}
	
	public void refreshList() {
		datalist = new ArrayList<HashMap<String, String>>();
		adapter = new FileListAdapter();
		ListView list = getListView();

		
		File f = new File(currentDirectory);
		if (!f.exists()) {
			f.mkdir();
		}
		File[] files = f.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				if (pathname.isDirectory())
					return true;
				return false;
			}

		});
		HashMap<String, String> h;
		if (isInSubdirectory()) {
			h = new HashMap<String, String>();
			h.put("fullpath", f.getParent());
			h.put("filename", "..");

			datalist.add(h);
			adapter.addDirectory("..");
		}

		if (files != null)
			for (File file : files) {
				h = new HashMap<String, String>();
				h.put("fullpath", file.getAbsolutePath());
				h.put("filename", file.getName());
				datalist.add(h);
				adapter.addDirectory(file.getName());
			}
		files = f.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				if (pathname.getName().endsWith(".txt"))
					return true;
				return false;
			}

		});
		if (files != null)
			for (File file : files) {
				h = new HashMap<String, String>();
				h.put("fullpath", file.getAbsolutePath());
				h.put("filename", file.getName());
				datalist.add(h);
				
				adapter.addFile(file.getName());
			}
		
		list.setAdapter(adapter);
	}

	private boolean isInSubdirectory() {
		return !currentDirectory.equals(Environment
				.getExternalStorageDirectory()
				+ "/" + Langleo.DIR_NAME);
	}

	private class FileListAdapter implements ListAdapter {

		ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();

		ArrayList<String> directories = new ArrayList<String>();
		ArrayList<String> files = new ArrayList<String>();

		public final int VIEWTYPE_DIR = 0;
		public final int VIEWTYPE_FILE = 1;

		public void finalize() {
			int len = observers.size();
			for (int i = 0; i < len; i++)
				observers.get(i).onInvalidated();
		}

		private void notifyObservers() {
			int len = observers.size();
			for (int i = 0; i < len; i++)
				observers.get(i).onChanged();
		}

		public void addDirectory(String name) {
			directories.add(name);
			notifyObservers();
		}

		public void addFile(String name) {
			files.add(name);
			notifyObservers();
		}

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

		@Override
		public int getCount() {
			return directories.size() + files.size();
		}

		@Override
		public Object getItem(int position) {
			if (position < directories.size())
				return directories.get(position);
			if (position - directories.size() < files.size())
				return files.get(position - directories.size());
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemViewType(int position) {
			if (position < directories.size())
				return VIEWTYPE_DIR;
			return VIEWTYPE_FILE;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView v;
			if (position < directories.size()) {

				if (convertView != null) {
					v = (TextView) convertView;
				} else
					v = (TextView) View.inflate(SelectFile.this,
							R.layout.dir_item, null);
				v.setText(directories.get(position));
			} else {
				boolean casts = true;

				if (convertView != null && casts) {
					v = (TextView) convertView;
				} else
					v = (TextView) View.inflate(SelectFile.this,
							R.layout.file_item, null);
				v.setText(files.get(position - directories.size()));
			}
			return v;
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		public boolean isDirectory(int position) {
			return position < directories.size();
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isEmpty() {
			return directories.size() + files.size() == 0;
		}

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}

	}
}
