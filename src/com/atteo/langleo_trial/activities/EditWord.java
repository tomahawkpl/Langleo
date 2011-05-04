package com.atteo.langleo_trial.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.atteo.langleo_trial.R;
import com.atteo.langleo_trial.models.Word;

public class EditWord extends Activity {
	private Word word;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_word);
		Bundle bundle = getIntent().getBundleExtra("word");
		word = new Word();
		word.loadBundle(bundle);
		word.load();
		Button button = (Button) findViewById(R.id.edit_word_cancel);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				cancel();
			}
		});
		button = (Button) findViewById(R.id.edit_word_ok);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				OK();
			}
		});

		button = (Button) findViewById(R.id.edit_word_new);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				new_word();
			}
		});

		TextView tv_word = (TextView) findViewById(R.id.edit_word_word);
		tv_word.setText(word.getWord());

		TextView tv_translation = (TextView) findViewById(R.id.edit_word_translation);
		tv_translation.setText(word.getTranslation());

		TextView tv_note = (TextView) findViewById(R.id.edit_word_note);
		tv_note.setText(word.getNote());

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.edit_word, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case R.id.edit_word_help:
			showHelp();
			break;
		}
		return true;
	}

	private void showHelp() {
		Intent intent = new Intent(this, Help.class);
		intent.putExtra("part", "edit_word");
		startActivity(intent);
	}

	private void OK() {
		Intent intent = new Intent();
		TextView tv_word = (TextView) findViewById(R.id.edit_word_word);
		TextView tv_translation = (TextView) findViewById(R.id.edit_word_translation);
		TextView tv_note = (TextView) findViewById(R.id.edit_word_note);
		String word_ = tv_word.getText().toString();
		String translation = tv_translation.getText().toString();
		String note = tv_note.getText().toString();
		word.setWord(word_);
		word.setTranslation(translation);
		word.setNote(note);
		intent.putExtra("word", word.toBundle());

		setResult(RESULT_OK, intent);
		finish();
	}

	private void cancel() {
		setResult(RESULT_CANCELED, null);
		finish();
	}

	private void new_word() {
		TextView tv_word = (TextView) findViewById(R.id.edit_word_word);
		TextView tv_translation = (TextView) findViewById(R.id.edit_word_translation);
		TextView tv_note = (TextView) findViewById(R.id.edit_word_note);
		String word_ = tv_word.getText().toString();
		String translation = tv_translation.getText().toString();
		String note = tv_note.getText().toString();
		word.setWord(word_);
		word.setTranslation(translation);
		word.setNote(note);
		word.save();
		Word nword = new Word();
		nword.setList(word.getList());
		word = nword;
		tv_word.setText(word.getWord());
		tv_translation.setText(word.getTranslation());
		tv_word.requestFocus();
	}

}
