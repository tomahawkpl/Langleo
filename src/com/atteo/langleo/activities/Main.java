package com.atteo.langleo.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.atteo.langleo.Langleo;
import com.atteo.langleo.R;
import com.atteo.silo.Silo;

public class Main extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		if (!Silo.isOpened()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.database_error));
			builder.setNeutralButton(R.string.ok, null);
			AlertDialog alert = builder.create();
			alert.show();
		}

		Button button = (Button) findViewById(R.id.button_study);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				study();
			}
		});


		button = (Button) findViewById(R.id.button_manage);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				manageCollections();
			}
		});

		button = (Button) findViewById(R.id.button_preferences);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				preferences();
			}
		});

		button = (Button) findViewById(R.id.button_help);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				showHelp();
			}
		});

	}

	// Button handlers

	private void study() {
		String a;
		if ((a = Langleo.getLearningAlgorithm().isQuestionWaiting()) != null) {
			Toast.makeText(this, a, Toast.LENGTH_LONG).show();
			return;
		}
		Intent intent = new Intent(this, Study.class);
		startActivity(intent);
	}


	private void manageCollections() {
		Intent intent = new Intent(this, Collections.class);
		startActivity(intent);
	}

	private void preferences() {
		Intent intent = new Intent(this, Preferences.class);
		startActivity(intent);
	}

	private void showHelp() {
		Intent intent = new Intent(this, Help.class);
		intent.putExtra("part", "");
		startActivity(intent);
	}

}