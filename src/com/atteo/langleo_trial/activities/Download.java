package com.atteo.langleo_trial.activities;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.atteo.langleo_trial.Langleo;
import com.atteo.langleo_trial.R;
import com.atteo.langleo_trial.models.Collection;
import com.atteo.langleo_trial.models.Language;
import com.atteo.langleo_trial.util.BetterAsyncTask;

public class Download extends ListActivity {
	public static final int BLOCK_SIZE = 10000;
	private static final long CACHE_TIMEOUT = 1000 * 60 * 60 * 24 * 3;

	private static Download INSTANCE = null;
	private Collection collection;

	private final int DIALOG_DOWNLOADING = 1;

	private final int DIALOG_LOADING_CACHE = 2;
	private boolean loading;

	private final int REQUEST_DETAILS = 1;
	private ArrayList<String> searchStrings;
	private ArrayList<StackData> shownStacks;

	private ArrayList<StackData> stacks;

	public static InputStream openHttpConnection(String urlString) {
		URI uri = null;
		try {
			uri = new URI(urlString);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		HttpGet get = new HttpGet(uri);
		HttpClient client = new DefaultHttpClient();

		try {
			HttpResponse response = client.execute(get);
			return response.getEntity().getContent();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		INSTANCE = this;

		if (savedInstanceState != null)
			loading = savedInstanceState.getBoolean("loading", false);

		setContentView(R.layout.download);

		setTitle("studystack.com");

		collection = new Collection();

		Intent intent = getIntent();
		collection.loadBundle(intent.getBundleExtra("collection"));

		ImageButton button = (ImageButton) findViewById(R.id.download_search);
		button.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				TextView tv = (TextView) findViewById(R.id.download_search_text);
				search(tv.getText().toString());

			}

		});

		button = (ImageButton) findViewById(R.id.download_clear);
		button.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				clear();

			}

		});

		if (!loading)
			loadStacks(true);
	}

	@Override
	public Dialog onCreateDialog(int dialog) {
		ProgressDialog progressDialog;
		switch (dialog) {
		case DIALOG_DOWNLOADING:
			progressDialog = new ProgressDialog(this);
			progressDialog
					.setMessage(getString(R.string.connecting_with_studystacks));
			progressDialog.setCancelable(false);
			return progressDialog;
		case DIALOG_LOADING_CACHE:
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage(getString(R.string.loading_index));
			progressDialog.setCancelable(false);
			return progressDialog;

		}
		return null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.download, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case R.id.download_refresh:
			loadStacks(false);
			break;
		case R.id.download_help:
			showHelp();
			break;
		}
		return true;
	}

	@Override
	public void onSaveInstanceState(Bundle b) {
		b.putBoolean("loading", loading);
	}

	private void clear() {
		TextView tv = (TextView) findViewById(R.id.download_search_text);
		tv.setText("");

		shownStacks = stacks;

		DownloadAdapter adapter = new DownloadAdapter();
		adapter.setStacks(shownStacks);
		setListAdapter(adapter);
	}

	private String getFromCache(int stackid) {
		FileInputStream in = null;
		try {
			in = openFileInput("stack_" + stackid);
		} catch (FileNotFoundException e) {
			return null;
		}
		InputStreamReader isr = new InputStreamReader(in);
		LineNumberReader reader = new LineNumberReader(isr);
		String result = "";

		try {
			result = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (Long.valueOf(result) + CACHE_TIMEOUT < new Date().getTime())
			return null;

		StringBuilder readCache = new StringBuilder();
		char data[] = new char[BLOCK_SIZE];
		int r;
		try {
			while (true) {
				r = reader.read(data);
				if (r == -1)
					break;
				readCache.append(data, 0, r);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}

		try {
			reader.close();
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return readCache.toString();
	}

	private StackData getStackFromLine(String line) {
		String parts[] = line.split("\\|");
		if (parts.length != 6 && parts.length != 3
				&& !(line.endsWith("|") && parts.length == 5)) {
			return null;
		}

		StackData result = new StackData();
		// result.put("original", line);
		result.id = parts[0];
		result.name = parts[1];
		result.description = parts[2];
		// result.put("cards", parts[3]);
		// result.put("creationDate", parts[4]);
		// if (parts.length == 5)
		// result.put("stars", "0");
		// else
		// result.put("stars", parts[5]);
		return result;
	}

	private ArrayList<StackData> getStacksForCategory(int category,
			boolean useCache) {
		ArrayList<StackData> result = new ArrayList<StackData>();

		String content = null;

		if (!useCache || (content = getFromCache(category)) == null) {
			StringBuilder new_content;

			int page = 1;
			InputStream in = null;
			content = "";

			String url;

			InputStreamReader reader;

			char input[] = new char[BLOCK_SIZE];
			int read;
			int s;
			while (true) {
				url = getStudyStackURL(category, page);
				in = openHttpConnection(url);

				if (in == null)
					return null;

				reader = new InputStreamReader(in, Charset
						.forName("ISO-8859-1"));

				read = -1;
				new_content = new StringBuilder();
				s = result.size();
				while (true) {
					try {
						read = reader.read(input);
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (read == -1)
						break;

					// if (new_content.equals(""))
					// new_content = new String(input).substring(0, read);
					// else
					new_content.append(input, 0, read);
				}

				getStacksFromString(result, new_content.toString());
				if (s == result.size())
					break;

				page++;
			}

			java.util.Collections.sort(result, new Comparator<StackData>() {
				public int compare(StackData object1, StackData object2) {
					return object1.name.compareToIgnoreCase(object2.name);
				}

			});

			updateCache(category, result);
		} else {
			getStacksFromString(result, content);
		}

		return result;
	}

	private String getStacksFromString(ArrayList<StackData> stacks,
			String content) {
		String lines[];
		int len;
		StackData stackData;
		lines = content.split("\n");
		len = lines.length;
		String result = "";
		for (int i = 0; i < len; i++) {
			stackData = getStackFromLine(lines[i]);
			if (stackData != null)
				stacks.add(stackData);
			else if (i == len - 1)
				result = lines[i];
		}
		return result;
	}

	private String getStudyStackURL(int categoryId, int page) {
		return "http://www.studystack.com/categoryStacks.jsp?page=" + page
				+ "&categoryId=" + categoryId;
	}

	private boolean isCacheValid(int stackid) {
		boolean result = false;
		FileInputStream in = null;
		try {
			in = openFileInput("stack_" + stackid);
		} catch (FileNotFoundException e) {
			return false;
		}
		InputStreamReader isr = new InputStreamReader(in);
		LineNumberReader reader = new LineNumberReader(isr);
		String line = null;

		try {
			line = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (Long.valueOf(line) + CACHE_TIMEOUT > new Date().getTime())
			result = true;

		try {
			reader.close();
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return result;
	}

	private void loadStacks(boolean useCache) {
		if (!Langleo.isConnectionAvailable(this))
			return;
		new LoadStacksTask(useCache).execute();

	}

	private void search(String text) {
		text = text.toLowerCase();

		if (text.equals("")) {
			shownStacks = stacks;
		} else {
			shownStacks = new ArrayList<StackData>();
			int len = searchStrings.size();
			for (int i = 0; i < len; i++)
				if (searchStrings.get(i).indexOf(text) != -1)
					shownStacks.add(stacks.get(i));
		}
		DownloadAdapter adapter = new DownloadAdapter();
		adapter.setStacks(shownStacks);
		setListAdapter(adapter);

	}

	private void showHelp() {
		Intent intent = new Intent(this, Help.class);
		intent.putExtra("part", "download");
		startActivity(intent);
	}

	private void updateCache(int stackid, ArrayList<StackData> content) {
		FileOutputStream out = null;
		try {
			out = openFileOutput("stack_" + stackid, Context.MODE_PRIVATE);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		OutputStreamWriter writer = new OutputStreamWriter(out);
		String str = String.valueOf(new Date().getTime());
		try {
			int len = content.size();
			writer.write(str);
			writer.write("\n");
			for (int i = 0; i < len; i++) {
				StackData h = content.get(i);
				writer.write(h.id);
				writer.write("|");
				writer.write(h.name);
				writer.write("|");
				writer.write(h.description);
				writer.write("\n");
			}
			writer.close();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (resultCode == RESULT_CANCELED)
			return;
		switch (requestCode) {
		case REQUEST_DETAILS:
			Intent i = new Intent();
			i.putExtras(intent);
			setResult(RESULT_OK, intent);
			finish();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		INSTANCE = null;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (!Langleo.isConnectionAvailable(this))
			return;
		Intent intent = new Intent(this, StackDetails.class);
		intent.putExtra("collection", collection.toBundle());
		intent.putExtra("id", shownStacks.get(position).id);
		intent.putExtra("name", shownStacks.get(position).name);
		intent.putExtra("description", shownStacks.get(position).description);
		startActivityForResult(intent, REQUEST_DETAILS);
	}

	private class DownloadAdapter extends BaseAdapter {
		ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();
		ArrayList<StackData> stacks = null;

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		public int getCount() {
			return stacks.size();
		}

		public Object getItem(int position) {
			return stacks.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public int getItemViewType(int position) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			ViewHolder h;

			if (convertView != null) {
				v = convertView;
				h = (ViewHolder) convertView.getTag();
			} else {
				v = View.inflate(Download.this, R.layout.stack_item, null);
				h = new ViewHolder();
				h.name = (TextView) v.findViewById(R.id.stack_item_name);
				h.description = (TextView) v
						.findViewById(R.id.stack_item_description);
				v.setTag(h);

			}
			h.name.setText(stacks.get(position).name);
			h.description.setText(stacks.get(position).description);
			return v;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isEmpty() {
			return stacks == null || stacks.size() == 0;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		public void setStacks(ArrayList<StackData> stacks) {
			this.stacks = stacks;
			notifyObservers();
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}

		private void notifyObservers() {
			int len = observers.size();
			for (int i = 0; i < len; i++)
				observers.get(i).onChanged();
		}

		private class ViewHolder {
			TextView name, description;
		}

	}

	private class LoadStacksTask extends
			BetterAsyncTask<Void, Void, ArrayList<StackData>> {
		private boolean useCache = true;

		public LoadStacksTask(boolean useCache) {
			this.useCache = useCache;
		}

		@Override
		protected ArrayList<StackData> doInBackground(Void... params) {
			ArrayList<StackData> stacks = new ArrayList<StackData>();

			Language l = collection.getTargetLanguage();
			l.load();

			stacks = getStacksForCategory(l.getStudyStackId(), useCache);
			return stacks;
		}

		@Override
		protected void onPostExecute(ArrayList<StackData> result) {
			if (result == null) {
				finish();
				return;
			}

			int len = result.size();
			Download.INSTANCE.searchStrings = new ArrayList<String>();
			for (int i = 0; i < len; i++)
				Download.INSTANCE.searchStrings.add(result.get(i).name
						.toLowerCase()
						+ " " + result.get(i).description.toLowerCase());
			DownloadAdapter adapter = new DownloadAdapter();
			adapter.setStacks(result);
			Download.INSTANCE.setListAdapter(adapter);
			Download.INSTANCE.stacks = result;
			Download.INSTANCE.shownStacks = stacks;
			if (useCache)
				Download.INSTANCE.removeDialog(DIALOG_LOADING_CACHE);
			else
				Download.INSTANCE.removeDialog(DIALOG_DOWNLOADING);
			Download.INSTANCE.loading = false;
		}

		@Override
		protected void onPreExecute() {
			Download.INSTANCE.loading = true;
			useCache = useCache
					&& isCacheValid(((Language) collection.getTargetLanguage()
							.l()).getStudyStackId());
			if (useCache)
				Download.INSTANCE.showDialog(DIALOG_LOADING_CACHE);
			else
				Download.INSTANCE.showDialog(DIALOG_DOWNLOADING);
		}
	}

	private class StackData {
		String id, name, description;
	}

}
