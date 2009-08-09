package com.atteo.langleo.models;

import java.util.Date;

import com.atteo.silo.Storable;
import com.atteo.silo.associations.BelongsTo;
import com.atteo.silo.associations.DatabaseField;

public class Question extends Storable {
	@BelongsTo
	private Collection collection;
	@DatabaseField
	private Date date;
	@DatabaseField
	private Date previousDate;
	@BelongsTo
	private Word word;
	@DatabaseField
	private Float difficulty = (float) 2.5;
	@DatabaseField
	private Integer repetitions = -1;
	@DatabaseField
	private Integer queries = 0;
	@DatabaseField
	private Integer correct = 0;
	@DatabaseField
	private Long previousInterval = (long)0;

	public Long getPreviousInterval() {
		return previousInterval;
	}

	public void setPreviousInterval(Long previousInterval) {
		this.previousInterval = previousInterval;
	}
	
	public Date getPreviousDate() {
		return previousDate;
	}

	public void setPreviousDate(Date previousDate) {
		this.previousDate = previousDate;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Date getDate() {
		return date;
	}

	public void setWord(Word word) {
		this.word = word;
	}

	public Word getWord() {
		return word;
	}

	public float getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(float difficulty) {
		this.difficulty = difficulty;
	}

	public int getRepetitions() {
		return repetitions;
	}

	public void addRepetition() {
		load();
		this.repetitions++;
	}

	public void zeroRepetitions() {
		this.repetitions = 0;
	}

	public int getQueries() {
		return queries;
	}

	public void addQuery() {
		load();
		this.queries++;
	}

	public int getCorrect() {
		return correct;
	}

	public void addCorrect() {
		load();
		this.correct++;
	}
/*
	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}*/

	public void setCollection(Collection collection) {
		this.collection = collection;
	}

	public Collection getCollection() {
		return collection;
	}

}
