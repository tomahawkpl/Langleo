package com.atteo.langleo_trial;

import android.view.View;
import android.widget.ProgressBar;

public class TaskInfo {
	
	public final static int TASK_IMPORT = 1;
	public final static int TASK_EXPORT = 2;
	public final static int TASK_DOWNLOAD = 3;
	
	public int type;
	public int collectionId;
	public int listId;
	public int maxProgress;
	public int progress;
	public int previousProgress = 0;
	
	public int getOperationName() {
		switch (type) {
		case TASK_IMPORT:
			return R.string.importing;
		case TASK_EXPORT:
			return R.string.exporting;
		case TASK_DOWNLOAD:
			return R.string.downloading;
		}
		throw new RuntimeException("Unknown task type");
	}
	

	
	public void setProgress(int p) {
		previousProgress = progress;
		progress = p;
	}
	
	ProgressBar listProgress = null;
	View listView;

}