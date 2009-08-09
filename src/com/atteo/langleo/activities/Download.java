package com.atteo.langleo.activities;

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
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.atteo.langleo.Langleo;
import com.atteo.langleo.R;
import com.atteo.langleo.models.Collection;
import com.atteo.langleo.models.Language;
import com.atteo.langleo.util.BetterAsyncTask;

public class Download extends ListActivity {

	private Collection collection;

	private final int DIALOG_DOWNLOADING = 1;

	private final int REQUEST_DETAILS = 1;

	public static final int BLOCK_SIZE = 10000;

	private ArrayList<HashMap<String, String>> stacks;
	private ArrayList<HashMap<String, String>> shownStacks;
	private ArrayList<String> searchStrings;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.download);

		setTitle("studystack.com");

		collection = new Collection();

		Intent intent = getIntent();
		collection.loadBundle(intent.getBundleExtra("collection"));

		ImageButton button = (ImageButton) findViewById(R.id.download_search);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				TextView tv = (TextView) findViewById(R.id.download_search_text);
				search(tv.getText().toString());

			}

		});

		button = (ImageButton) findViewById(R.id.download_clear);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				clear();

			}

		});

		loadStacks(true);
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

	private void showHelp() {
		Intent intent = new Intent(this, Help.class);
		intent.putExtra("part", "download");
		startActivity(intent);
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

		}
		return null;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (!Langleo.isConnectionAvailable(this))
			return;
		Intent intent = new Intent(this, StackDetails.class);
		intent.putExtra("collection", collection.toBundle());
		intent.putExtra("id", shownStacks.get(position).get("id"));
		intent.putExtra("name", shownStacks.get(position).get("name"));
		intent.putExtra("description", shownStacks.get(position).get(
				"description"));
		startActivityForResult(intent, REQUEST_DETAILS);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_CANCELED)
			return;
		switch(requestCode) {
		case REQUEST_DETAILS:
			Intent i = new Intent();
			i.putExtras(intent);
			setResult(RESULT_OK,intent);
			finish();
		}
	}

	private void search(String text) {
		text = text.toLowerCase();

		if (text.equals("")) {
			shownStacks = stacks;
		} else {
			shownStacks = new ArrayList<HashMap<String, String>>();
			int len = searchStrings.size();
			for (int i = 0; i < len; i++)
				if (searchStrings.get(i).indexOf(text) != -1)
					shownStacks.add(stacks.get(i));
		}
		SimpleAdapter adapter = new SimpleAdapter(Download.this, shownStacks,
				R.layout.stack_item, new String[] { "name", "description" },
				new int[] { R.id.stack_item_name, R.id.stack_item_description });
		Download.this.getListView().setAdapter(adapter);

	}

	private void clear() {
		TextView tv = (TextView) findViewById(R.id.download_search_text);
		tv.setText("");

		shownStacks = stacks;

		SimpleAdapter adapter = new SimpleAdapter(Download.this, shownStacks,
				R.layout.stack_item, new String[] { "name", "description" },
				new int[] { R.id.stack_item_name, R.id.stack_item_description });
		setListAdapter(adapter);
	}

	private void loadStacks(boolean useCache) {
		if (!Langleo.isConnectionAvailable(this))
			return;
		new LoadStacksTask().execute(useCache);

	}

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

	private String getStudyStackURL(int categoryId, int page) {
		return "http://www.studystack.com/categoryStacks.jsp?page=" + page
				+ "&categoryId=" + categoryId;
	}

	private ArrayList<HashMap<String, String>> getStacksForCategory(
			int category, boolean useCache) {
		ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();

		String content = null;

		if (!useCache || (content = getFromCache(category)) == null) {
			String new_content;

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
				new_content = "";
				s = result.size(); 
				while (true) {
					try {
						read = reader.read(input);
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (read == -1)
						break;

					//if (new_content.equals(""))
					//	new_content = new String(input).substring(0, read);
					//else
					new_content += new String(input).substring(0, read);
					

				}
				
				getStacksFromString(result,new_content);
				if (s == result.size())
					break;
				
				page++;
			}
			

			
			java.util.Collections.sort(result,
					new Comparator<HashMap<String, String>>() {
						@Override
						public int compare(HashMap<String, String> object1,
								HashMap<String, String> object2) {
							return object1.get("name").toLowerCase().compareTo(
									object2.get("name").toLowerCase());
						}

					});
			

			updateCache(category, result);
		} else {
			getStacksFromString(result,content);
		}


		return result;
	}

	private String getStacksFromString(ArrayList<HashMap<String, String>> stacks,
			String content) {
		String lines[];
		int len;
		HashMap<String, String> stackData;
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
	
	private HashMap<String, String> getStackFromLine(String line) {
		String parts[] = line.split("\\|");
		if (parts.length != 6 && parts.length != 3 && !(line.endsWith("|") && parts.length == 5)) {
			return null;
		}
		
		HashMap<String, String> result = new HashMap<String, String>();
//		result.put("original", line);
		result.put("id", parts[0]);
		result.put("name", parts[1]);
		result.put("description", parts[2]);
//		result.put("cards", parts[3]);
		//result.put("creationDate", parts[4]);
		//if (parts.length == 5)
//			result.put("stars", "0");
		//else
//			result.put("stars", parts[5]);
		return result;
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
		
		if (Long.valueOf(result) + 1000 * 60 * 60 * 24 < new Date().getTime())
			return null;


		result = "";
		char data[] = new char[BLOCK_SIZE];
		int r;
		try {
			while (true) {
				r = reader.read(data);
				if (r == -1)
					break;
				result += new String(data).substring(0,r);
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

		return result;
	}

	private void updateCache(int stackid, ArrayList<HashMap<String, String>> content) {
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
			for (int i = 0;i<len;i++) {
				HashMap<String, String> h = content.get(i);
				writer.write(h.get("id"));
				writer.write("|");
				writer.write(h.get("name"));
				writer.write("|");
				writer.write(h.get("description"));
				writer.write("\n");
			}
			writer.close();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

	}

	private class LoadStacksTask extends
			BetterAsyncTask<Boolean, Void, ArrayList<HashMap<String, String>>> {
		@Override
		protected void onPreExecute() {
			showDialog(DIALOG_DOWNLOADING);
		}

		@Override
		protected void onPostExecute(ArrayList<HashMap<String, String>> result) {
			if (result == null) {
				finish();
				return;
			}
			int len = result.size();
			searchStrings = new ArrayList<String>();
			for (int i = 0; i < len; i++)
				searchStrings.add(result.get(i).get("name").toLowerCase() + " "
						+ result.get(i).get("description").toLowerCase());
			SimpleAdapter adapter = new SimpleAdapter(Download.this, result,
					R.layout.stack_item,
					new String[] { "name", "description" }, new int[] {
							R.id.stack_item_name, R.id.stack_item_description });
			Download.this.getListView().setAdapter(adapter);
			stacks = result;
			shownStacks = stacks;
			removeDialog(DIALOG_DOWNLOADING);
		}

		@Override
		protected ArrayList<HashMap<String, String>> doInBackground(
				Boolean... params) {
			boolean useCache = params[0];
			ArrayList<HashMap<String, String>> stacks = new ArrayList<HashMap<String, String>>();

			Language l = collection.getTargetLanguage();
			l.load();

			stacks = getStacksForCategory(l.getStudyStackId(), useCache);
			return stacks;
		}
	}

}
