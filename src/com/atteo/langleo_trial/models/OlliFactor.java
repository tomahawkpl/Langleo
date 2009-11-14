package com.atteo.langleo_trial.models;

import com.atteo.silo.Storable;
import com.atteo.silo.associations.DatabaseField;

public class OlliFactor extends Storable {
	@DatabaseField
	private Integer difficulty;
	@DatabaseField
	private Integer repetitions;
	@DatabaseField
	private Float factor;
	@DatabaseField
	private Integer hits = 0;

	public Float getFactor() {
		return factor;
	}

	public void setFactor(Float factor) {
		this.factor = factor;
	}

	public int getDifficulty() {
		return difficulty;
	}

	public Integer getRepetitions() {
		return repetitions;
	}

	public void setRepetitions(Integer repetitions) {
		this.repetitions = repetitions;
	}

	public Integer getHits() {
		return hits;
	}

	public void setHits(Integer hits) {
		this.hits = hits;
	}

	public void setDifficulty(Integer difficulty) {
		this.difficulty = difficulty;
	}

	public void setDifficulty(int difficulty) {
		this.difficulty = difficulty;
	}
}
