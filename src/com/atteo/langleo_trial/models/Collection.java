package com.atteo.langleo_trial.models;

import java.util.Date;

import com.atteo.silo.Storable;
import com.atteo.silo.StorableCollection;
import com.atteo.silo.associations.BelongsTo;
import com.atteo.silo.associations.DatabaseField;
import com.atteo.silo.associations.HasMany;

public class Collection extends Storable {
	@DatabaseField
	private String name;
	@DatabaseField
	private Boolean disabled = false;
	@DatabaseField
	private Integer priority = 5;
	@DatabaseField
	private Date started = null;
	@BelongsTo
	private Language baseLanguage;
	@BelongsTo
	private Language targetLanguage;
	@HasMany(klass = List.class, foreignField = "collection", dependent = true)
	StorableCollection lists;

	public Collection() {
		super();
		baseLanguage = new StorableCollection(Language.class).whereInPlace(
				"name='English'").getFirst();
		targetLanguage = new StorableCollection(Language.class).whereInPlace(
				"name='Spanish'").getFirst();
	}

	public Collection(int id) {
		super(id);
	}

	public Date getStarted() {
		return started;
	}

	public void setStarted(Date started) {
		this.started = started;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean getDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public Language getBaseLanguage() {
		return baseLanguage;
	}

	public void setBaseLanguage(Language baseLanguage) {
		this.baseLanguage = baseLanguage;
	}

	public Language getTargetLanguage() {
		return targetLanguage;
	}

	public void setTargetLanguage(Language targetLanguage) {
		this.targetLanguage = targetLanguage;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public StorableCollection getLists() {
		return lists;
	}

	public int getWordsCount() {
		return lists.children(Word.class, "list").getCount();
	}

	public int getLearnedWordsCount() {
		return lists.children(Word.class, "list").whereInPlace("studied != 0")
				.getCount();
	}

	public int getNotLearnedWordsCount() {
		return lists.children(Word.class, "list").whereInPlace("studied == 0")
				.getCount();

	}
}
