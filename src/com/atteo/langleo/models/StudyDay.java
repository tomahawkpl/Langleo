package com.atteo.langleo.models;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.atteo.silo.Storable;
import com.atteo.silo.StorableCollection;
import com.atteo.silo.associations.DatabaseField;

public class StudyDay extends Storable {
	@DatabaseField
	private Date date;
	@DatabaseField
	private Integer newWords;

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
		Date d = new Date(c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH));
		c.setTime(d);
		long t = c.getTimeInMillis();
		
		StudyDay result = new StorableCollection(StudyDay.class).whereInPlace(
				"date = " + t).getFirst();
		if (result != null)
			return result;
		result = new StudyDay();
		result.setDate(c.getTime());
		result.setNewWords(0);
		result.save();
		return result;
	}
}
