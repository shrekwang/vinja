package com.google.code.vimsztool.debug;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
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
				} else {
					eventSet.resume();
				}
			}
		}
	}
	
	private void handleVMStartEvent(VMStartEvent event) {
		eventSet.resume();
	}
	
	private void handleClassPrepareEvent(ClassPrepareEvent event) {
		ClassPrepareEvent classPrepareEvent = (ClassPrepareEvent) event;
		String mainClassName = classPrepareEvent.referenceType().name();
		BreakpointManager bpm = BreakpointManager.getInstance();
		bpm.tryCreateBreakpointRequest(mainClassName);
		eventSet.resume();
	}
	
	private void handleBreakpointEvent(BreakpointEvent event) {
		BreakpointEvent breakpointEvent = (BreakpointEvent) event;
		ThreadReference threadRef = breakpointEvent.thread();
		ReferenceType refType = breakpointEvent.location().declaringType();
		SuspendThreadStack threadStack = SuspendThreadStack.getInstance();
		threadStack.setCurRefType(refType);
		threadStack.setCurThreadRef(threadRef);
	}


}
