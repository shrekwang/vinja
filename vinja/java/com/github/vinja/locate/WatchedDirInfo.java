package com.github.vinja.locate;

public class WatchedDirInfo {

	private String alias;
	private String startDir;
	private String excludes;
	private int depth;
	private int watchId;

	public String getStartDir() {
		return startDir;
	}

	public void setStartDir(String startDir) {
		this.startDir = startDir;
	}

	public String getExcludes() {
		return excludes;
	}

	public void setExcludes(String excludes) {
		this.excludes = excludes;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public int getWatchId() {
		return watchId;
	}

	public void setWatchId(int watchId) {
		this.watchId = watchId;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getAlias() {
		return alias;
	}

}
