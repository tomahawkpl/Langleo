package com.atteo.langleo.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.atteo.langleo.Langleo;
import com.atteo.langleo.LearningAlgorithm;
import com.atteo.langleo.R;
import com.atteo.langleo.views.NumberPicker;
import com.atteo.langleo.views.SelectLimitDialog;
import com.atteo.silo.Silo;

public class Main extends Activity {

	private boolean forceStudy = false;

	private static final int DIALOG_SELECT_LIMIT = 0;
	
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

	@Override
	protected Dialog onCreateDialog(int dialogId) {
		switch (dialogId) {
		case DIALOG_SELECT_LIMIT:
			final Dialog dialog = new SelectLimitDialog(this);
			Button b = (Button) dialog.findViewById(R.id.increase_limit_dialog_ok);
			b.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
					Intent intent = new Intent(Main.this, Study.class);
					NumberPicker np = (NumberPicker) dialog.findViewById(R.id.increase_limit_dialog_picker);
					intent.putExtra("limit_increase", np.getCurrent());
					startActivity(intent);
					forceStudy = false;
				}
			});
			b = (Button) dialog.findViewById(R.id.increase_limit_dialog_cancel);
			b.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});
			
			return dialog;
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
		intent.putExtra("part", "");
		startActivity(intent);
	}

}