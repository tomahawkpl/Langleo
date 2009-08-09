package com.atteo.langleo;

import java.util.ArrayList;
import java.util.HashMap;

import com.atteo.langleo.activities.Collections;
import com.atteo.langleo.activities.Lists;

import android.view.View;
import android.view.ViewStub;
import android.widget.ProgressBar;
import android.widget.TextView;

public class TaskManager {
	private static ArrayList<TaskInfo> tasks = new ArrayList<TaskInfo>();

	private static HashMap<Integer, View> viewsForLists = new HashMap<Integer, View>();
	private static HashMap<Integer, View> viewsForCollections = new HashMap<Integer, View>();

	private static HashMap<Integer, CollectionProgress> collectionProgresses = new HashMap<Integer, CollectionProgress>();

	private static int lastId = -1;

	private static Collections collections;
	private static Lists lists;
	
	public static void setCollections(Collections collections) {
		TaskManager.collections = collections;
	}
	
	public static void setLists(Lists lists) {
		TaskManager.lists = lists;
	}
	
	private static CollectionProgress getCollectionProgress(int collectionId) {
		CollectionProgress result = collectionProgresses.get(collectionId);
		if (result != null)
			return result;

		result = new CollectionProgress();
		collectionProgresses.put(collectionId, result);
		return result;
	}
	

	private static void syncCollectionProgress(int collectionId) {
		View v = viewsForCollections.get(collectionId);
		if (v == null)
			return;
		ProgressBar p = getProgressBarFromView(v);
		CollectionProgress c = collectionProgresses.get(collectionId);

		if (c == null)
			dismissProgressBarInView(v);
		else {
			p.setMax(c.max);
			p.setProgress(c.progress);
			if (c.operationName != 0)
				((TextView) v.findViewById(R.id.operation_addon_name)).setText(c.operationName);
		}

	}

	public static TaskInfo registerTask(int type, int collectionId, int listId,
			int maxProgress) {
		lastId++;
		TaskInfo task = new TaskInfo();
		task.type = type;
		task.maxProgress = maxProgress;
		task.collectionId = collectionId;
		task.listId = listId;

		CollectionProgress c = getCollectionProgress(collectionId);
		c.max += task.maxProgress;
		syncCollectionProgress(collectionId);

		View v = viewsForLists.get(listId);
		if (v != null) {
			task.listView = v;
			task.listProgress = getProgressBarFromView(v);
			task.listProgress.setMax(task.maxProgress);
			((TextView) v.findViewById(R.id.operation_addon_name)).setText(task
					.getOperationName());
		}

		tasks.add(task);

		return task;
	}

	public static void taskFinished(TaskInfo task) {
		tasks.remove(task);
		if (task.listView != null) {
			dismissProgressBarInView(task.listView);
		}

		int len = tasks.size();
		boolean found = false;
		for (int i = 0; i < len; i++) {
			if (tasks.get(i).collectionId == task.collectionId) {
				found = true;
				break;
			}
		}
		if (!found) {
			collectionProgresses.remove(task.collectionId);
			syncCollectionProgress(task.collectionId);
		}
		
		if (lists != null)
			lists.updateListItem(task.listId);
		if (collections != null)
			collections.updateListItem(task.collectionId);
	}

	public static void updateTask(TaskInfo task) {
		if (task.listProgress != null) {
			task.listProgress.setProgress(task.progress);
		}

		CollectionProgress c = getCollectionProgress(task.collectionId);
		c.progress += -task.previousProgress + task.progress;
		c.operationName = task.getOperationName();
		syncCollectionProgress(task.collectionId);

	}

	private static ProgressBar getProgressBarFromView(View v) {
		View operationView = v.findViewById(R.id.operation_addon);
		if (operationView == null) {
			operationView = ((ViewStub) v
					.findViewById(R.id.operation_addon_stub)).inflate();
			((ProgressBar) operationView
					.findViewById(R.id.operation_addon_progress_bar)).setMax(0);
		} else
			operationView.setVisibility(View.VISIBLE);

		return (ProgressBar) operationView
				.findViewById(R.id.operation_addon_progress_bar);

	}

	private static void dismissProgressBarInView(View v) {
		View operationView = v.findViewById(R.id.operation_addon);
		operationView.setVisibility(View.GONE);
		v.requestLayout();
	}

	public static void registerProgressBarForList(int listId, View v) {
		viewsForLists.put(listId, v);

		int len = tasks.size();
		TaskInfo task;
		for (int i = 0; i < len; i++) {
			task = tasks.get(i);
			if (task.listId == listId) {
				task.listView = v;
				task.listProgress = getProgressBarFromView(v);
				task.listProgress.setMax(task.maxProgress);
				((TextView) v.findViewById(R.id.operation_addon_name))
						.setText(task.getOperationName());
				updateTask(task);
				break;
			}
		}
	}

	public static void unregisterProgressBarForList(int listId) {
		viewsForLists.remove(listId);

		int len = tasks.size();
		TaskInfo task;
		for (int i = 0; i < len; i++) {
			task = tasks.get(i);
			if (task.listId == listId) {
				task.listProgress = null;
				task.listView = null;
				break;
			}

		}
	}

	public static void clearProgressBarsForLists() {
		viewsForLists.clear();

		int len = tasks.size();
		TaskInfo task;
		for (int i = 0; i < len; i++) {
			task = tasks.get(i);
			task.listProgress = null;
			task.listView = null;

		}
	}

	public static void registerProgressBarForCollection(int collectionId, View v) {
		viewsForCollections.put(collectionId, v);
		syncCollectionProgress(collectionId);
	}

	public static void unregisterProgressBarForCollection(int collectionId) {
		viewsForCollections.remove(collectionId);

	}

	public static void clearProgressBarsForCollections() {
		viewsForCollections.clear();

	}

	public static boolean isThereATaskForList(int listId) {
		int len = tasks.size();
		for (int i = 0; i < len; i++)
			if (tasks.get(i).listId == listId)
				return true;
		return false;
	}

	public static boolean isThereATaskForCollection(int collectionId) {
		int len = tasks.size();
		for (int i = 0; i < len; i++)
			if (tasks.get(i).collectionId == collectionId)
				return true;
		return false;
	}
}
