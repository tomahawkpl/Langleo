package com.atteo.langleo_trial.views;

import android.app.Dialog;
import android.content.Context;

import com.atteo.langleo_trial.R;

public class SelectLimitDialog extends Dialog {

	public SelectLimitDialog(Context context) {
		super(context);
		setTitle(R.string.more_new_words);
		setContentView(R.layout.increase_limit_dialog);
		
		
		NumberPicker np = (NumberPicker) findViewById(R.id.increase_limit_dialog_picker);
		np.setRange(1, 100);
		np.setCurrent(5);
	}

}
