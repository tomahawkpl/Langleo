package com.atteo.langleo.models;

import java.util.Date;

import com.atteo.langleo.Langleo;
import com.atteo.silo.Storable;
import com.atteo.silo.StorableCollection;
import com.atteo.silo.associations.DatabaseField;

public class StudySession extends Storable {
	
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

	static public StudySession getThisSession() {
		long t = new Date().getTime();

		t -= Langleo.SESSION_TIMEOUT;
		StudySession result = new StorableCollection(StudySession.class).whereInPlace(
				"date > " + t).getFirst();

		if (result != null)
			return result;
		result = new StudySession();
		result.setDate(new Date());
		result.setNewWords(0);
		result.save();
		return result;
	}
}
