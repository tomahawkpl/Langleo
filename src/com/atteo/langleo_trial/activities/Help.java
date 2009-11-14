package com.atteo.langleo_trial.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

import com.atteo.langleo_trial.R;

public class Help extends Activity {
	private final String prefix = "file:///android_asset/help/main.html";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);
	}

	@Override
	public void onStart() {
		super.onStart();
		WebView wv = (WebView) this.findViewById(R.id.help_webview);
		Intent intent = getIntent();
		wv.loadUrl(prefix + "#" + intent.getStringExtra("part"));

	}
}
