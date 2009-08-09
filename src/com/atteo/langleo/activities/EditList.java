package com.atteo.langleo.activities;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.atteo.langleo.R;
import com.atteo.langleo.models.List;

public class EditList extends Activity {
	private List list;
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_list);
        Bundle bundle = getIntent().getBundleExtra("list");
        list = new List();
        list.loadBundle(bundle);
        list.load();
        Button button = (Button)findViewById(R.id.edit_list_cancel);
        button.setOnClickListener(new OnClickListener() {
        	public void onClick(View view) {
        		cancel();
        	}
        });
        button = (Button)findViewById(R.id.edit_list_ok);
        button.setOnClickListener(new OnClickListener() {
        	public void onClick(View view) {
        		OK();
        	}
        });

        TextView tv_name = (TextView)findViewById(R.id.edit_list_name);
        tv_name.setText(list.getName());

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.edit_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case R.id.edit_list_help:
			showHelp();
			break;
		}
		return true;
	}

	private void showHelp() {
		Intent intent = new Intent(this, Help.class);
		intent.putExtra("part", "edit_list");
		startActivity(intent);
	}
	
	private void OK() {
		Intent intent = new Intent();
		TextView textview_name= (TextView)findViewById(R.id.edit_list_name);
		String name = textview_name.getText().toString();
		list.setName(name);
		intent.putExtra("list", list.toBundle());
		
		setResult(RESULT_OK,intent);
		finish();
	}
	
	private void cancel() {
		setResult(RESULT_CANCELED,null);
		finish();
	}

}
