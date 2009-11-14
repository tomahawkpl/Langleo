package com.atteo.langleo_trial;

import java.util.ArrayList;

import android.os.Bundle;

public class ImportData {
	public ArrayList<ImportFile> contents = new ArrayList<ImportFile>();
	public String wordDelimiter;
	public boolean switchOrder;

	public String getFirstLine() {
		if (contents.get(0).lines.size() == 0)
			return null;
		return contents.get(0).lines.get(0);
	}
	
	public Bundle toBundle() {
		Bundle b = new Bundle();
		b.putString("word_delimiter", wordDelimiter);
		b.putBoolean("switch_order", switchOrder);
		int len = contents.size();
		b.putInt("content_size", len);
		for (int i=0;i<len;i++)
			b.putBundle("content_" + i, contents.get(i).toBundle());

		return b;
	}

	public void loadBundle(Bundle b) {
		wordDelimiter = b.getString("word_delimiter");
		switchOrder = b.getBoolean("switch_order");
		int len = b.getInt("content_size");
		for (int i=0;i<len;i++) {
			ImportFile importFile = new ImportFile();
			importFile.loadBundle(b.getBundle("content_" + i));
			contents.add(importFile);
		}
			

	}
}