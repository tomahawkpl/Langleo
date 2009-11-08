package com.atteo.langleo.views;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Button;

import com.atteo.langleo.R;

public class MainMenuButton extends Button {

	public MainMenuButton(Context context) {
		super(context);
	}

	public MainMenuButton(Context context, AttributeSet attrs) {
		super(context, attrs);

	}

	protected void onFocusChanged(boolean gainFocus, int direction,
			Rect previouslyFocusedRect) {
		if (gainFocus) {
			setBackgroundResource(R.drawable.button_focused);
		} else
			setBackgroundResource(R.drawable.button);
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			setBackgroundResource(R.drawable.button_clicked);
		}
		return super.onKeyDown(keyCode, event);
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			setBackgroundResource(R.drawable.button);
		}
		return super.onKeyUp(keyCode, event);
	}

	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN)
			setBackgroundResource(R.drawable.button_clicked);
		else if (event.getAction() == MotionEvent.ACTION_UP)
			setBackgroundResource(R.drawable.button);
		return super.onTouchEvent(event);
		
	}

}
