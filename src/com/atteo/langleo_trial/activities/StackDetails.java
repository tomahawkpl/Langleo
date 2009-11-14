package com.atteo.langleo_trial.activities;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.atteo.langleo_trial.Langleo;
import com.atteo.langleo_trial.R;
import com.atteo.langleo_trial.models.Collection;
import com.atteo.langleo_trial.models.Language;
import com.atteo.langleo_trial.models.Word;
import com.atteo.langleo_trial.util.BetterAsyncTask;

public class StackDetails extends ListActivity {

	private int stackId;
	private String stackName;
	private String stackDescription;
	
	private String content;
	private Collection collection;
	
	private boolean switchOrder;
	
	private ArrayList<Word> words;
	
	private static final int DIALOG_DOWNLOADING = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.stack_details);
		setTitle("studystack.com");
		
		Intent intent = getIntent();
		stackId = Integer.valueOf(intent.getStringExtra("id"));
		stackName = intent.getStringExtra("name");
		stackDescription = intent.getStringExtra("description");
		collection = new Collection();
		collection.loadBundle(intent.getBundleExtra("collection"));
		collection.load();
		
		Language l;
		
		TextView tv = (TextView) findViewById(R.id.stack_details_name);
		tv.setText(stackName);
		tv = (TextView) findViewById(R.id.stack_details_description);
		tv.setText(stackDescription);
		tv = (TextView) findViewById(R.id.stack_details_base_language);
		l = collection.getBaseLanguage();
		l.load();
		tv.setText(l.getName());
		tv = (TextView) findViewById(R.id.stack_details_target_language);
		l = collection.getTargetLanguage();
		l.load();
		tv.setText(l.getName());
		
		getListView().setClickable(false);
		
		CheckBox checkbox = (CheckBox) findViewById(R.id.stack_details_switch_order);
		checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				switchOrder = isChecked;
				loadWordsFromString(content);
				updateExample();
			}

		});
		
		Button button = (Button) findViewById(R.id.stack_details_download);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				download();
			}
			
		});
		
		loadStack();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.stack_details, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case R.id.stack_details_help:
			showHelp();
			break;
		}
		return true;
	}

	private void showHelp() {
		Intent intent = new Intent(this, Help.class);
		intent.putExtra("part", "list_details");
		startActivity(intent);
	}
	
	public Dialog onCreateDialog(int dialog) {
		ProgressDialog progressDialog;
		switch (dialog) {
		case DIALOG_DOWNLOADING:
			progressDialog = new ProgressDialog(this);
			progressDialog
					.setMessage(getString(R.string.loading_words));
			progressDialog.setCancelable(false);
			return progressDialog;

		}
		return null;
	}
	
	private void download() {
		if (!Langleo.isConnectionAvailable(this))
			return;
		Intent intent = new Intent();
		intent.putExtra("name",stackName);
		int len = words.size();
		intent.putExtra("words",len);
		for (int i =0;i<len;i++)
			intent.putExtra("word_"+i,words.get(i).toBundle());
		setResult(RESULT_OK,intent);
		finish();
	}
	
	private String getStudyStackURL(int stackId) {
		return "http://www.studystack.com/servlet/simpledelim?studyStackId="
				+ stackId + "&delimiter=%7C";
	}

	private Word getWordFromLine(String line) {
		String parts[] = line.split("\\|");
		if (parts.length != 2) {
			return null;
		}
		Word result = new Word();
		if (switchOrder) {
			result.setWord(parts[0].trim());
			result.setTranslation(parts[1].trim());
		} else {
			result.setWord(parts[1].trim());
			result.setTranslation(parts[0].trim());
		}
		return result;
	}
	
	private void loadStack() {
		new LoadStackTask().execute(stackId);
	}
	
	private ArrayList<Word> loadWordsFromString(String content) {
		words = new ArrayList<Word>();
		String lines[];
		int len;
		Word stackData;
		lines = content.split("\r\n");
		len = lines.length;
		for (int i = 0; i < len; i++) {
			stackData = getWordFromLine(lines[i]);
			if (stackData != null)
				words.add(stackData);
		}
		
		return words;
	}
	
	private void updateExample() {
		ArrayList<HashMap<String,String>> hash = new ArrayList<HashMap<String,String>>();
		int len = words.size();
		HashMap<String,String> h;
		for(int i=0;i<len;i++) {
			h = new HashMap<String,String>();
			h.put("word", words.get(i).getWord());
			h.put("translation", words.get(i).getTranslation());
			hash.add(h);
		}
		SimpleAdapter adapter = new SimpleAdapter(StackDetails.this, hash,
				R.layout.stack_details_item, new String[] { "word", "translation" },
				new int[] { R.id.stack_details_item_word, R.id.stack_details_item_translation });
		setListAdapter(adapter);
	}
	
	private class LoadStackTask extends BetterAsyncTask<Integer, Void, ArrayList<Word>> {

		@Override
		protected void onPreExecute() {
			showDialog(DIALOG_DOWNLOADING);
		}

		@Override
		protected void onPostExecute(ArrayList<Word> result) {
			TextView tv = (TextView) findViewById(R.id.stack_details_words);
			tv.setText(words.size() + " " + getString(R.string.words));
			
			updateExample();
			removeDialog(DIALOG_DOWNLOADING);
		}

		@Override
		protected ArrayList<Word> doInBackground(Integer... params) {
			char input[] = new char[Download.BLOCK_SIZE];
			InputStream in = Download.openHttpConnection(getStudyStackURL(stackId));
			
			InputStreamReader reader = new InputStreamReader(in, Charset
					.forName("ISO-8859-1"));
			
			int read = -1;
			content = "";
			while (true) {
				try {
					read = reader.read(input);
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (read == -1)
					break;

				content += new String(input).substring(0, read);

			}

			return loadWordsFromString(content);
		}
		
		
	}
}
