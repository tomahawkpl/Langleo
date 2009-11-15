package com.atteo.langleo_trial.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.atteo.langleo_trial.Langleo;
import com.atteo.langleo_trial.LearningAlgorithm;
import com.atteo.langleo_trial.R;
import com.atteo.langleo_trial.views.NumberPicker;
import com.atteo.langleo_trial.views.SelectLimitDialog;
import com.atteo.silo.Silo;

public class Main extends Activity {
	private boolean forceStudy = false;

	private static final int DIALOG_SELECT_LIMIT = 0;
	private static final int DIALOG_WELCOME = 1;
	private static final int DIALOG_WELCOME2 = 2;

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

		TextView version = (TextView) findViewById(R.id.version_label);
		version.setText(Langleo.VERSION);

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

	private boolean firstRun(SharedPreferences prefs) {
		boolean firstRun = prefs.getBoolean("first_run", true);
		if (firstRun) {
			Editor e = prefs.edit();
			e.putString("version", Langleo.VERSION);
			e.putBoolean("first_run", false);
			e.commit();
			showDialog(DIALOG_WELCOME);
		}
		return firstRun;
	}

	private void checkVersion(SharedPreferences prefs) {
		String version = prefs.getString("version", "");
		if (!version.equals(Langleo.VERSION)) {
			Editor e = prefs.edit();
			e.putString("version", Langleo.VERSION);
			e.commit();
			showUpdates();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		SharedPreferences prefs = Langleo.getPreferences();

		if (!firstRun(prefs))
			checkVersion(prefs);

	}

	// Button handlers

	@Override
	protected Dialog onCreateDialog(int dialogId) {
		AlertDialog.Builder builder;
		AlertDialog alert;
		switch (dialogId) {
		case DIALOG_WELCOME:
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.welcome_title).setMessage(
					getString(R.string.welcome_message)).setCancelable(false)
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									showDialog(DIALOG_WELCOME2);
								}
							});
			alert = builder.create();
			return alert;
		case DIALOG_WELCOME2:
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.welcome_title).setMessage(
					getString(R.string.welcome_message2)).setCancelable(false)
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {

								}
							});
			alert = builder.create();
			return alert;

		case DIALOG_SELECT_LIMIT:
			final Dialog limit_dialog = new SelectLimitDialog(this);
			Button b = (Button) limit_dialog
					.findViewById(R.id.increase_limit_dialog_ok);
			b.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					limit_dialog.dismiss();
					Intent intent = new Intent(Main.this, Study.class);
					NumberPicker np = (NumberPicker) limit_dialog
							.findViewById(R.id.increase_limit_dialog_picker);
					intent.putExtra("limit_increase", np.getCurrent());
					startActivity(intent);
					forceStudy = false;
				}
			});
			b = (Button) limit_dialog
					.findViewById(R.id.increase_limit_dialog_cancel);
			b.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					limit_dialog.dismiss();
				}
			});

			return limit_dialog;
		default:
			return null;
		}

	}

	private void study() {
		int a;
		if ((a = Langleo.getLearningAlgorithm().isQuestionWaiting()) != LearningAlgorithm.QUESTIONS_WAITING) {
			switch (a) {
			case LearningAlgorithm.NO_QUESTIONS:
				Toast.makeText(this, getString(R.string.no_words_to_study),
						Toast.LENGTH_LONG).show();
				break;
			case LearningAlgorithm.QUESTIONS_ANSWERED:
				Toast.makeText(this, getString(R.string.no_need_to_study_now),
						Toast.LENGTH_LONG).show();
				forceStudy = false;
				break;
			case LearningAlgorithm.QUESTIONS_ANSWERED_FORCEABLE:
				if (forceStudy) {
					showDialog(DIALOG_SELECT_LIMIT);
					return;
				}
				Toast.makeText(this, getString(R.string.no_need_to_study_now),
						Toast.LENGTH_LONG).show();
				Toast.makeText(this,
						getString(R.string.click_again_to_start_either_way),
						Toast.LENGTH_LONG).show();
				forceStudy = true;

				break;
			}

			return;
		}
		Intent intent = new Intent(this, Study.class);
		intent.putExtra("limit_increase", 0);
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
		intent.putExtra("part", "main");
		startActivity(intent);
	}

	private void showUpdates() {
		Intent intent = new Intent(this, Updates.class);
		startActivity(intent);
	}

}