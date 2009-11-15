package com.atteo.langleo_trial.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

import com.atteo.langleo_trial.R;

public class Help extends Activity {
	private final String prefix = "file:///android_asset/help/";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);
		WebView wv = (WebView) this.findViewById(R.id.help_webview);
		Intent intent = getIntent();
		if (savedInstanceState != null)
			wv.loadUrl(savedInstanceState.getString("url"));
		else
			wv.loadUrl(prefix + intent.getStringExtra("part") + ".html");

	}

	@Override
	public void onSaveInstanceState(Bundle b) {
		WebView wv = (WebView) this.findViewById(R.id.help_webview);
		b.putString("url", wv.getUrl());
	}
}
