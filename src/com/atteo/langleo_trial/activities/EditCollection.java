package com.atteo.langleo_trial.activities;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.atteo.langleo_trial.Langleo;
import com.atteo.langleo_trial.R;
import com.atteo.langleo_trial.models.Collection;
import com.atteo.langleo_trial.models.Language;

public class EditCollection extends Activity {
	private Collection collection;

	private Language selectedBaseLanguage;
	private Language selectedTargetLanguage;

	private String[] languageStrings;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_collection);
		Bundle bundle = getIntent().getBundleExtra("collection");
		collection = new Collection();
		collection.loadBundle(bundle);
		collection.load();

		Button button = (Button) findViewById(R.id.edit_collection_cancel);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				cancelCollection();
			}
		});
		button = (Button) findViewById(R.id.edit_collection_ok);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				returnCollection();
			}
		});

		EditText et_name = (EditText) findViewById(R.id.edit_collection_name);
		et_name.setText(collection.getName());

		SeekBar e = (SeekBar) findViewById(R.id.edit_collection_priority);
		e.setProgress(collection.getPriority() - 1);

		CheckBox cb_disabled = (CheckBox) findViewById(R.id.edit_collection_disabled);
		cb_disabled.setChecked(collection.getDisabled());

		cb_disabled.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateNewWords();
			}

		});

		if (savedInstanceState != null) {
			Bundle b = savedInstanceState.getBundle("selectedBaseLanguage");
			selectedBaseLanguage = new Language();
			selectedBaseLanguage.loadBundle(b);
			b = savedInstanceState.getBundle("selectedTargetLanguage");
			selectedTargetLanguage = new Language();
			selectedTargetLanguage.loadBundle(b);
		} else {
			selectedBaseLanguage = collection.getBaseLanguage();
			selectedTargetLanguage = collection.getTargetLanguage();
		}

		updateNewWords();

		updateBaseLanguage();
		updateTargetLanguage();

		ArrayList<Language> languages = Langleo.getLanguages();
		ArrayList<String> strings = new ArrayList<String>();
		int len = languages.size();
		for (int i = 0; i < len; i++) {
			strings.add(languages.get(i).getName());
		}
		languageStrings = strings.toArray(new String[len]);

		Button btn_change = (Button) findViewById(R.id.edit_collection_change_base_language);
		btn_change.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						EditCollection.this);
				builder.setTitle(R.string.select_language);
				builder.setItems(languageStrings,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								selectedBaseLanguage = Langleo.getLanguages()
										.get(item);
								updateBaseLanguage();
							}
						});
				AlertDialog alert = builder.create();
				alert.show();

			}

		});

		btn_change = (Button) findViewById(R.id.edit_collection_change_target_language);
		btn_change.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						EditCollection.this);
				builder.setTitle(R.string.select_language);
				builder.setItems(languageStrings,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								selectedTargetLanguage = Langleo.getLanguages()
										.get(item);
								updateTargetLanguage();
							}
						});
				AlertDialog alert = builder.create();
				alert.show();

			}

		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.edit_collection, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case R.id.edit_collection_help:
			showHelp();
			break;
		}
		return true;
	}

	private void showHelp() {
		Intent intent = new Intent(this, Help.class);
		intent.putExtra("part", "edit_collection");
		startActivity(intent);
	}
	
	private void updateNewWords() {
		CheckBox cb_enabled = (CheckBox) findViewById(R.id.edit_collection_disabled);
		SeekBar e = (SeekBar) findViewById(R.id.edit_collection_priority);
		TextView tv_enabled_title = (TextView) findViewById(R.id.edit_collection_max_new_words_title);
		if (cb_enabled.isChecked()) {
			e.setEnabled(false);
			e.setFocusable(false);
			tv_enabled_title.setEnabled(false);
		} else {
			e.setEnabled(true);
			e.setFocusableInTouchMode(true);
			tv_enabled_title.setEnabled(true);
		}
	}

	private void updateBaseLanguage() {
		selectedBaseLanguage.load();
		ImageView iv = (ImageView) findViewById(R.id.edit_collection_base_language_image);
		iv.setImageDrawable(getResources().getDrawable(
				getResources().getIdentifier(
						"flag_" + selectedBaseLanguage.getName().toLowerCase(),
						"drawable", Langleo.PACKAGE)));
		TextView tv = (TextView) findViewById(R.id.edit_collection_base_language_name);
		tv.setText(selectedBaseLanguage.getName());
	}

	private void updateTargetLanguage() {
		selectedTargetLanguage.load();
		ImageView iv = (ImageView) findViewById(R.id.edit_collection_target_language_image);
		iv.setImageDrawable(getResources().getDrawable(
				getResources().getIdentifier(
						"flag_"
								+ selectedTargetLanguage.getName()
										.toLowerCase(), "drawable",
						Langleo.PACKAGE)));
		TextView tv = (TextView) findViewById(R.id.edit_collection_target_language_name);
		tv.setText(selectedTargetLanguage.getName());
	}

	private void returnCollection() {
		Intent intent = new Intent();
		EditText et_name = (EditText) findViewById(R.id.edit_collection_name);
		String name = et_name.getText().toString().trim();
		collection.setName(name);
		CheckBox cb_disabled = (CheckBox) findViewById(R.id.edit_collection_disabled);
		collection.setDisabled(cb_disabled.isChecked());
		SeekBar e = (SeekBar) findViewById(R.id.edit_collection_priority);
		collection.setPriority(e.getProgress() + 1);
		collection.setBaseLanguage(selectedBaseLanguage);
		collection.setTargetLanguage(selectedTargetLanguage);
		intent.putExtra("collection", collection.toBundle());
		
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	protected void onSaveInstanceState(Bundle bundle) {
		super.onSaveInstanceState(bundle);
		bundle.putBundle("selectedBaseLanguage", selectedBaseLanguage
				.toBundle());
		bundle.putBundle("selectedTargetLanguage", selectedTargetLanguage
				.toBundle());
	}

	private void cancelCollection() {
		setResult(RESULT_CANCELED, null);
		finish();
	}
}
