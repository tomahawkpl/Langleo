package com.atteo.langleo.activities;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.atteo.langleo.ImportData;
import com.atteo.langleo.ImportFile;
import com.atteo.langleo.R;
import com.atteo.langleo.models.Collection;
import com.atteo.langleo.util.BetterAsyncTask;

public class ImportFromFile extends Activity {
	private Collection collection;

	private String wordDelim = "\t";
	private ImportData importData;

	private ProgressDialog loadDialog;

	private CheckBox checkbox;
	
	private final int DIALOG_LOADING = 1;
	private final int DIALOG_WORD_DELIMITER = 2;

	private final int REQUEST_SELECT_FILE = 1;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.import_from_file);
		collection = new Collection();
		collection.loadBundle(getIntent().getBundleExtra("collection"));
		collection.load();

		Button button = (Button) findViewById(R.id.import_from_file_select_file);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				selectFile();
			}
		});
		button = (Button) findViewById(R.id.import_from_file_import);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				returnFile();
			}
		});

		RadioButton radio = (RadioButton) findViewById(R.id.import_from_file_radio_other);
		radio.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showDialog(DIALOG_WORD_DELIMITER);

			}

		});

		radio = (RadioButton) findViewById(R.id.import_from_file_radio_tabulator);
		radio.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				importData.wordDelimiter = "\t";
				updateExample();
			}

		});

		checkbox = (CheckBox) findViewById(R.id.import_from_file_switch_order);
		checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				importData.switchOrder = isChecked;
				updateExample();
			}

		});

		if (savedInstanceState != null) {
			Bundle b = savedInstanceState.getBundle("import_data");
			importData = new ImportData();
			importData.loadBundle(b);
			updateImportData();
		} else
			selectFile();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.import_from_file, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {

		switch (menuItem.getItemId()) {
		case R.id.import_from_file_help:
			showHelp();
			break;
		}
		return true;
	}

	private void showHelp() {
		Intent intent = new Intent(this, Help.class);
		intent.putExtra("part", "import");
		startActivity(intent);
	}
	
	private void selectFile() {
		Intent intent = new Intent(getApplicationContext(), SelectFile.class);
		startActivityForResult(intent, REQUEST_SELECT_FILE);
	}

	public Dialog onCreateDialog(int dialog) {
		switch (dialog) {
		case DIALOG_LOADING:
			loadDialog = new ProgressDialog(this);
			loadDialog.setMessage(getString(R.string.loading));
			loadDialog.setCancelable(false);
			return loadDialog;
		case DIALOG_WORD_DELIMITER:
			LayoutInflater factory = LayoutInflater.from(this);
			final View textEntryView = factory.inflate(
					R.layout.edittext_dialog, null);
			return new AlertDialog.Builder(this).setTitle(
					R.string.insert_word_delimiter).setView(textEntryView)
					.setPositiveButton(R.string.ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									EditText et_word_delim = (EditText) textEntryView
											.findViewById(R.id.edittext_dialog_content);
									importData.wordDelimiter = et_word_delim
											.getText().toString();
									updateExample();

								}
							}).setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									if (importData.wordDelimiter.equals("\t")) {
										RadioGroup group = (RadioGroup) findViewById(R.id.import_from_file_radio_group);
										group
												.check(R.id.import_from_file_radio_tabulator);
									}

								}
							}).create();

		}
		return null;

	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_CANCELED && importData == null) {
			finish();
			return;
		}

		if (resultCode == RESULT_OK)
			switch (requestCode) {
			case REQUEST_SELECT_FILE:
				showDialog(DIALOG_LOADING);
				Bundle b = intent.getExtras();
				new LoadTask().execute(b);
				
				break;
			}

	}

	private void returnFile() {
		if (importData == null)
			throw new RuntimeException("No input file read");

		Intent intent = new Intent();
		intent.putExtra("import_data", importData.toBundle());
		intent.putExtra("collection", collection.toBundle());
		setResult(RESULT_OK, intent);
		finish();
	}

	protected void onSaveInstanceState(Bundle bundle) {
		super.onSaveInstanceState(bundle);
		if (importData != null)
			bundle.putBundle("import_data", importData.toBundle());
	}

	private void emptyFile() {
		new AlertDialog.Builder(this).setTitle(R.string.empty_file).setMessage(
				R.string.empty_file_select_another).setPositiveButton(
				R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						selectFile();
					}
				}).create().show();

	}

	private void updateExample() {
		String firstLine = importData.getFirstLine();
		TextView tv_input = (TextView) findViewById(R.id.import_from_file_example_input);
		tv_input.setText(firstLine);
		String[] tokens = firstLine.split(importData.wordDelimiter);
		if (tokens.length == 2) {
			TextView tv_word = (TextView) findViewById(R.id.import_from_file_example_word);
			TextView tv_translation = (TextView) findViewById(R.id.import_from_file_example_translation);
			collection.getBaseLanguage().load();
			collection.getTargetLanguage().load();
			String word = tokens[0];
			String translation = tokens[1];
			if (importData.switchOrder) {
				word = tokens[1];
				translation = tokens[0];
			}
			tv_word.setText(getString(R.string.example_word, collection
					.getBaseLanguage().getName(), word));
			tv_translation.setText(getString(R.string.example_translation,
					collection.getTargetLanguage().getName(), translation));
			Button b = (Button) findViewById(R.id.import_from_file_import);
			b.setVisibility(View.VISIBLE);
			TextView tv_error = (TextView) findViewById(R.id.import_from_file_parsing_error);
			tv_error.setVisibility(View.GONE);
		} else {
			TextView tv_word = (TextView) findViewById(R.id.import_from_file_example_word);
			TextView tv_translation = (TextView) findViewById(R.id.import_from_file_example_translation);
			TextView tv_error = (TextView) findViewById(R.id.import_from_file_parsing_error);
			tv_error.setVisibility(View.VISIBLE);
			tv_word.setText("");
			tv_translation.setText("");
			Button b = (Button) findViewById(R.id.import_from_file_import);
			b.setVisibility(View.INVISIBLE);
		}

	}

	public void updateImportData() {
		TextView tv_filename = (TextView) findViewById(R.id.import_from_file_selected_file_name);
		if (importData.contents.size() == 1)
			tv_filename.setText(importData.contents.get(0).filename);
		else
			tv_filename.setText(importData.contents.size() + " "
					+ getString(R.string.files_selected));
		updateExample();
	}

	private class LoadTask extends BetterAsyncTask<Bundle, Void, ImportData> {

		@Override
		public void onPreExecute() {
			showDialog(DIALOG_LOADING);
		}

		@Override
		public void onPostExecute(ImportData result) {
			dismissDialog(DIALOG_LOADING);
			if (result != null && result.getFirstLine() != null) {
				ImportFromFile.this.importData = result;
				importData.switchOrder = checkbox.isChecked();
				updateImportData();
			} else
				emptyFile();

		}

		@Override
		protected ImportData doInBackground(Bundle... args) {
			Bundle b = args[0];
			Set<String> k = b.keySet();
			String[] keys = k.toArray(new String[k.size()]);
			int len = keys.length;
			Bundle fileToLoad;
			ImportData result = new ImportData();
			for (int i = 0; i < len; i++) {
				ImportFile importFile = new ImportFile();
				fileToLoad = b.getBundle(keys[i]);

				importFile.fullpath = fileToLoad.getString("fullpath");
				importFile.filename = fileToLoad.getString("filename");
				result.wordDelimiter = wordDelim;

				importFile.lines = new ArrayList<String>();

				FileReader fileReader = null;

				try {
					fileReader = new FileReader(importFile.fullpath);
				} catch (FileNotFoundException e) {
					return null;
				}

				BufferedReader reader = new BufferedReader(fileReader);
				String line;
				int lineNumber = 0;
				while (true) {
					lineNumber++;
					try {
						line = reader.readLine();
					} catch (IOException e) {
						return null;
					}

					if (line == null)
						break;

					line = line.trim();
					if (line.length() == 0)
						continue;

					importFile.lines.add(line);
				}

				try {
					reader.close();
					fileReader.close();
				} catch (IOException e) {
					return null;
				}

				result.contents.add(importFile);
			}
			return result;

		}
	}

}
