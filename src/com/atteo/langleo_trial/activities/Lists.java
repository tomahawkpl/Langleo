package com.atteo.langleo_trial.activities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.atteo.langleo_trial.ImportData;
import com.atteo.langleo_trial.ImportFile;
import com.atteo.langleo_trial.Langleo;
import com.atteo.langleo_trial.R;
import com.atteo.langleo_trial.TaskInfo;
import com.atteo.langleo_trial.TaskManager;
import com.atteo.langleo_trial.models.Collection;
import com.atteo.langleo_trial.models.Language;
import com.atteo.langleo_trial.models.List;
import com.atteo.langleo_trial.models.Word;
import com.atteo.langleo_trial.util.BetterAsyncTask;
import com.atteo.silo.StorableCollection;

public class Lists extends ListActivity {
	private Collection collection;
	private ListsAdapter adapter;

	private final int REQUEST_NEW_LIST = 1;
	private final int REQUEST_EDIT_LIST = 2;
	private final int REQUEST_IMPORT = 3;
	private final int REQUEST_EXPORT = 4;
	private final int REQUEST_DOWNLOAD = 5;
	private final int REQUEST_LIST_WORDS = 6;

	private final int DIALOG_DELETING = 1;
	private final int DIALOG_PREPARING = 2;

	private int enteredList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.lists_list);

		TaskManager.setLists(this);

		adapter = new ListsAdapter();

		collection = new Collection();
		Intent intent = getIntent();
		collection.loadBundle(intent.getBundleExtra("collection"));
		collection.load();
		ListView list = getListView();
		setListAdapter(adapter);

		registerForContextMenu(list);

		list.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				registerProgressBars(view);

			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {

			}

		});

		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				List list = new List((Integer) adapter.getItem(position));
				Intent intent = new Intent(getApplicationContext(), Words.class);
				intent.putExtra("list", list.toBundle());
				enteredList = list.getId();
				startActivityForResult(intent, REQUEST_LIST_WORDS);
			}

		});

		refreshList();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		TaskManager.setLists(null);
		TaskManager.clearProgressBarsForLists();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.lists, menu);
		return true;
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.list, menu);

		if (TaskManager
				.isThereATaskForList((int) ((AdapterContextMenuInfo) menuInfo).id))
			menu.getItem(1).setEnabled(false);
	}

	public Dialog onCreateDialog(int dialog) {
		ProgressDialog progressDialog;
		switch (dialog) {
		case DIALOG_DELETING:
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage(getString(R.string.deleting));
			progressDialog.setCancelable(false);
			return progressDialog;
		case DIALOG_PREPARING:
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage(getString(R.string.preparing));
			progressDialog.setCancelable(false);
			return progressDialog;

		}
		return null;
	}

	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.delete_list:
			final int rowToDelete = (int) info.id;
			AlertDialog.Builder builder = new AlertDialog.Builder(this) {
			};
			builder.setMessage(getString(R.string.are_you_sure)).setCancelable(
					false).setPositiveButton(getString(R.string.yes),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Lists.this.deleteList(rowToDelete);
						}
					}).setNegativeButton(getString(R.string.no),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {

						}
					});
			AlertDialog alert = builder.create();
			alert.show();

			return true;
		case R.id.edit_list:
			editList(info.id);
			return true;
		default:
			return super.onContextItemSelected(item);
		}

	}

	public boolean onOptionsItemSelected(MenuItem menuItem) {
		Intent intent;
		switch (menuItem.getItemId()) {
		case R.id.lists_new_list:
			intent = new Intent(getApplicationContext(), EditList.class);
			List list = new List();
			list.setCollection(collection);

			intent.putExtra("list", list.toBundle());
			startActivityForResult(intent, REQUEST_NEW_LIST);
			break;
		case R.id.lists_import:
			importList();

			break;
		case R.id.lists_export:
			exportList();
			break;
		case R.id.lists_download:
			download();
			break;
		case R.id.lists_help:
			showHelp();
			break;
		}
		return true;
	}

	private void showHelp() {
		Intent intent = new Intent(this, Help.class);
		intent.putExtra("part", "lists");
		startActivity(intent);
	}

	private void registerProgressBars(AbsListView view) {
		TaskManager.clearProgressBarsForLists();

		int last = view.getLastVisiblePosition();
		int first = view.getFirstVisiblePosition();
		for (int i = first; i <= last; i++) {
			View v = view.getChildAt(i - first);
			TaskManager.registerProgressBarForList((Integer) adapter
					.getItem(view.getPositionForView(v)), v);
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == REQUEST_LIST_WORDS)
			updateListItem(enteredList);
		if (resultCode == RESULT_CANCELED)
			return;
		Bundle b;
		switch (requestCode) {
		case REQUEST_NEW_LIST:
		case REQUEST_EDIT_LIST:
			b = intent.getBundleExtra("list");
			List list = new List();
			list.loadBundle(b);
			list.save();
			refreshList();
			break;

		case REQUEST_IMPORT:
			new PrepareImportTask().execute(intent);

			break;

		case REQUEST_EXPORT:
			new PrepareExportTask().execute(intent);

			break;

		case REQUEST_DOWNLOAD:
			new DownloadTask(intent, collection).execute();

			break;

		}
	}

	private void importList() {
		if (!Langleo.checkCard()) {
			Toast.makeText(this, R.string.card_not_mounted, Toast.LENGTH_LONG)
			.show();
			return;
		}
		Intent intent = new Intent(this, ImportFromFile.class);
		intent.putExtra("collection", collection.toBundle());
		startActivityForResult(intent, REQUEST_IMPORT);
	}

	private void exportList() {
		if (!Langleo.checkCard()) {
			Toast.makeText(this, R.string.card_not_mounted, Toast.LENGTH_LONG)
			.show();
			return;
		}
		Intent intent = new Intent(this, SelectList.class);
		intent.putExtra("collection", collection.toBundle());
		startActivityForResult(intent, REQUEST_EXPORT);
	}

	private void download() {
		if (!Langleo.isConnectionAvailable(this))
			return;

		Language l = collection.getTargetLanguage();
		l.load();

		if (l.getStudyStackId() == -1) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this) {
			};
			builder.setMessage(
					getString(R.string.no_download_for_this_language, l
							.getName())).setCancelable(false)
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {

								}
							});
			AlertDialog alert = builder.create();
			alert.show();
			return;
		}
		Intent intent = new Intent(this, Download.class);
		intent.putExtra("collection", collection.toBundle());
		startActivityForResult(intent, REQUEST_DOWNLOAD);
	}

	private void deleteList(long id) {
		new DeleteTask().execute(new List((int) id));
	}

	private void editList(long id) {
		Intent intent = new Intent(getApplicationContext(), EditList.class);
		intent.putExtra("list", new List((int) id).toBundle());
		startActivityForResult(intent, REQUEST_EDIT_LIST);

	}

	public void updateListItem(int listId) {
		List list = new List(listId);
		int words = list.getWords().getCount();
		String s1 = getString(R.string.stats_words);
		String s2 = getString(R.string.stats_learned);
		String stat = words
				+ " "
				+ s1
				+ ", "
				+ (words > 0 ? (list.getWords().whereInPlace("studied != 0")
						.getCount() * 100 / words) : 0) + s2;
		adapter.updateItem(listId, stat);
		getListView().invalidateViews();
	}

	private void refreshList() {
		adapter.clear();

		collection.load();
		StorableCollection lists = collection.getLists().orderByInPlace("name");
		lists.iterate();
		List list;
		String stat;
		String s1 = getString(R.string.stats_words);
		String s2 = getString(R.string.stats_learned);
		int c;
		while ((list = lists.next()) != null) {
			c = list.getWords().getCount();
			stat = c
					+ " "
					+ s1
					+ ", "
					+ (c > 0 ? (list.getWords().whereInPlace("studied != 0")
							.getCount() * 100 / c) : 0) + s2;

			adapter.addList(list.getId(), list.getName(), stat, list
					.isFromStudyStack());
		}
	}

	private class ListsAdapter implements ListAdapter {
		ArrayList<String> names = new ArrayList<String>();
		ArrayList<String> stats = new ArrayList<String>();
		ArrayList<Boolean> studyStack = new ArrayList<Boolean>();
		ArrayList<Integer> ids = new ArrayList<Integer>();

		ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();

		private class ViewHolder {
			TextView tv;
			TextView stats;
			ImageView studystack;
		}

		private void notifyObservers() {
			int len = observers.size();
			for (int i = 0; i < len; i++)
				observers.get(i).onChanged();
		}

		public void addList(int listId, String name, String stat,
				boolean fromStudyStack) {
			ids.add(listId);
			names.add(name);
			stats.add(stat);
			studyStack.add(fromStudyStack);
			notifyObservers();
		}

		public void updateItem(int listId, String stat) {
			int len = ids.size();
			int pos = -1;
			for (int i = 0; i < len; i++) {
				if (ids.get(i) == listId)
					pos = i;
			}
			if (pos == -1)
				return;
			stats.set(pos, stat);
		}

		public void clear() {
			names.clear();
			stats.clear();
			ids.clear();
			studyStack.clear();
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
			return names.size();
		}

		@Override
		public Object getItem(int position) {
			return ids.get(position);
		}

		@Override
		public long getItemId(int position) {
			return ids.get(position);
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
				v = View.inflate(Lists.this, R.layout.list_item, null);
				h = new ViewHolder();
				h.tv = (TextView) v.findViewById(R.id.list_item_name);
				h.stats = (TextView) v.findViewById(R.id.list_item_stats);
				h.studystack = (ImageView) v
						.findViewById(R.id.list_item_studystack_logo);
				v.setTag(h);

			}
			h.tv.setText(names.get(position));
			h.stats.setText(stats.get(position));

			if (studyStack.get(position))
				h.studystack.setVisibility(View.VISIBLE);
			else
				h.studystack.setVisibility(View.GONE);

			TaskManager.registerProgressBarForList(ids.get(position), v);

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
			return names.size() == 0;
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

	private class DeleteTask extends BetterAsyncTask<List, Void, Void> {

		public void onPreExecute() {
			showDialog(DIALOG_DELETING);
		}

		@Override
		public void onPostExecute(Void result) {
			dismissDialog(DIALOG_DELETING);
			refreshList();
		}

		@Override
		protected Void doInBackground(List... params) {
			params[0].delete();
			return null;

		}
	}

	private class PrepareImportTask extends BetterAsyncTask<Intent, Void, Void> {
		private ArrayList<ImportTask> tasks = new ArrayList<ImportTask>();
		private ArrayList<List> lists = new ArrayList<List>();

		ImportData importData;

		@Override
		protected void onPreExecute() {
			showDialog(DIALOG_PREPARING);
		}

		@Override
		protected void onPostExecute(Void result) {
			refreshList();
			int len = importData.contents.size();
			for (int i = 0; i < len; i++)
				tasks.add(new ImportTask(collection, lists.get(i), importData,
						i, tasks));

			dismissDialog(DIALOG_PREPARING);

			tasks.get(0).execute((Void) null);

		}

		private String prepareListName(String filename) {
			String listname = filename.substring(0, filename.length() - 4);
			listname = listname.substring(0, 1).toUpperCase()
					+ listname.substring(1).toLowerCase();
			int pos = -1;
			int p;
			for (int i = 1; i < 10; i++) {
				p = listname.indexOf(String.valueOf(i));
				if (p != -1 && (p < pos || pos == -1))
					pos = p;
			}

			if (pos > 0) {
				listname = listname.substring(0, pos) + " "
						+ listname.substring(pos);
			}
			listname = listname.replace('_', ' ');
			return listname;

		}

		@Override
		protected Void doInBackground(Intent... params) {
			Intent intent = params[0];
			importData = new ImportData();
			importData.loadBundle(intent.getBundleExtra("import_data"));

			int len = importData.contents.size();
			List list;
			for (int i = 0; i < len; i++) {
				ImportFile importFile = importData.contents.get(i);
				list = new List();
				list.setName(prepareListName(importFile.filename));
				list.setCollection(collection);
				list.save();
				lists.add(list);
			}

			return null;
		}

	}

	private class ImportTask extends AsyncTask<Void, Integer, Void> {
		private Collection collection;
		private List list;
		private TaskInfo taskInfo;
		private ImportData importData;
		private int importFileNumber;
		private ArrayList<ImportTask> tasks;

		public ImportTask(Collection collection, List list,
				ImportData importData, int importFileNumber,
				ArrayList<ImportTask> tasks) {
			super();
			this.importFileNumber = importFileNumber;
			this.importData = importData;
			this.collection = collection;
			this.list = list;
			this.tasks = tasks;

			ImportFile importFile = importData.contents.get(importFileNumber);
			int len = importFile.lines.size();
			taskInfo = TaskManager.registerTask(TaskInfo.TASK_IMPORT,
					collection.getId(), list.getId(), len);

		}

		@Override
		public void onPreExecute() {
			tasks.remove(0);
		}

		@Override
		public void onPostExecute(Void result) {
			TaskManager.taskFinished(taskInfo);
			if (!tasks.isEmpty())
				tasks.get(0).execute((Void) null);

		}

		@Override
		protected Void doInBackground(Void... params) {
			collection.load();

			int done = 0;
			String line;

			ImportFile importFile = importData.contents.get(importFileNumber);

			int len = importFile.lines.size();

			Word w = new Word();
			w.setList(list);

			for (int i = 0; i < len; i++) {
				line = importFile.lines.get(i); //
				// if (i % 20 == 0)
				publishProgress(done);

				String[] s = line.split(importData.wordDelimiter);
				if (s.length != 2 && s.length != 3)
					continue;
				if (importData.switchOrder) {
					w.setWord(s[1].trim());
					w.setTranslation(s[0].trim());
				} else {
					w.setWord(s[0].trim());
					w.setTranslation(s[1].trim());
				}
				if (s.length == 3)
					w.setNote(s[2].trim());
				w.quickInsert();
				done++;
			}
			publishProgress(done);

			return null;

		}

		@Override
		public void onProgressUpdate(Integer... progress) {
			taskInfo.setProgress(progress[0]);
			TaskManager.updateTask(taskInfo);
		}

	}

	private class PrepareExportTask extends BetterAsyncTask<Intent, Void, Void> {
		ArrayList<List> lists = new ArrayList<List>();
		ArrayList<String> paths = new ArrayList<String>();

		@Override
		protected void onPreExecute() {
			showDialog(DIALOG_PREPARING);
		}

		@Override
		protected void onPostExecute(Void result) {
			int len = lists.size();
			for (int i = 0; i < len; i++)
				new ExportTask(collection, lists.get(i), paths.get(i))
						.execute();

			dismissDialog(DIALOG_PREPARING);
		}

		@Override
		protected Void doInBackground(Intent... params) {
			String baseDir = Environment.getExternalStorageDirectory() + "/"
					+ Langleo.DIR_NAME;

			File f = new File(baseDir);
			f = new File(f, collection.getName());
			if (!f.exists()) {
				f.mkdir();
			}

			String dir = baseDir + "/" + collection.getName();

			Intent intent = params[0];
			int listids[] = intent.getIntArrayExtra("lists");

			int len = listids.length;
			List list;
			String filePath;
			for (int i = 0; i < len; i++) {
				list = new List(listids[i]);
				list.load();
				filePath = dir + "/" + list.getName() + ".txt";
				paths.add(filePath);
				lists.add(list);
			}
			return null;
		}

	}

	private class ExportTask extends AsyncTask<Void, Integer, Void> {
		private Collection collection;
		private List list;
		private TaskInfo taskInfo;
		private String path;
		private int len;

		public ExportTask(Collection collection, List list, String path) {
			super();
			this.collection = collection;
			this.list = list;
			this.path = path;

		}

		@Override
		public void onPreExecute() {
			len = list.getCount();
			taskInfo = TaskManager.registerTask(TaskInfo.TASK_EXPORT,
					collection.getId(), list.getId(), len);

		}

		@Override
		protected Void doInBackground(Void... params) {
			collection.load();

			int done = 0;
			String line;

			FileWriter fileWriter = null;

			try {
				fileWriter = new FileWriter(path);
			} catch (FileNotFoundException e) {
				return null;
			} catch (IOException e) {
				e.printStackTrace();
			}

			BufferedWriter writer = new BufferedWriter(fileWriter);

			StorableCollection words = list.getWords();
			Word w;

			words.iterate();
			int i = 0;
			while ((w = words.next()) != null) {
				// if (i % 20 == 0)
				publishProgress(done);
				line = w.getWord() + "\t" + w.getTranslation();
				try {
					writer.append(line);
					writer.newLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
				i++;
			}
			publishProgress(done);
			try {
				writer.close();
				fileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;

		}

		@Override
		public void onPostExecute(Void result) {
			TaskManager.taskFinished(taskInfo);
		}

		@Override
		public void onProgressUpdate(Integer... progress) {
			taskInfo.setProgress(progress[0]);
			TaskManager.updateTask(taskInfo);
		}

	}

	private class DownloadTask extends AsyncTask<Void, Integer, Void> {
		private Collection collection;
		private TaskInfo taskInfo;
		private String name;
		private List list;
		private int len;
		private ArrayList<Word> words;

		public DownloadTask(Intent intent, Collection collection) {
			super();
			this.name = intent.getStringExtra("name");
			len = intent.getIntExtra("words", 0);
			words = new ArrayList<Word>();
			Word w;
			for (int i = 0; i < len; i++) {
				w = new Word();
				w.loadBundle(intent.getBundleExtra("word_" + i));
				words.add(w);
			}
			this.collection = collection;

		}

		@Override
		public void onPreExecute() {
			list = new List();
			list.setCollection(collection);
			list.setFromStudyStack(true);
			list.setName(name);
			list.save();
			refreshList();
			taskInfo = TaskManager.registerTask(TaskInfo.TASK_DOWNLOAD,
					collection.getId(), list.getId(), len);

		}

		@Override
		protected Void doInBackground(Void... params) {
			collection.load();

			int done = 0;

			Word w;
			for (int i = 0; i < len; i++) {
				// if (i % 20 == 0)
				publishProgress(done);
				w = words.get(i);
				w.setList(list);
				w.quickInsert();
				done++;

			}
			publishProgress(done);

			return null;

		}

		@Override
		public void onPostExecute(Void result) {
			TaskManager.taskFinished(taskInfo);
		}

		@Override
		public void onProgressUpdate(Integer... progress) {
			taskInfo.setProgress(progress[0]);
			TaskManager.updateTask(taskInfo);
		}

	}

}
