package com.google.code.vimsztool.debug;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;

public class SuspendThreadStack {
	
	public SuspendThreadStack() { }
	
	private ThreadReference curThreadRef; 
	private ReferenceType curRefType;
	private int curFrame = 0 ;
	
	public synchronized void clean() {
		this.curFrame = 0;
		this.curRefType = null;
		this.curThreadRef = null;
	}
	
	public synchronized void setCurThreadRef(ThreadReference curThreadRef) {
		this.curThreadRef = curThreadRef;
	}
	public synchronized ThreadReference getCurThreadRef() {
		return curThreadRef;
	}
	public synchronized void setCurRefType(ReferenceType curRefType) {
		this.curRefType = curRefType;
	}
	public synchronized ReferenceType getCurRefType() {
		return curRefType;
	}
	public synchronized void setCurFrame(int curFrame) {
		this.curFrame = curFrame;
	}
	public synchronized int getCurFrame() {
		return curFrame;
	} 

}
