package com.atteo.langleo_trial.activities;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.atteo.langleo_trial.Langleo;
import com.atteo.langleo_trial.R;
import com.atteo.langleo_trial.TaskManager;
import com.atteo.langleo_trial.models.Collection;
import com.atteo.langleo_trial.models.Language;
import com.atteo.silo.StorableCollection;

public class Collections extends ListActivity {
	private CollectionsAdapter adapter;

	private ProgressDialog deleteDialog;

	private final int REQUEST_NEW_COLLECTION = 1;
	private final int REQUEST_EDIT_COLLECTION = 2;
	private final int REQUEST_COLLECTION_LISTS = 3;

	private final int DIALOG_DELETING = 1;

	private int enteredCollection;

	private HashMap<Integer, Float> collectionLearningSpeeds;

	String s1, s2, s3, s4, s5, s6, s7;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		adapter = new CollectionsAdapter();

		setListAdapter(adapter);
		setContentView(R.layout.collections_list);

		TaskManager.setCollections(this);

		ListView list = getListView();
		registerForContextMenu(list);

		list.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				registerProgressBars(view);

			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub

			}

		});

		s1 = " " + getString(R.string.stats_words);
		s2 = " " + getString(R.string.stats_lists);
		s3 = getString(R.string.stats_learned);
		s4 = getString(R.string.days_learning) + ": ";
		s5 = getString(R.string.days_left) + ": ";
		s6 = getString(R.string.not_learning_yet);
		s7 = getString(R.string.finished_learning);

		refreshList();
	}

	public void onDestroy() {
		super.onDestroy();
		TaskManager.setCollections(null);
		TaskManager.clearProgressBarsForCollections();
	}

	private void calculateLearningSpeeds() {
		SharedPreferences prefs = Langleo.getPreferences();
		int maxNewWordsPerDay = Integer.valueOf(prefs.getString(
				"new_words_per_day", Langleo.DEFAULT_NEW_WORDS_PER_DAY));

		collectionLearningSpeeds = new HashMap<Integer, Float>();

		ArrayList<Integer> priorities = new ArrayList<Integer>();
		ArrayList<Integer> ids = new ArrayList<Integer>();
		int sum = 0;

		StorableCollection collections = new StorableCollection(
				Collection.class);
		collections.whereInPlace("disabled = 0");
		collections.iterate();
		Collection c;

		while ((c = collections.next()) != null) {
			if (c.getNotLearnedWordsCount() == 0)
				continue;
			priorities.add(c.getPriority());
			ids.add(c.getId());
			sum += c.getPriority();
		}

		int len = ids.size();
		for (int i = 0; i < len; i++) {
			collectionLearningSpeeds.put(ids.get(i), (float) maxNewWordsPerDay
					* priorities.get(i) / sum);
		}

	}

	private void registerProgressBars(AbsListView view) {
		TaskManager.clearProgressBarsForCollections();

		int last = view.getLastVisiblePosition();
		int first = view.getFirstVisiblePosition();
		for (int i = first; i <= last; i++) {
			View v = view.getChildAt(i - first);
			TaskManager.registerProgressBarForCollection((Integer) adapter
					.getItem(view.getPositionForView(v)), v);
		}
	}

	@Override
	public void onListItemClick(ListView l, View view, int position, long id) {
		Collection collection = new Collection((Integer) adapter
				.getItem(position));
		collection.load();
		Intent intent = new Intent(Collections.this, Lists.class);
		intent.putExtra("collection", collection.toBundle());
		enteredCollection = collection.getId();
		startActivityForResult(intent, REQUEST_COLLECTION_LISTS);
	}

	public void updateListItem(int collectionId) {
		calculateLearningSpeeds();
		Collection c = new Collection(collectionId);
		c.load();
		String days_learning, days_left, stats_learned, stats_words, stats_lists, stats;
		int words = 0, learned = 0, lists;

		if (c.getDisabled()) {
			days_learning = days_left = stats_learned = stats_words = stats_lists = "";
		} else {
			words = c.getWordsCount();
			learned = c.getLearnedWordsCount();
			lists = c.getLists().getCount();

			Date started = c.getStarted();
			if (started == null || (words == learned && words == 0)) {
				days_learning = s6;
				days_left = "";
			} else if (words == learned && words != 0) {
				days_learning = s7;
				days_left = "";
			} else {

				days_learning = s4
						+ ((new Date().getTime() - started.getTime()) / (1000 * 60 * 60 * 24));
				days_left = s5
						+ (int) ((words - learned) / collectionLearningSpeeds
								.get(collectionId));

			}

			stats = words + s1;
			stats_words = stats;
			stats = lists + s2;
			stats_lists = stats;
			stats = (words > 0 ? (learned * 100) / words : 0) + s3;
			stats_learned = stats;
		}

		adapter.updateItem(collectionId, words, learned, days_learning,
				days_left, stats_learned, stats_words, stats_lists);
		getListView().invalidateViews();
	}

	private void refreshList() {
		calculateLearningSpeeds();
		adapter.clear();
		ArrayList<Language> languages = Langleo.getLanguages();
		StorableCollection collection = new StorableCollection(Collection.class)
				.orderByInPlace("disabled,name");

		String stats = "";
		int words = 0, learned = 0, lists = 0;

		String name, days_learning, days_left, stats_learned, stats_words, stats_lists;
		String base_language_image, target_language_image;
		int id;
		Collection c;
		collection.iterate();
		while ((c = collection.next()) != null) {
			name = c.getName();
			id = c.getId();
			if (c.getDisabled()) {
				days_learning = days_left = stats_learned = stats_words = stats_lists = "";
			} else {
				words = c.getWordsCount();
				learned = c.getLearnedWordsCount();
				lists = c.getLists().getCount();

				Date started = c.getStarted();
				if (started == null || (words == learned && words == 0)) {
					days_learning = s6;
					days_left = "";
				} else if (words == learned && words != 0) {
					days_learning = s7;
					days_left = "";
				} else {
					days_learning = s4
							+ ((new Date().getTime() - started.getTime()) / (1000 * 60 * 60 * 24));

					days_left = s5
							+ (int) ((words - learned) / collectionLearningSpeeds
									.get(id));

				}

				stats = words + s1;
				stats_words = stats;
				stats = lists + s2;
				stats_lists = stats;
				stats = (words > 0 ? (learned * 100) / words : 0) + s3;
				stats_learned = stats;
			}

			int searchedId = c.getBaseLanguage().getId();
			int len = languages.size();
			for (int j = 0; j < len; j++)
				if (languages.get(j).getId() == searchedId) {
					c.setBaseLanguage(languages.get(j));
					break;
				}
			searchedId = c.getTargetLanguage().getId();
			for (int j = 0; j < len; j++)
				if (languages.get(j).getId() == searchedId) {
					c.setTargetLanguage(languages.get(j));
					break;
				}

			base_language_image = c.getBaseLanguage().getName().toLowerCase();
			target_language_image = c.getTargetLanguage().getName()
					.toLowerCase();

			adapter.addCollection(id, name, words, learned, days_learning,
					days_left, stats_learned, stats_words, stats_lists,
					base_language_image, target_language_image);
		}

	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.collections, menu);
		return true;
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.collection, menu);

		if (TaskManager
				.isThereATaskForCollection((int) ((AdapterContextMenuInfo) menuInfo).id))
			menu.getItem(1).setEnabled(false);
	}

	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();

		switch (item.getItemId()) {
		case R.id.delete_collection:
			final int rowToDelete = (int) info.id;
			AlertDialog.Builder builder = new AlertDialog.Builder(this) {
			};
			builder.setMessage(getString(R.string.are_you_sure)).setCancelable(
					false).setPositiveButton(getString(R.string.yes),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Collections.this.deleteCollection(rowToDelete);
						}
					}).setNegativeButton(getString(R.string.no),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {

						}
					});
			AlertDialog alert = builder.create();
			alert.show();

			return true;
		case R.id.edit_collection:
			editCollection((int) info.id);
			return true;

		default:
			return super.onContextItemSelected(item);
		}

	}

	public boolean onOptionsItemSelected(MenuItem menuItem) {
		Intent intent;
		switch (menuItem.getItemId()) {
		case R.id.new_collection:
			intent = new Intent(this, EditCollection.class);
			intent.putExtra("collection", new Collection().toBundle());
			startActivityForResult(intent, REQUEST_NEW_COLLECTION);
			break;

		case R.id.collections_help:
			showHelp();
			break;
		}
		return true;
	}

	private void showHelp() {
		Intent intent = new Intent(this, Help.class);
		intent.putExtra("part", "collections");
		startActivity(intent);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == REQUEST_COLLECTION_LISTS)
			updateListItem(enteredCollection);
		if (resultCode == RESULT_CANCELED)
			return;
		Bundle b;
		Collection collection;
		switch (requestCode) {
		case REQUEST_NEW_COLLECTION:
		case REQUEST_EDIT_COLLECTION:
			b = intent.getBundleExtra("collection");
			collection = new Collection();
			collection.loadBundle(b);
			collection.save();
			refreshList();
			break;
		}
	}

	public Dialog onCreateDialog(int dialog) {
		switch (dialog) {
		case DIALOG_DELETING:
			deleteDialog = new ProgressDialog(this);
			deleteDialog.setMessage(getString(R.string.deleting));
			deleteDialog.setCancelable(false);
			return deleteDialog;
		}
		return null;
	}

	private void deleteCollection(int id) {
		Collection collection = new Collection((int) id);
		new DeleteTask().execute(collection);
	}

	private void editCollection(int id) {
		Intent intent = new Intent(this, EditCollection.class);
		intent.putExtra("collection", new Collection((int) id).toBundle());
		startActivityForResult(intent, REQUEST_EDIT_COLLECTION);

	}

	private class DeleteTask extends AsyncTask<Collection, Void, Void> {
		public void onPreExecute() {
			showDialog(DIALOG_DELETING);
		}

		@Override
		public void onPostExecute(Void result) {
			dismissDialog(DIALOG_DELETING);
			refreshList();
		}

		@Override
		protected Void doInBackground(Collection... params) {
			params[0].delete();
			return null;

		}
	}

	private class CollectionsAdapter implements ListAdapter {
		ArrayList<CollectionInfo> collections = new ArrayList<CollectionInfo>();

		ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();

		private class CollectionInfo {
			String name, days_learning, days_left, stats_learned, stats_words,
					stats_lists;
			int words, learned, id;
			String baseLanguageImage, targetLanguageImage;
		}

		private class ViewHolder {
			TextView name, days_learning, days_left, stats_lists, stats_words,
					stats_learned;
			ProgressBar progress;
			ImageView baseLanguage, targetLanguage;
		}

		private void notifyObservers() {
			int len = observers.size();
			for (int i = 0; i < len; i++)
				observers.get(i).onChanged();
		}

		public void updateItem(int collectionId, int words, int learned,
				String days_learning, String days_left, String stats_learned,
				String stats_words, String stats_lists) {
			int len = collections.size();
			for (int i = 0; i < len; i++) {
				if (collections.get(i).id == collectionId) {
					CollectionInfo c = collections.get(i);
					c.words = words;
					c.days_learning = days_learning;
					c.days_left = days_left;
					c.learned = learned;
					c.stats_learned = stats_learned;
					c.stats_lists = stats_lists;
					c.stats_words = stats_words;
					break;
				}
			}
		}

		public void addCollection(int collectionId, String name, int words,
				int learned, String days_learning, String days_left,
				String stats_learned, String stats_words, String stats_lists,
				String base_language_image, String target_language_image) {
			CollectionInfo c = new CollectionInfo();
			c.baseLanguageImage = base_language_image;
			c.days_learning = days_learning;
			c.days_left = days_left;
			c.id = collectionId;
			c.learned = learned;
			c.name = name;
			c.stats_learned = stats_learned;
			c.stats_lists = stats_lists;
			c.stats_words = stats_words;
			c.targetLanguageImage = target_language_image;
			c.words = words;
			collections.add(c);
			notifyObservers();
		}

		public void clear() {
			collections.clear();
			notifyObservers();
		}

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

		@Override
		public int getCount() {
			return collections.size();
		}

		@Override
		public Object getItem(int position) {
			return collections.get(position).id;
		}

		@Override
		public long getItemId(int position) {
			return collections.get(position).id;
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View operation, v;
			ViewHolder h;

			if (convertView != null) {
				v = convertView;
				h = (ViewHolder) convertView.getTag();
				operation = v.findViewById(R.id.operation_addon);
				if (operation != null)
					operation.setVisibility(View.GONE);
			} else {
				v = View.inflate(Collections.this, R.layout.collection_item,
						null);
				h = new ViewHolder();
				h.name = (TextView) v.findViewById(R.id.collection_item_name);
				h.days_learning = (TextView) v
						.findViewById(R.id.collection_item_days_learning);
				h.days_left = (TextView) v
						.findViewById(R.id.collection_item_days_left);
				h.stats_learned = (TextView) v
						.findViewById(R.id.collection_item_stats_learned);
				h.stats_words = (TextView) v
						.findViewById(R.id.collection_item_stats_words);
				h.stats_lists = (TextView) v
						.findViewById(R.id.collection_item_stats_lists);
				h.progress = (ProgressBar) v
						.findViewById(R.id.collection_item_progress);
				h.baseLanguage = (ImageView) v
						.findViewById(R.id.collection_item_base_language_image);
				h.targetLanguage = (ImageView) v
						.findViewById(R.id.collection_item_target_language_image);

				v.setTag(h);

			}
			CollectionInfo c = collections.get(position);
			h.name.setText(c.name);
			if (c.stats_learned.equals("")) { // if is disabled
				h.progress.setVisibility(View.GONE);
				h.days_learning.setVisibility(View.GONE);
				h.days_left.setVisibility(View.GONE);
				h.stats_learned.setVisibility(View.GONE);
				h.stats_lists.setVisibility(View.GONE);
				h.stats_words.setVisibility(View.GONE);
			} else {
				h.progress.setVisibility(View.VISIBLE);
				h.days_learning.setVisibility(View.VISIBLE);
				h.days_left.setVisibility(View.VISIBLE);
				h.stats_learned.setVisibility(View.VISIBLE);
				h.stats_lists.setVisibility(View.VISIBLE);
				h.stats_words.setVisibility(View.VISIBLE);
				h.progress.setMax(0);
				h.progress.setProgress(0);
				h.progress.setMax(c.words);
				h.progress.setProgress(c.learned);
				h.days_learning.setText(c.days_learning);
				h.days_left.setText(c.days_left);
				h.stats_learned.setText(c.stats_learned);
				h.stats_lists.setText(c.stats_lists);
				h.stats_words.setText(c.stats_words);
			}
			h.baseLanguage.setImageDrawable(getResources().getDrawable(
					getResources().getIdentifier("flag_" + c.baseLanguageImage,
							"drawable", Langleo.PACKAGE)));
			h.targetLanguage.setImageDrawable(getResources().getDrawable(
					getResources().getIdentifier(
							"flag_" + c.targetLanguageImage, "drawable",
							Langleo.PACKAGE)));

			TaskManager.registerProgressBarForCollection(collections
					.get(position).id, v);
			return v;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isEmpty() {
			return collections.size() == 0;
		}

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}

	}

}
