package com.atteo.langleo.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.atteo.langleo.Langleo;
import com.atteo.langleo.LearningAlgorithm;
import com.atteo.langleo.R;
import com.atteo.langleo.models.Collection;
import com.atteo.langleo.models.Language;
import com.atteo.langleo.models.List;
import com.atteo.langleo.models.Question;
import com.atteo.langleo.models.Word;
import com.atteo.langleo.util.BetterAsyncTask;
import com.atteo.langleo.views.NumberPicker;
import com.atteo.langleo.views.SelectLimitDialog;
import com.google.marvin.widget.TouchGestureControlOverlay;
import com.google.marvin.widget.TouchGestureControlOverlay.Gesture;
import com.google.marvin.widget.TouchGestureControlOverlay.GestureListener;
import com.google.tts.TTS;

public class Study extends Activity {

	private Question currentQuestion = null;
	private Language questionBaseLanguage;
	private Language questionTargetLanguage;
	private TextView tv_word, tv_translation, tv_new, tv_note, tv_progress,
			tv_time_estimation;
	private LinearLayout new_word_buttons, normal_buttons;
	private TouchGestureControlOverlay gestures;
	private ImageView baseLanguageImage, targetLanguageImage;
	private ProgressBar progressBar = null;

	private Chronometer chronometer;

	private long startTime = 0;

	private boolean audioEnabled;
	private boolean readTranslation;

	private int limitIncrease = 0;

	private static final int REQUEST_EDIT_WORD = 0;

	private static final int DIALOG_SELECT_LIMIT = 0;
	private static final int DIALOG_PLEASE_WAIT = 1;

	private PrepareTask prepareTask;

	private TTS tts = null;

	private TTS.InitListener ttsInitListener = new TTS.InitListener() {

		public void onInit(int version) {
			tts.setSpeechRate(120);
			Word w = currentQuestion.getWord().l();

			tts.setLanguage(questionBaseLanguage.getShortName());
			tts.speak(prepareToSpeak(w.getWord()), 1, null);

			readTranslation = true;
		}

	};

	private void initAudio() {
		if (tts == null)
			tts = new TTS(this, ttsInitListener, true);
		else {
			Word w = currentQuestion.getWord();
			w.load();

			tts.setLanguage(questionBaseLanguage.getShortName());
			tts.speak(prepareToSpeak(w.getWord()), 1, null);

			readTranslation = true;
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("t", "onCreate");

		setContentView(R.layout.study);

		
		
		Intent intent = getIntent();
		limitIncrease = intent.getIntExtra("limit_increase", 0);

		SharedPreferences prefs = Langleo.getPreferences();
		audioEnabled = prefs.getBoolean("audio_enabled", false);

		baseLanguageImage = (ImageView) findViewById(R.id.study_base_language_image);
		targetLanguageImage = (ImageView) findViewById(R.id.study_target_language_image);

		progressBar = (ProgressBar) findViewById(R.id.study_progress_bar);

		tv_new = (TextView) findViewById(R.id.study_new_word);
		tv_word = (TextView) findViewById(R.id.study_word_content);
		tv_translation = (TextView) findViewById(R.id.study_translation_content);
		tv_progress = (TextView) findViewById(R.id.study_progress_info);
		tv_note = (TextView) findViewById(R.id.study_note);
		tv_time_estimation = (TextView) findViewById(R.id.study_progress_time_estimation);

		new_word_buttons = (LinearLayout) findViewById(R.id.study_first_time_buttons);
		normal_buttons = (LinearLayout) findViewById(R.id.study_normal_buttons);

		chronometer = (Chronometer) findViewById(R.id.study_chronometer);

		Button button = (Button) findViewById(R.id.study_button_incorrect);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				answer(LearningAlgorithm.ANSWER_INCORRECT);
			}
		});

		button = (Button) findViewById(R.id.study_button_correct);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				answer(LearningAlgorithm.ANSWER_CORRECT);
			}
		});

		button = (Button) findViewById(R.id.study_button_continue);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				answer(LearningAlgorithm.ANSWER_CONTINUE);
			}
		});

		button = (Button) findViewById(R.id.study_button_not_new);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				answer(LearningAlgorithm.ANSWER_NOT_NEW);
			}
		});

		gestures = new TouchGestureControlOverlay(this);
		gestures.setGestureListener(new GestureListener() {

			@Override
			public void onGestureChange(Gesture gesture) {

			}

			@Override
			public void onGestureFinish(Gesture gesture) {
				if (gesture == Gesture.CENTER) {

					tv_translation.setText(currentQuestion.getWord()
							.getTranslation());
					tv_note.setText(currentQuestion.getWord().getNote());

					if (!audioEnabled)
						return;

					Word w = currentQuestion.getWord();

					List list = w.getList();
					list.load();
					Collection c = list.getCollection();
					c.load();

					if (readTranslation) {
						tts.setLanguage(questionTargetLanguage.getShortName());
						tts.speak(prepareToSpeak(w.getTranslation()), 1, null);
					} else {
						tts.setLanguage(questionBaseLanguage.getShortName());
						tts.speak(prepareToSpeak(w.getWord()), 1, null);
					}
					readTranslation = !readTranslation;

				}

				if (gesture == Gesture.UP && audioEnabled) {
					if (currentQuestion.getRepetitions() == -1)
						answer(LearningAlgorithm.ANSWER_CONTINUE);
					else
						answer(LearningAlgorithm.ANSWER_CORRECT);
				}

				if (gesture == Gesture.DOWN && audioEnabled) {
					if (currentQuestion.getRepetitions() == -1)
						answer(LearningAlgorithm.ANSWER_NOT_NEW);
					else
						answer(LearningAlgorithm.ANSWER_INCORRECT);
				}

			}

			@Override
			public void onGestureStart(Gesture gesture) {

			}

		});

		ToggleButton tb = (ToggleButton) findViewById(R.id.study_audio_switch);
		tb.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				SharedPreferences prefs = Langleo.getPreferences();
				audioEnabled = !audioEnabled;
				Editor e = prefs.edit();
				e.putBoolean("audio_enabled", audioEnabled);
				e.commit();
				// updateAudioIcon();
				if (audioEnabled)
					initAudio();

			}
		});

		FrameLayout f = (FrameLayout) findViewById(R.id.study_layout);
		f.addView(gestures);

		updateAudioIcon();

		if (savedInstanceState != null) {
			if (savedInstanceState.getBundle("question") != null) {
				currentQuestion = new Question();
				currentQuestion.loadBundle(savedInstanceState
						.getBundle("question"));
				showQuestion();
				if (savedInstanceState.getBoolean("answer_shown")) {
					tv_translation.setText(currentQuestion.getWord()
							.getTranslation());
					tv_note.setText(currentQuestion.getWord().getNote());
				}
				findViewById(R.id.study_main_layout)
						.setVisibility(View.VISIBLE);
			}
			startTime = savedInstanceState.getLong("elapsedTime");
			prepareTask = new PrepareTask();
			prepareTask.execute(savedInstanceState.getBundle("alg_state"));
		} else {
			startTime = SystemClock.elapsedRealtime();
			prepareTask = new PrepareTask();
			prepareTask.execute();
		}
		
	}

	@Override
	public void onStart() {
		super.onStart();
		if (prepareTask == null) {
			startTime = SystemClock.elapsedRealtime() - startTime;
			chronometer.setBase(startTime);
			chronometer.start();
		}
		Log.i("t", "onStart");
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.i("t", "onResume");
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.i("t", "onPause");
	}

	@Override
	protected void onStop() {
		super.onStop();
		startTime = SystemClock.elapsedRealtime() - startTime;
		chronometer.stop();
		Log.i("t", "onStop");
		removeDialog(DIALOG_PLEASE_WAIT);
		if (prepareTask != null)
			prepareTask.cancel(true);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i("t", "onDestroy");
		Langleo.getLearningAlgorithm().stop();
	}

	private String prepareToSpeak(String string) {
		return string.replaceAll("\\([^)]*\\)", "");
	}

	private void updateAudioIcon() {
		ToggleButton tb = (ToggleButton) findViewById(R.id.study_audio_switch);
		if (audioEnabled)
			tb.setChecked(true);
		else
			tb.setChecked(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.study, menu);
		return true;
	}

	@Override
	protected Dialog onCreateDialog(int dialogId) {
		switch (dialogId) {
		case DIALOG_PLEASE_WAIT:
			ProgressDialog progressDialog;
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage(getString(R.string.please_wait));
			progressDialog.setCancelable(false);
			return progressDialog;

		case DIALOG_SELECT_LIMIT:
			final Dialog dialog = new SelectLimitDialog(this);
			Button b = (Button) dialog
					.findViewById(R.id.increase_limit_dialog_ok);
			b.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
					NumberPicker np = (NumberPicker) dialog
							.findViewById(R.id.increase_limit_dialog_picker);
					LearningAlgorithm alg = Langleo.getLearningAlgorithm();
					alg.increaseLimit(np.getCurrent());

					progressBar.setMax(alg.allQuestions());
					progressBar.setProgress(alg.questionsAnswered() + 1);

					tv_progress.setText((alg.questionsAnswered() + 1) + "/"
							+ alg.allQuestions());
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

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		Intent intent;

		switch (menuItem.getItemId()) {
		case R.id.study_more_new_words:
			showDialog(DIALOG_SELECT_LIMIT);
			break;
		case R.id.study_edit:
			intent = new Intent(getApplicationContext(), EditWord.class);
			intent.putExtra("word", currentQuestion.getWord().toBundle());
			startActivityForResult(intent, REQUEST_EDIT_WORD);
			break;
		case R.id.study_delete:
			Langleo.getLearningAlgorithm().deletedQuestion(currentQuestion);
			currentQuestion.getWord().delete();
			nextQuestion();
			break;
		case R.id.study_help:
			showHelp();
			break;
		}
		return true;
	}

	private void showHelp() {
		Intent intent = new Intent(this, Help.class);
		intent.putExtra("part", "study");
		startActivity(intent);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_CANCELED)
			return;
		Bundle b;
		switch (requestCode) {
		case REQUEST_EDIT_WORD:
			b = intent.getBundleExtra("word");
			Word word = new Word();
			word.loadBundle(b);
			word.save();
			currentQuestion.getWord().reload();
			showQuestion();
			break;

		}
	}

	@Override
	protected void onSaveInstanceState(Bundle b) {
		b.putBoolean("answer_shown", !tv_translation.getText().toString()
				.equals(""));
		if (currentQuestion != null)
			b.putBundle("question", currentQuestion.toBundle());
		b.putLong("elapsedTime", SystemClock.elapsedRealtime() - startTime);
		b.putBundle("alg_state", Langleo.getLearningAlgorithm()
				.getInstanceState());
	}

	private void nextQuestion() {
		currentQuestion = Langleo.getLearningAlgorithm().getQuestion();
		if (currentQuestion == null) {
			finish();
			return;
		}

		showQuestion();
	}

	private void showQuestion() {
		currentQuestion.load();
		Word w = currentQuestion.getWord();
		w.load();
		Collection c = currentQuestion.getCollection();
		c.load();
		if (questionBaseLanguage == null
				|| questionBaseLanguage.getId() != c.getBaseLanguage().getId()) {
			questionBaseLanguage = c.getBaseLanguage();
			questionBaseLanguage.load();
		}
		if (questionTargetLanguage == null
				|| questionTargetLanguage.getId() != c.getTargetLanguage()
						.getId()) {
			questionTargetLanguage = c.getTargetLanguage();
			questionTargetLanguage.load();
		}
		tv_word.setText(w.getWord());
		tv_note.setText("");
		tv_translation.setText("");
		baseLanguageImage.setImageDrawable(getResources().getDrawable(
				getResources().getIdentifier(
						"flag_" + questionBaseLanguage.getName().toLowerCase(),
						"drawable", Langleo.PACKAGE)));

		targetLanguageImage.setImageDrawable(getResources().getDrawable(
				getResources().getIdentifier(
						"flag_"
								+ questionTargetLanguage.getName()
										.toLowerCase(), "drawable",
						Langleo.PACKAGE)));

		LearningAlgorithm alg = Langleo.getLearningAlgorithm();
		progressBar.setMax(alg.allQuestions());
		progressBar.setProgress(alg.questionsAnswered() + 1);

		tv_progress.setText((alg.questionsAnswered() + 1) + "/"
				+ alg.allQuestions());

		if (currentQuestion.getRepetitions() == -1) {
			normal_buttons.setVisibility(View.GONE);
			new_word_buttons.setVisibility(View.VISIBLE);
			tv_new.setVisibility(View.VISIBLE);
			tv_translation.setText(currentQuestion.getWord().getTranslation());
			tv_note.setText(currentQuestion.getWord().getNote());
		} else {
			normal_buttons.setVisibility(View.VISIBLE);
			new_word_buttons.setVisibility(View.GONE);
			tv_new.setVisibility(View.GONE);
		}

		if (audioEnabled) {
			tts.setLanguage(questionBaseLanguage.getShortName());
			tts.speak(prepareToSpeak(w.getWord()), 1, null);
			readTranslation = true;
		}

	}

	private void updateTimeEstimation() {
		LearningAlgorithm alg = Langleo.getLearningAlgorithm();
		if (alg.questionsAnswered() < 3) {
			tv_time_estimation.setText("--:--");
			return;
		}
		long estimation = SystemClock.elapsedRealtime() - startTime;
		estimation += estimation / alg.questionsAnswered()
				* (alg.allQuestions() - alg.questionsAnswered());
		int hours = (int) (estimation / (1000 * 60 * 60));
		int minutes = (int) (estimation / (1000 * 60)) % 60;
		int seconds = (int) (estimation / 1000) % 60;

		String minStr;
		String secStr;

		if (minutes < 10 && hours > 0)
			minStr = "0" + minutes;
		else
			minStr = String.valueOf(minutes);

		if (seconds < 10)
			secStr = "0" + seconds;
		else
			secStr = String.valueOf(seconds);

		if (hours > 0)
			tv_time_estimation.setText(hours + ":" + minStr + ":" + secStr);
		else
			tv_time_estimation.setText(minStr + ":" + secStr);
	}

	private void answer(int answerQuality) {
		Langleo.getLearningAlgorithm().answer(currentQuestion, answerQuality);
		nextQuestion();
		updateTimeEstimation();
	}

	private class PrepareTask extends BetterAsyncTask<Bundle, Void, Void> {
		@Override
		protected void onPreExecute() {
			showDialog(DIALOG_PLEASE_WAIT);
		}

		@Override
		protected void onPostExecute(Void v) {
			prepareTask = null;
			if (audioEnabled)
				initAudio();
			if (currentQuestion == null)
				nextQuestion();
			else
				showQuestion();
			findViewById(R.id.study_main_layout).setVisibility(View.VISIBLE);
			removeDialog(DIALOG_PLEASE_WAIT);
			chronometer.setBase(startTime);
			chronometer.start();
			updateTimeEstimation();

		}

		@Override
		protected Void doInBackground(Bundle... params) {
			Bundle bundle;
			if (params.length == 1)
				bundle = params[0];
			else
				bundle = null;
			Langleo.getLearningAlgorithm().start(bundle);
			Langleo.getLearningAlgorithm().increaseLimit(limitIncrease);
			return null;
		}

	}

}
