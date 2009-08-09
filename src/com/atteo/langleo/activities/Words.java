package com.atteo.langleo.activities;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.atteo.langleo.R;
import com.atteo.langleo.models.List;
import com.atteo.langleo.models.Word;

public class Words extends ListActivity {
	private SimpleCursorAdapter adapter;
	private List list;

	private Cursor cursor;
	
	private final int REQUEST_NEW_WORD = 1;
	private final int REQUEST_EDIT_WORD = 2;

	ArrayList<HashMap<String, String>> datalist;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.words_list);
		
		list = new List();
		list.loadBundle(getIntent().getBundleExtra("list"));

		ListView list = getListView();

		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				editWord((int)id);
			}

		});

		registerForContextMenu(list);

	}

	public void onResume() {
		super.onResume();
		refreshList();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.words, menu);
		return true;
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.word, menu);
	}

	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.delete_word:
			deleteWord((int)info.id);
			return true;
		case R.id.edit_word:
			editWord((int)info.id);
			return true;
		default:
			return super.onContextItemSelected(item);
		}

	}

	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case R.id.new_word:
			Intent intent = new Intent(getApplicationContext(), EditWord.class);
			Word word = new Word();
			word.setList(list);

			intent.putExtra("word", word.toBundle());
			startActivityForResult(intent, REQUEST_NEW_WORD);
			break;
		}
		return true;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_CANCELED)
			return;
		Bundle b;
		switch (requestCode) {
		case REQUEST_NEW_WORD:
		case REQUEST_EDIT_WORD:
			b = intent.getBundleExtra("word");
			Word word = new Word();
			word.loadBundle(b);
			word.save();
			refreshList();
			break;

		}
	}
	
	public void editWord(int id) {
		Intent intent = new Intent(getApplicationContext(), EditWord.class);
		intent.putExtra("word", new Word(id).toBundle());
		startActivityForResult(intent, REQUEST_EDIT_WORD);
		
	}

	public void deleteWord(int id) {
		new Word(id).delete();
		refreshList();
	}

	public void refreshList() {
		ListView list = getListView();
		datalist = new ArrayList<HashMap<String, String>>();

		this.list.load();
		this.cursor = this.list.getWords().orderByInPlace("word").getCursor();
		adapter = new SimpleCursorAdapter(this, R.layout.word_item, this.cursor,
				new String[] { "word", "translation" }, new int[] {
						R.id.word_word, R.id.word_translation });
		list.setAdapter(adapter);
	}
}
