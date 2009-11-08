package com.atteo.langleo.activities;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import com.atteo.langleo.R;

public class Updates extends Activity {
	private final String prefix = "file:///android_asset/updates.html";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);
	}

	@Override
	public void onStart() {
		super.onStart();
		WebView wv = (WebView) this.findViewById(R.id.help_webview);
		wv.loadUrl(prefix);
	}

}
