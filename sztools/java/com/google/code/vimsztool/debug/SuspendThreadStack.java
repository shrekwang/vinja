package com.google.code.vimsztool.debug;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;

public class SuspendThreadStack {
	
	private static SuspendThreadStack instance = new SuspendThreadStack();
	public static SuspendThreadStack getInstance() {
		return instance;
	}
	private SuspendThreadStack() {}
	
	private ThreadReference curThreadRef; 
	private ReferenceType curRefType;
	
	public void setCurThreadRef(ThreadReference curThreadRef) {
		this.curThreadRef = curThreadRef;
	}
	public ThreadReference getCurThreadRef() {
		return curThreadRef;
	}
	public void setCurRefType(ReferenceType curRefType) {
		this.curRefType = curRefType;
	}
	public ReferenceType getCurRefType() {
		return curRefType;
	} 

}
