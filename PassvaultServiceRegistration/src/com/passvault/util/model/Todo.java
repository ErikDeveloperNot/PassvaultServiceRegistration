package com.passvault.util.model;

public class Todo {
/*
 * REMOVE THIS - JUST FOR TESTING
 */
	
	private String summary;
    private String description;
    public String getSummary() {
        return summary;
    }
    public void setSummary(String summary) {
        this.summary = summary;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "Summary: " + summary + "\nDescription: " + description;
	}
    
    
}
