package com.atteo.langleo_trial.models;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.content.SharedPreferences;

import com.atteo.langleo_trial.Langleo;
import com.atteo.silo.Storable;
import com.atteo.silo.StorableCollection;
import com.atteo.silo.associations.DatabaseField;

public class StudyDay extends Storable {
	@DatabaseField
	private Date date;
	@DatabaseField
	private Integer newWords;
	@DatabaseField
	private Integer maxNewWords;

	public Integer getMaxNewWords() {
		return maxNewWords;
	}

	public void setMaxNewWords(Integer maxNewWords) {
		this.maxNewWords = maxNewWords;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Date getDate() {
		return date;
	}

	public void setNewWords(int newWords) {
		this.newWords = newWords;
	}


	public int getNewWords() {
		return newWords;
	}

	static public StudyDay getToday() {
		GregorianCalendar c = new GregorianCalendar();
		Date d = new Date(c.get(Calendar.YEAR)-1900,c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH));
		c.setTime(d);
		long t = c.getTimeInMillis();
		
		StudyDay result = new StorableCollection(StudyDay.class).whereInPlace(
				"date = " + t).getFirst();
		if (result != null)
			return result;
		SharedPreferences prefs = Langleo.getPreferences();
		int maxNewWordsPerDay = Integer.valueOf(prefs.getString(
				"new_words_per_day", Langleo.DEFAULT_NEW_WORDS_PER_DAY));
		
		result = new StudyDay();
		result.setDate(c.getTime());
		result.setNewWords(0);
		result.setMaxNewWords(maxNewWordsPerDay);
		result.save();
		return result;
	}
}
