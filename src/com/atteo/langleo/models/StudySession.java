package com.atteo.langleo.models;

import java.util.Date;

import android.content.SharedPreferences;

import com.atteo.langleo.Langleo;
import com.atteo.silo.Storable;
import com.atteo.silo.StorableCollection;
import com.atteo.silo.associations.DatabaseField;

public class StudySession extends Storable {
	
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

	static public StudySession getThisSession() {
		long t = new Date().getTime();

		t -= Langleo.SESSION_TIMEOUT;
		StudySession result = new StorableCollection(StudySession.class).whereInPlace(
				"date > " + t).getFirst();

		if (result != null)
			return result;
		SharedPreferences prefs = Langleo.getPreferences();
		int maxNewWordsPerSession = Integer
				.valueOf(prefs.getString("new_words_per_session",
						Langleo.DEFAULT_NEW_WORDS_PER_SESSION));
		result = new StudySession();
		result.setDate(new Date());
		result.setNewWords(0);
		result.setMaxNewWords(maxNewWordsPerSession);
		result.save();
		return result;
	}
}
