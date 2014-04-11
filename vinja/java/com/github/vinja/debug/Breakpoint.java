package com.github.vinja.debug;

import java.util.ArrayList;
import java.util.List;

import com.sun.jdi.request.EventRequest;

public class Breakpoint {
	
	public static enum Kind { BREAK_POINT, WATCH_POINT };
	public static final int ACCESS_READ = 1;
	public static final int ACCESS_WRITE = 2;
	
	private String mainClass;
	private String innerClass;
	private int lineNum;
	private String conExp;
	private String field;
	private boolean temp;
	private int accessMode = ACCESS_READ | ACCESS_WRITE ;
	private Kind kind = Kind.BREAK_POINT;
	private List<EventRequest> requests = new ArrayList<EventRequest>();
	private boolean enabled = true;
	
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	//do when code executing reached the breakpoint
	private List<String> autoCmds = new ArrayList<String>();
	
	public Breakpoint(String className, String field) {
		this.mainClass = className;
		this.field = field;
		this.kind = Kind.WATCH_POINT;
	}
	
	public Breakpoint(String className, int lineNum) {
		this.mainClass = className;
		this.lineNum = lineNum;
	}
	
	public Breakpoint(String mainClass, String innerClass, int lineNum) {
		this.mainClass = mainClass;
		this.innerClass = innerClass;
		this.lineNum = lineNum;
	}
	
	public String getConExp() {
		return conExp;
	}

	public void setConExp(String conExp) {
		this.conExp = conExp;
	}
	

	public String getMainClass() {
		return mainClass;
	}
	
	public String getInnerClass() {
		return this.innerClass;
	}

	public int getLineNum() {
		return lineNum;
	}
	
	public void clearRequests() {
		requests = new ArrayList<EventRequest>();
	}
	
	public void addRequest(EventRequest request) {
		requests.add(request);
	}
	public List<EventRequest> getRequests() {
		return requests;
	}

	public boolean isTemp() {
		return temp;
	}

	public void setTemp(boolean temp) {
		this.temp = temp;
	}

	public Kind getKind() {
		return kind;
	}

	public void setKind(Kind kind) {
		this.kind = kind;
	}

	public int getAccessMode() {
		return accessMode;
	}

	public void setAccessMode(int accessMode) {
		this.accessMode = accessMode;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}
	
	public void addAutoCmd(String cmd) {
		this.autoCmds.add(cmd);
	}
	
	public void clearAutoCmd() {
		this.autoCmds.clear();
	}
	
	public List<String> getAutoCmds() {
		return this.autoCmds;
	}
	

}
