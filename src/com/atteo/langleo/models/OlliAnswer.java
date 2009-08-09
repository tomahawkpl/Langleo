package com.atteo.langleo.models;

import com.atteo.silo.Storable;
import com.atteo.silo.associations.DatabaseField;

public class OlliAnswer extends Storable {
	@DatabaseField private Integer difficulty;
	@DatabaseField private Integer repetitions;
	@DatabaseField private Float factor;
	@DatabaseField private Integer correct = 0;
	@DatabaseField private Integer incorrect = 0;
	
	public Integer getCorrect() {
		return correct;
	}
	public void setCorrect(Integer correct) {
		this.correct = correct;
	}
	public Integer getIncorrect() {
		return incorrect;
	}
	public void setIncorrect(Integer incorrect) {
		this.incorrect = incorrect;
	}
	public Integer getDifficulty() {
		return difficulty;
	}
	public void setDifficulty(Integer difficulty) {
		this.difficulty = difficulty;
	}
	public Integer getRepetitions() {
		return repetitions;
	}
	public void setRepetitions(Integer repetitions) {
		this.repetitions = repetitions;
	}

	public Float getFactor() {
		return factor;
	}
	public void setFactor(Float factor) {
		this.factor = factor;
	}
}
