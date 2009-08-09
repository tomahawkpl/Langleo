package com.atteo.langleo.models;

import com.atteo.silo.Storable;
import com.atteo.silo.StorableCollection;
import com.atteo.silo.associations.BelongsTo;
import com.atteo.silo.associations.DatabaseField;
import com.atteo.silo.associations.HasMany;

public class List extends Storable {
	@DatabaseField private String name;
	@DatabaseField private Boolean fromStudyStack = false;
	@BelongsTo private Collection collection;
	@HasMany(klass=Word.class,foreignField="list",dependent=true) StorableCollection words;
	
	public List() {
		super();
	}
	
	public List(int id) {
		super(id);
	}
	
	public boolean isFromStudyStack() {
		return fromStudyStack;
	}

	public void setFromStudyStack(Boolean fromStudyStack) {
		this.fromStudyStack = fromStudyStack;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Collection getCollection() {
		return collection;
	}
	
	public void setCollection(Collection collection) {
		this.collection = collection;
	}
	
	public StorableCollection getWords() {
		return words;
	}
}
