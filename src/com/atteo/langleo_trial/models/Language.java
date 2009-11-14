package com.atteo.langleo_trial.models;

import com.atteo.silo.Storable;
import com.atteo.silo.associations.DatabaseField;

public class Language extends Storable {
	@DatabaseField
	private String name;
	@DatabaseField
	private String shortName;
	@DatabaseField
	private Integer studyStackId;

	public Language() {
		super();
	}

	public Language(int id) {
		super(id);
	}

	public String getName() {
		return name;
	}

	public String getShortName() {
		return shortName;
	}

	public Integer getStudyStackId() {
		return studyStackId;
	}
}
