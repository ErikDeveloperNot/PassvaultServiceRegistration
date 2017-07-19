package com.passvault.util.model;

public class Change {

	private int seq;
	private String id;
	private Revision[] changes;
	
	public int getSeq() {
		return seq;
	}
	public void setSeq(int seq) {
		this.seq = seq;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Revision[] getChanges() {
		return changes;
	}
	public void setChanges(Revision[] changes) {
		this.changes = changes;
	}
	
	
}
