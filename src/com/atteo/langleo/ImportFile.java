package com.atteo.langleo;

import java.util.ArrayList;

import android.os.Bundle;

public class ImportFile {
	public String filename;
	public String fullpath;
	public ArrayList<String> lines;
	
	public Bundle toBundle() {
		Bundle b = new Bundle();
		b.putString("filename",filename);
		b.putString("fullpath",fullpath);
		b.putStringArray("lines", lines.toArray(new String[lines.size()]));
		
		return b;
	}
	
	public void loadBundle(Bundle b) {
		filename = b.getString("filename");
		fullpath = b.getString("fullpath");
		String[] l = b.getStringArray("lines");
		lines = new ArrayList<String>();
		int len = l.length;
		for (int i=0;i<len;i++)
			lines.add(l[i]);
	}
}
