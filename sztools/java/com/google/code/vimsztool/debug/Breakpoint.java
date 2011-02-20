package com.google.code.vimsztool.debug;

import java.util.ArrayList;
import java.util.List;

import com.sun.jdi.request.BreakpointRequest;

public class Breakpoint {
	
	private String mainClass;
	private int lineNum;
	private List<BreakpointRequest> requests = new ArrayList<BreakpointRequest>();
	
	public Breakpoint(String fileName, int lineNum) {
		this.mainClass = fileName;
		this.lineNum = lineNum;
	}

	public String getMainClass() {
		return mainClass;
	}

	public int getLineNum() {
		return lineNum;
	}
	
	public void addRequest(BreakpointRequest request) {
		requests.add(request);
	}
	public List<BreakpointRequest> getRequests() {
		return requests;
	}
	

}
