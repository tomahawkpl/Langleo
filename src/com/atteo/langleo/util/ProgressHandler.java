package com.atteo.langleo.util;


import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class ProgressHandler extends Handler {
	protected ProgressDialog progressDialog;
	
	public ProgressHandler(ProgressDialog progressDialog) {
		this.progressDialog = progressDialog;
	}

	public void sendMaxMessage(int max) {
		Message msg = obtainMessage();
		Bundle b = new Bundle();
		b.putInt("max", max);
		msg.setData(b);
		sendMessage(msg);
	}
	
	public void sendChangeMessage(String message) {
		Message msg = obtainMessage();
		Bundle b = new Bundle();
		b.putString("message", message);
		msg.setData(b);
		sendMessage(msg);
	}

	public void sendProgressMessage(int progress) {
		Message msg = obtainMessage();
		Bundle b = new Bundle();
		b.putBoolean("progress_sent", true);
		b.putInt("progress", progress);
		msg.setData(b);
		sendMessage(msg);
	}

	public void sendFinishedMessage() {
		Message msg = obtainMessage();
		Bundle b = new Bundle();
		b.putBoolean("finished", true);
		msg.setData(b);
		sendMessage(msg);
	}

	public void handleMessage(Message msg) {
		int max = msg.getData().getInt("max");
		if (max > 0)
			progressDialog.setMax(max);
		int progress = msg.getData().getInt("progress");
		boolean progress_sent = msg.getData().getBoolean("progress_sent");
		if (progress_sent)
			progressDialog.setProgress(progress);

		String message = msg.getData().getString("message");
		if (message != null)
			progressDialog.setMessage(message);
		
		if (msg.getData().getBoolean("finished")) {
			progressDialog.cancel();
		}

	}
}
