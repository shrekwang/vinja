package com.google.code.vimsztool.debug;

import com.google.code.vimsztool.util.VjdeUtil;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.request.EventRequestManager;

public class EventHandler extends Thread {

	private VirtualMachine vm;
	private EventQueue eventQueue;
	private EventSet eventSet;
	private EventRequestManager eventRequestManager;
	private boolean vmExit = false;

	public EventHandler(VirtualMachine vm) {
		this.vm = vm;
		this.eventRequestManager = vm.eventRequestManager();
	}

	public void run() {
		eventQueue = vm.eventQueue();
		while (true) {
			if (vmExit == true) {
				Debugger debugger = Debugger.getInstance();
				debugger.exit();
				break;
			}
			try {
				eventSet = eventQueue.remove();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			EventIterator eventIterator = eventSet.eventIterator();
			while (eventIterator.hasNext()) {
				Event event = (Event) eventIterator.next();
				if (event instanceof VMStartEvent) {
					handleVMStartEvent((VMStartEvent)event);
				} else if (event instanceof ClassPrepareEvent) {
					handleClassPrepareEvent((ClassPrepareEvent)event);
				} else if (event instanceof BreakpointEvent) {
					handleBreakpointEvent((BreakpointEvent)event);
				} else if (event instanceof VMDisconnectEvent) {
					vmExit = true;
				} else if (event instanceof VMDeathEvent) {
					vmExit = true;
				} else if (event instanceof StepEvent) {
					handleStepEvent((StepEvent)event);
				} else {
					eventSet.resume();
				}
			}
		}
	}
	
	public void handleVMDeathEvent(VMDeathEvent event) {
		//TODO : send msg to vim
	}
	
	public void handleVMDisconnectEvent(VMDisconnectEvent event) {
		//TODO : send msg to vim
	}
	
	private void handleVMStartEvent(VMStartEvent event) {
		eventSet.resume();
	}
	
	private void handleClassPrepareEvent(ClassPrepareEvent event) {
		ClassPrepareEvent classPrepareEvent = (ClassPrepareEvent) event;
		String mainClassName = classPrepareEvent.referenceType().name();
		BreakpointManager bpm = BreakpointManager.getInstance();
		bpm.tryCreateBreakpointRequest(mainClassName);
		event.thread().resume();
	}
	
	private void handleBreakpointEvent(BreakpointEvent event) {
		BreakpointEvent breakpointEvent = (BreakpointEvent) event;
		ThreadReference threadRef = breakpointEvent.thread();
		ReferenceType refType = breakpointEvent.location().declaringType();
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		threadStack.setCurRefType(refType);
		threadStack.setCurThreadRef(threadRef);
		
		Location loc = breakpointEvent.location();
		Debugger debugger = Debugger.getInstance();
		String className = loc.declaringType().name();
		int lineNum = loc.lineNumber();
		
		String[] cmdLine = {"HandleSuspend" ,className, String.valueOf(lineNum)};
		VjdeUtil.runVimCmd(debugger.getVimServerName(), cmdLine);
		
	}

	private void handleStepEvent(StepEvent event) {
		StepEvent stepEvent = (StepEvent) event;
		ThreadReference threadRef = stepEvent.thread();
		ReferenceType refType = stepEvent.location().declaringType();
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		threadStack.setCurRefType(refType);
		threadStack.setCurThreadRef(threadRef);
		
		Location loc = stepEvent.location();
		Debugger debugger = Debugger.getInstance();
		String className = loc.declaringType().name();
		int lineNum = loc.lineNumber();
		
		String[] cmdLine = {"HandleSuspend" ,className, String.valueOf(lineNum)};
		VjdeUtil.runVimCmd(debugger.getVimServerName(), cmdLine);
		
	}

}
