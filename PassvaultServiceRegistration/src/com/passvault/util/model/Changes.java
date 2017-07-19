package com.passvault.util.model;

public class Changes {

	private Change[] results;
	private String last_seq;

	public String getLast_seq() {
		return last_seq;
	}

	public void setLast_seq(String last_seq) {
		this.last_seq = last_seq;
	}

	public Change[] getResults() {
		return results;
	}

	public void setResults(Change[] results) {
		this.results = results;
	}
}
