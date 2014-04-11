package com.github.vinja.locate;

public class Record {
	
	private String startDir;
	private String name;
	private String relativePath;
	
	public Record() {
		
	}
	public Record(String name, String startDir, String relativePath ) {
		this.name = name;
		this.startDir = startDir;
		this.relativePath = relativePath;
	}
	
	
	public String getStartDir() {
		return startDir;
	}
	public void setStartDir(String startDir) {
		this.startDir = startDir;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getRelativePath() {
		return relativePath;
	}
	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

}
